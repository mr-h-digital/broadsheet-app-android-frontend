package com.hildebrandtdigital.wpcbroadsheet.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    // ── Base URL ──────────────────────────────────────────────────────────────
    // 10.0.2.2 is the Android emulator's alias for localhost on the host machine.
    // Change to your server IP when deploying to a real device or server.
    private const val BASE_URL = "http://10.0.2.2:8080/"

    // ── OkHttp ────────────────────────────────────────────────────────────────
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // ── Retrofit ──────────────────────────────────────────────────────────────
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val wpcApi: WpcApiService by lazy {
        retrofit.create(WpcApiService::class.java)
    }
}