package com.hildebrandtdigital.wpcbroadsheet.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

// ── Request / Response DTOs ───────────────────────────────────────────────────

data class LoginRequest(
    val email: String,
    val password: String,   // SHA-256 hex hash — matches existing UserRepository.sha256()
)

data class ApiUser(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val phone: String,
    val siteId: String?,
    val avatarUrl: String?,
)

data class LoginResponse(
    val token: String,
    val user: ApiUser,
)

// ── API Service ───────────────────────────────────────────────────────────────

interface WpcApiService {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/auth/me")
    suspend fun me(@Header("Authorization") bearerToken: String): Response<ApiUser>
}