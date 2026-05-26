package com.nenykely.front_kotlin.data.api

import com.nenykely.front_kotlin.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @Headers("Accept: application/json")
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @Headers("Accept: application/json")
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @Headers("Accept: application/json")
    @POST("api/auth/logout")
    suspend fun logout(@Header("Authorization") token: String): Response<Unit>

    @Headers("Accept: application/json")
    @GET("api/auth/me")
    suspend fun me(@Header("Authorization") token: String): Response<User>

    @Headers("Accept: application/json")
    @GET("api/tracking/{identifier}")
    suspend fun tracking(@Path("identifier") identifier: String): Response<Delivery>

    @Headers("Accept: application/json")
    @GET("api/deliveries")
    suspend fun getDeliveries(@Header("Authorization") token: String): Response<List<Delivery>>

    @Headers("Accept: application/json")
    @GET("api/deliveries/{id}")
    suspend fun getDelivery(@Header("Authorization") token: String, @Path("id") id: Int): Response<Delivery>

    @Headers("Accept: application/json")
    @PATCH("api/deliveries/{id}/status")
    suspend fun updateDeliveryStatus(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body body: Map<String, String>
    ): Response<Delivery>

    @Headers("Accept: application/json")
    @GET("api/driver/deliveries")
    suspend fun getDriverDeliveries(@Header("Authorization") token: String): Response<List<Delivery>>

    @Headers("Accept: application/json")
    @POST("api/driver/location")
    suspend fun updateDriverLocation(
        @Header("Authorization") token: String,
        @Body location: Map<String, Double>
    ): Response<Unit>
}
