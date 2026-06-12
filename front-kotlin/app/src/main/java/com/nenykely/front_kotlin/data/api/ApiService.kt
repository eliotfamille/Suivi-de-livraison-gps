package com.nenykely.front_kotlin.data.api

import com.nenykely.front_kotlin.data.models.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
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
    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body body: Map<String, String>): Response<Unit>

    @Headers("Accept: application/json")
    @POST("api/auth/logout")
    suspend fun logout(@Header("Authorization") token: String): Response<Unit>

    @Headers("Accept: application/json")
    @GET("api/auth/me")
    suspend fun me(@Header("Authorization") token: String): Response<User>

    @Headers("Accept: application/json")
    @PATCH("api/auth/profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body body: Map<String, String?>
    ): Response<User>

    @Headers("Accept: application/json")
    @Multipart
    @POST("api/auth/profile")
    suspend fun updateProfileMultipart(
        @Header("Authorization") token: String,
        @Part("name") name: okhttp3.RequestBody?,
        @Part("phone") phone: okhttp3.RequestBody?,
        @Part("domicile") domicile: okhttp3.RequestBody?,
        @Part("domicile_lat") domicile_lat: okhttp3.RequestBody?,
        @Part("domicile_lng") domicile_lng: okhttp3.RequestBody?,
        @Part("vehicle_type") vehicle_type: okhttp3.RequestBody? = null,
        @Part("vehicle_model") vehicle_model: okhttp3.RequestBody? = null,
        @Part("vehicle_plate") vehicle_plate: okhttp3.RequestBody? = null,
        @Part avatar: okhttp3.MultipartBody.Part?,
        @Part("_method") method: okhttp3.RequestBody
    ): Response<User>

    @Headers("Accept: application/json")
    @GET("api/users")
    suspend fun getUsers(
        @Header("Authorization") token: String,
        @Query("search") search: String? = null
    ): Response<List<User>>

    @Headers("Accept: application/json")
    @GET("api/tracking/{identifier}")
    suspend fun tracking(@Path("identifier") identifier: String): Response<Delivery>

    @Headers("Accept: application/json")
    @GET("api/deliveries")
    suspend fun getDeliveries(@Header("Authorization") token: String): Response<List<Delivery>>

    @Headers("Accept: application/json")
    @POST("api/deliveries")
    suspend fun storeDelivery(
        @Header("Authorization") token: String,
        @Body request: StoreDeliveryRequest
    ): Response<DeliveryResponse>

    @Headers("Accept: application/json")
    @POST("api/deliveries/{id}/accept")
    suspend fun acceptDelivery(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Delivery>

    @Headers("Accept: application/json")
    @GET("api/deliveries/{id}")
    suspend fun getDelivery(@Header("Authorization") token: String, @Path("id") id: Int): Response<Delivery>

    @Headers("Accept: application/json")
    @PATCH("api/deliveries/{id}/status")
    suspend fun updateDeliveryStatus(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body body: Map<String, String?>
    ): Response<Delivery>

    @Headers("Accept: application/json")
    @Multipart
    @POST("api/deliveries/{id}/status")
    suspend fun updateDeliveryStatusMultipart(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Part("status") status: okhttp3.RequestBody,
        @Part("signature") signature: okhttp3.RequestBody?,
        @Part("latitude") latitude: okhttp3.RequestBody?,
        @Part("longitude") longitude: okhttp3.RequestBody?,
        @Part("note") note: okhttp3.RequestBody?,
        @Part proof_photo: okhttp3.MultipartBody.Part?,
        @Part("_method") method: okhttp3.RequestBody
    ): Response<Delivery>

    @Headers("Accept: application/json")
    @POST("api/deliveries/{id}/rate")
    suspend fun rateDelivery(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body body: Map<String, Int>
    ): Response<Unit>

    @Headers("Accept: application/json")
    @GET("api/deliveries/{id}/receipt")
    @Streaming
    suspend fun downloadReceipt(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<okhttp3.ResponseBody>

    @Headers("Accept: application/json")
    @GET("api/driver/deliveries")
    suspend fun getDriverDeliveries(@Header("Authorization") token: String): Response<List<Delivery>>

    @Headers("Accept: application/json")
    @POST("api/driver/location")
    suspend fun updateDriverLocation(
        @Header("Authorization") token: String,
        @Body location: Map<String, Double>
    ): Response<Unit>

    @Headers("Accept: application/json")
    @PUT("api/driver/location")
    suspend fun updateDriverLocationV2(
        @Header("Authorization") token: String,
        @Body location: Map<String, Double>
    ): Response<Unit>
}
