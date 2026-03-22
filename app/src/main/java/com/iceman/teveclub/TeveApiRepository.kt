package com.iceman.teveclub

import android.content.Context
import com.iceman.teveclub.network.ApiService
import com.iceman.teveclub.network.NetworkModule
import kotlinx.coroutines.delay
import org.jsoup.Jsoup

class TeveApiRepository(private val context: Context) {
    private val api: ApiService = NetworkModule.provideApi(context)

    private fun html(body: okhttp3.ResponseBody?): String = body?.string() ?: ""

    suspend fun login(username: String, password: String): Result<String> {
        return try {
            val resp = api.login(username, password)
            if (resp.isSuccessful) {
                val page = html(resp.body())
                if (page.contains("Teve Legyen Veled", ignoreCase = true) ||
                    page.contains("myteve.pet", ignoreCase = true)) {
                    AuthManager.saveUsername(context, username)
                    AuthManager.setLoggedIn(context, true)
                    Result.success("logged")
                } else {
                    Result.failure(Exception("Hibás felhasználónév vagy jelszó"))
                }
            } else Result.failure(Exception("Login failed: ${resp.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> {
        return try {
            val resp = api.logout()
            AuthManager.clear(context)
            AuthManager.setLoggedIn(context, false)
            if (resp.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Logout failed: ${resp.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class CamelStatus(
        val foodId: String? = null,
        val drinkId: String? = null,
        val trick: String? = null,
        val canFeed: Boolean = false,
        val feedCountText: String? = null,
        val fullHtml: String = "",
        val petImageUrl: String? = null,
        val trickImageUrl: String? = null,
        val foodPercent: Int = 0,
        val drinkPercent: Int = 0,
        val foodCount: Int = 0,
        val foodMax: Int = 7,
        val drinkCount: Int = 0,
        val drinkMax: Int = 7,
        val foodImageUrl: String? = null,
        val drinkImageUrl: String? = null
    )

    suspend fun getCamelStatus(): Result<CamelStatus> {
        return try {
            val resp = api.getMyTeve()
            if (resp.isSuccessful) {
                val page = html(resp.body())

                val doc = Jsoup.parse(page)

                // Check if feeding is possible
                val canFeed = page.contains("Mehet!", ignoreCase = true)

                // Parse Súly (weight = food fullness) percentage: "(100%)" pattern after "Súly:" text
                val sulySection = page.substringAfter("S\u00faly:", "").substringBefore("Kedv:", "")
                val foodPercentMatch = Regex("""\((\d+)%\)""").find(sulySection)
                val foodPercent = foodPercentMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0

                // Parse Kedv (mood = drink fullness) percentage
                val kedvSection = page.substringAfter("Kedv:", "")
                val drinkPercentMatch = Regex("""\((\d+)%\)""").find(kedvSection)
                val drinkPercent = drinkPercentMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0

                // Count food/drink by img tags inside <a href="/setfood.pet"> and <a href="/setdrink.pet">
                // Only count images that are NOT "no.gif" (no.gif = empty slot)
                val foodLink = doc.select("a[href=/setfood.pet]").firstOrNull { it.select("img").isNotEmpty() }
                val foodImgs = foodLink?.select("img") ?: emptyList()
                val foodCount = foodImgs.count { !it.attr("src").contains("no.gif") }.coerceAtMost(7)
                val firstFoodImg = foodImgs.firstOrNull { !it.attr("src").contains("no.gif") }?.attr("src") ?: ""
                val foodImageUrl = if (firstFoodImg.isNotEmpty()) "https://teveclub.hu$firstFoodImg" else null

                val drinkLink = doc.select("a[href=/setdrink.pet]").firstOrNull { it.select("img").isNotEmpty() }
                val drinkImgs = drinkLink?.select("img") ?: emptyList()
                val drinkCount = drinkImgs.count { !it.attr("src").contains("no.gif") }.coerceAtMost(7)
                val firstDrinkImg = drinkImgs.firstOrNull { !it.attr("src").contains("no.gif") }?.attr("src") ?: ""
                val drinkImageUrl = if (firstDrinkImg.isNotEmpty()) "https://teveclub.hu$firstDrinkImg" else null

                // Parse trick text
                val trickEl = doc.select("div:containsOwn(Tanult tr\u00fckk)").firstOrNull()
                    ?: doc.select("div:containsOwn(tr\u00fckk)").firstOrNull()
                val trick = trickEl?.ownText()?.trim()

                // Extract pet image from <img> in the activity div (not from SWF — no GIF version exists for SWF tricks)
                var petImageUrl: String? = null
                val activityDiv = doc.getElementsContainingOwnText("Tev\u00e9d most \u00e9ppen").firstOrNull()
                val petImageEl = activityDiv?.select("img")?.firstOrNull()
                if (petImageEl != null) {
                    val src = petImageEl.attr("src")
                    petImageUrl = if (src.startsWith("http")) src else "https://teveclub.hu/$src"
                }

                // Parse trick/activity text from "Tevéd most éppen ..." div
                val activityDiv2 = doc.getElementsContainingOwnText("Tev\u00e9d most \u00e9ppen").firstOrNull()
                val activityText = activityDiv2?.ownText()?.trim()
                    ?.replace("Tev\u00e9d most \u00e9ppen", "")?.trim()?.trimEnd('.')

                Result.success(CamelStatus(
                    foodId = Regex("""(\d+)\.gif""").find(firstFoodImg)?.groupValues?.get(1),
                    drinkId = Regex("""(\d+)\.gif""").find(firstDrinkImg)?.groupValues?.get(1),
                    trick = activityText ?: trick,
                    canFeed = canFeed,
                    fullHtml = page,
                    petImageUrl = petImageUrl,
                    foodPercent = foodPercent,
                    drinkPercent = drinkPercent,
                    foodCount = foodCount,
                    foodMax = 7,
                    drinkCount = drinkCount,
                    drinkMax = 7,
                    foodImageUrl = foodImageUrl,
                    drinkImageUrl = drinkImageUrl
                ))
            } else Result.failure(Exception("Status failed: ${resp.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Feed pet repeatedly until full, like the Django bot. */
    suspend fun feedUntilFull(onProgress: (String) -> Unit): Result<String> {
        var feedCount = 0
        val maxAttempts = 10

        while (feedCount < maxAttempts) {
            // Check if feeding is still needed
            val checkResp = api.getMyTeve()
            if (!checkResp.isSuccessful) {
                return Result.failure(Exception("Nem sikerült ellenőrizni az etetést"))
            }
            val checkPage = html(checkResp.body())

            if (!checkPage.contains("Mehet!")) {
                return if (feedCount == 0) {
                    Result.success("A tevéd már jóllakott! Nem kell etetni.")
                } else {
                    Result.success("Kész! $feedCount alkalommal etetve, a teve jóllakott!")
                }
            }

            // Feed
            val feedResp = api.feedAndDrink("1", "1")
            if (!feedResp.isSuccessful) {
                return Result.failure(Exception("Etetés sikertelen $feedCount próba után"))
            }

            feedCount++
            val feedPage = html(feedResp.body())
            onProgress("Etetés $feedCount/$maxAttempts...")

            if (feedPage.contains("elég jóllakott", ignoreCase = true) ||
                feedPage.contains("tele a hasa", ignoreCase = true)) {
                return Result.success("Kész! $feedCount alkalommal etetve, a teve jóllakott!")
            }

            delay(500)
        }
        return Result.success("$feedCount alkalommal etetve (maximum elérve)")
    }

    suspend fun setFood(foodId: String): Result<Unit> {
        return try {
            val resp = api.setFood(foodId)
            if (resp.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Kaja beállítás sikertelen"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setDrink(drinkId: String): Result<Unit> {
        return try {
            val resp = api.setDrink(drinkId)
            if (resp.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Ital beállítás sikertelen"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class LearnOption(val value: String, val name: String)

    sealed class LearnPageState {
        data class HasOptions(val options: List<LearnOption>) : LearnPageState()
        object NoOptionsButCanLearn : LearnPageState()
        object AlreadyLearnedAll : LearnPageState()
    }

    suspend fun getLearnPage(): Result<LearnPageState> {
        return try {
            val resp = api.getLearnPage()
            if (resp.isSuccessful) {
                val page = html(resp.body())
                val doc = Jsoup.parse(page)

                val select = doc.select("select[name=tudomany]").firstOrNull()
                if (select != null) {
                    val options = select.select("option").mapNotNull { opt ->
                        val value = opt.attr("value")
                        val name = opt.text().trim()
                        if (value.isNotBlank() && name.isNotBlank()) LearnOption(value, name) else null
                    }
                    if (options.isNotEmpty()) {
                        Result.success(LearnPageState.HasOptions(options))
                    } else {
                        Result.success(LearnPageState.AlreadyLearnedAll)
                    }
                } else {
                    val learnButton = doc.select("input[name=learn]").firstOrNull()
                    if (learnButton != null) {
                        Result.success(LearnPageState.NoOptionsButCanLearn)
                    } else {
                        Result.success(LearnPageState.AlreadyLearnedAll)
                    }
                }
            } else Result.failure(Exception("Tanítás oldal hiba: ${resp.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitLearn(lessonId: String): Result<String> {
        return try {
            val resp = api.submitLearn(lessonId)
            if (resp.isSuccessful) {
                Result.success("Tanítás sikeres!")
            } else Result.failure(Exception("Tanítás hiba: ${resp.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun guessNumber(number: String): Result<String> {
        return try {
            val resp = api.guessNumber(guess = number)
            if (resp.isSuccessful) {
                val page = html(resp.body())
                val doc = Jsoup.parse(page)
                // Only extract the game result, not the whole page
                val resultEl = doc.select("div:containsOwn(kisebb), div:containsOwn(nagyobb), div:containsOwn(eltalál), div:containsOwn(Gratulál), div:containsOwn(szám)").firstOrNull()
                val resultText = resultEl?.text()?.trim()
                    ?: doc.select("center").lastOrNull()?.text()?.trim()
                    ?: "Tipp elküldve: $number"
                Result.success(resultText)
            } else Result.failure(Exception("Számjáték hiba: ${resp.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
