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
        val trickImageUrl: String? = null
    )

    suspend fun getCamelStatus(): Result<CamelStatus> {
        return try {
            val resp = api.getMyTeve()
            if (resp.isSuccessful) {
                val page = html(resp.body())

                val foodMatch = Regex("""Etet[őo].*?/(\d+)\.gif""", RegexOption.DOT_MATCHES_ALL).find(page)
                val drinkMatch = Regex("""Itat[óo].*?/(\d+)\.gif""", RegexOption.DOT_MATCHES_ALL).find(page)

                // Parse trick text
                val doc = Jsoup.parse(page)
                val trickEl = doc.select("div:containsOwn(Tanult trükk)").firstOrNull()
                    ?: doc.select("div:containsOwn(trükk)").firstOrNull()
                val trick = trickEl?.ownText()?.trim()

                // Check if feeding is possible
                val canFeed = page.contains("Mehet!", ignoreCase = true)

                // Try to parse feed count from page (e.g. "5/10" pattern)
                val feedCountMatch = Regex("""(\d+)\s*/\s*(\d+)""").find(page)
                val feedCountText = feedCountMatch?.value

                // Extract pet activity image/gif URL
                // Find the img tag that follows "Tevéd most éppen" text
                val petImageEl = doc.getElementsContainingOwnText("Tevéd most éppen").firstOrNull()
                    ?.nextElementSiblings()?.select("img")?.firstOrNull()
                    ?: doc.select("img[src*=/images/farm/]").firstOrNull()
                val petImageSrc = petImageEl?.attr("src")
                val petImageUrl = if (!petImageSrc.isNullOrBlank()) {
                    if (petImageSrc.startsWith("http")) petImageSrc
                    else "https://teveclub.hu/$petImageSrc"
                } else null

                // Extract trick image from /images/farm/truk/ path
                val trickImgEl = doc.select("img[src*=/images/farm/truk/]").firstOrNull()
                val trickImgSrc = trickImgEl?.attr("src")
                val trickImageUrl = if (!trickImgSrc.isNullOrBlank()) {
                    if (trickImgSrc.startsWith("http")) trickImgSrc
                    else "https://teveclub.hu/$trickImgSrc"
                } else null

                Result.success(CamelStatus(
                    foodId = foodMatch?.groupValues?.get(1),
                    drinkId = drinkMatch?.groupValues?.get(1),
                    trick = trick,
                    canFeed = canFeed,
                    feedCountText = feedCountText,
                    fullHtml = page,
                    petImageUrl = petImageUrl,
                    trickImageUrl = trickImageUrl
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
                val page = html(resp.body())
                val doc = Jsoup.parse(page)
                val resultText = doc.body().text().take(200)
                Result.success(resultText)
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
