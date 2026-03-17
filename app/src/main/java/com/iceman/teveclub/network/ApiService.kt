package com.iceman.teveclub.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    // Direct teveclub.hu endpoints

    @FormUrlEncoded
    @POST(".")
    suspend fun login(
        @Field("tevenev") username: String,
        @Field("pass") password: String,
        @Field("x") x: String = "38",
        @Field("y") y: String = "42",
        @Field("login") login: String = "Gyere!"
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST(".")
    suspend fun logout(
        @Field("logout") logout: String = "1"
    ): Response<ResponseBody>

    @GET("myteve.pet")
    suspend fun getMyTeve(): Response<ResponseBody>

    @FormUrlEncoded
    @POST("myteve.pet")
    suspend fun feedAndDrink(
        @Field("kaja") foodId: String,
        @Field("pia") drinkId: String,
        @Field("etet") submit: String = "Mehet!"
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST("setfood.pet")
    suspend fun setFood(
        @Field("kaja") foodId: String
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST("setdrink.pet")
    suspend fun setDrink(
        @Field("kaja") drinkId: String
    ): Response<ResponseBody>

    @GET("tanit.pet")
    suspend fun getLearnPage(): Response<ResponseBody>

    @FormUrlEncoded
    @POST("egyszam.pet")
    suspend fun guessNumber(
        @Field("honnan") from: String = "403",
        @Field("tipp") submit: String = "Ez a tippem!"
    ): Response<ResponseBody>
}
