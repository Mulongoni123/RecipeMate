package com.example.recipemate.data.remote

import com.example.recipemate.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {

    private const val BASE_URL = "https://api.spoonacular.com/"

    // ✅ Shared OkHttp client with logging + API key interceptor
    private val client by lazy {
        OkHttpClient.Builder()
            .addInterceptor(ApiKeyInterceptor(BuildConfig.SPOONACULAR_API_KEY))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // ✅ Global singleton Retrofit instance
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // ✅ Public API property used by ViewModels
    val api: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}