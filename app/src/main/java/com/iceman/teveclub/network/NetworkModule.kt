package com.iceman.teveclub.network

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import okhttp3.JavaNetCookieJar
import java.net.CookieManager
import java.net.CookiePolicy

object NetworkModule {
    private const val BASE_URL = "https://teveclub.hu/"

    fun provideOkHttp(context: Context): OkHttpClient {
        val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }

        val cookieStore = PersistentCookieStore(context)
        val cookieManager = CookieManager(cookieStore, CookiePolicy.ACCEPT_ALL)

        // Browser-like headers so teveclub.hu accepts our requests
        val headerInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "hu-HU,hu;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Referer", "https://teveclub.hu/")
                .header("Origin", "https://teveclub.hu")
                .build()
            chain.proceed(request)
        }

        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .cookieJar(JavaNetCookieJar(cookieManager))
            .addInterceptor(headerInterceptor)
            .addInterceptor(logger)
            .build()
    }

    fun provideRetrofit(context: Context): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(provideOkHttp(context))
            .build()
    }

    fun provideApi(context: Context): ApiService = provideRetrofit(context).create(ApiService::class.java)
}
