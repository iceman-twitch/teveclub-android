package com.iceman.teveclub.network

import android.content.Context
import com.iceman.teveclub.SecurePrefs
import org.json.JSONArray
import org.json.JSONObject
import java.net.CookieStore
import java.net.HttpCookie
import java.net.URI
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList

class PersistentCookieStore(private val context: Context) : CookieStore {
    private val prefsKey = "cookie_store_v1"
    private val lock = Any()
    private val cookieList = CopyOnWriteArrayList<HttpCookie>()

    init {
        loadFromPrefs()
    }

    private fun loadFromPrefs() {
        val prefs = SecurePrefs.getEncryptedPrefs(context)
        val json = prefs.getString(prefsKey, null) ?: return
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val name = o.getString("name")
                val value = o.getString("value")
                val cookie = HttpCookie(name, value)
                if (o.has("domain")) cookie.domain = o.getString("domain")
                if (o.has("path")) cookie.path = o.getString("path")
                if (o.has("maxAge")) cookie.maxAge = o.getLong("maxAge")
                cookie.secure = o.optBoolean("secure", false)
                cookie.version = o.optInt("version", 0)
                cookieList.add(cookie)
            }
        } catch (e: Exception) {
            // ignore parse errors
        }
    }

    private fun saveToPrefs() {
        val prefs = SecurePrefs.getEncryptedPrefs(context)
        val arr = JSONArray()
        for (c in cookieList) {
            val o = JSONObject()
            o.put("name", c.name)
            o.put("value", c.value)
            o.put("domain", c.domain)
            o.put("path", c.path)
            o.put("maxAge", c.maxAge)
            o.put("secure", c.secure)
            o.put("version", c.version)
            arr.put(o)
        }
        prefs.edit().putString(prefsKey, arr.toString()).apply()
    }

    override fun add(uri: URI?, cookie: HttpCookie?) {
        if (cookie == null) return
        cookieList.removeIf { it.name == cookie.name && it.domain == cookie.domain && it.path == cookie.path }
        cookieList.add(cookie)
        saveToPrefs()
    }

    override fun get(uri: URI?): MutableList<HttpCookie> {
        if (uri == null) return Collections.synchronizedList(mutableListOf())
        val result = mutableListOf<HttpCookie>()
        val host = uri.host
        for (c in cookieList) {
            val domain = c.domain
            if (domain == null) continue
            if (host.endsWith(domain.trimStart('.'))) result.add(c)
        }
        return result
    }

    override fun getCookies(): MutableList<HttpCookie> = cookieList

    override fun getURIs(): MutableList<URI> = Collections.synchronizedList(mutableListOf())

    override fun remove(uri: URI?, cookie: HttpCookie?): Boolean {
        if (cookie == null) return false
        val removed = cookieList.removeIf { it.name == cookie.name && it.domain == cookie.domain && it.path == cookie.path }
        if (removed) saveToPrefs()
        return removed
    }

    override fun removeAll(): Boolean {
        val had = cookieList.isNotEmpty()
        cookieList.clear()
        saveToPrefs()
        return had
    }
}
