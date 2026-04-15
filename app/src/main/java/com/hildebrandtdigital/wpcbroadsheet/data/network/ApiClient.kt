package com.hildebrandtdigital.wpcbroadsheet.data.network

import com.hildebrandtdigital.wpcbroadsheet.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "https://web-production-927ff.up.railway.app/"

    // ── Auth interceptor ──────────────────────────────────────────────────────
    // Holds a reference to the token provider so the interceptor always uses
    // the current token without needing to be rebuilt.
    private var tokenProvider: (() -> String?)? = null

    fun setTokenProvider(provider: () -> String?) {
        tokenProvider = provider
    }

    private val authInterceptor = Interceptor { chain ->
        val token = tokenProvider?.invoke()
        val request = if (token != null) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    // ── Logging (debug builds only) ───────────────────────────────────────────
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                else                   HttpLoggingInterceptor.Level.NONE
    }

    // ── OkHttp ────────────────────────────────────────────────────────────────
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
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
