package com.example.recipemate.data.remote

import android.util.Log
import com.example.recipemate.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {

    private const val BASE_URL = "https://api.spoonacular.com/"
    private const val TAG = "RetrofitInstance"

    // Fallback API key (replace with your actual key)
    private const val FALLBACK_API_KEY = "7fa03aef94104c62a770ac92b39edb09"

    // Get API key from BuildConfig or use fallback
    private val apiKey: String by lazy {
        try {
            // Try to get from BuildConfig first
            BuildConfig.SPOONACULAR_API_KEY.ifEmpty { FALLBACK_API_KEY }
        } catch (e: Exception) {
            Log.w(TAG, "BuildConfig API key not found, using fallback")
            FALLBACK_API_KEY
        }
    }


    // ✅ Shared OkHttp client with logging + API key interceptor
    private val client by lazy {
        OkHttpClient.Builder()
            .addInterceptor(ApiKeyInterceptor(apiKey))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
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