package com.nenykely.front_kotlin.data

import com.nenykely.front_kotlin.data.api.RetrofitClient
import com.nenykely.front_kotlin.data.models.*
import retrofit2.Response

class DeliveryRepository {
    private val api = RetrofitClient.instance

    suspend fun register(request: RegisterRequest) = api.register(request)

    suspend fun login(email: String, password: String) = api.login(LoginRequest(email, password))

    suspend fun logout(token: String) = api.logout("Bearer $token")

    suspend fun me(token: String) = api.me("Bearer $token")

    suspend fun updateProfile(token: String, body: Map<String, String?>) = 
        api.updateProfile("Bearer $token", body)

    suspend fun updateProfileMultipart(
        token: String,
        name: okhttp3.RequestBody?,
        phone: okhttp3.RequestBody?,
        domicile: okhttp3.RequestBody?,
        domicile_lat: okhttp3.RequestBody?,
        domicile_lng: okhttp3.RequestBody?,
        avatar: okhttp3.MultipartBody.Part?
    ) = api.updateProfileMultipart("Bearer $token", name, phone, domicile, domicile_lat, domicile_lng, avatar)

    suspend fun getUsers(token: String, query: String? = null) = 
        api.getUsers("Bearer $token", query)

    suspend fun tracking(identifier: String) = api.tracking(identifier)

    suspend fun getDeliveries(token: String) = api.getDeliveries("Bearer $token")

    suspend fun storeDelivery(token: String, request: StoreDeliveryRequest): Response<DeliveryResponse> =
        api.storeDelivery("Bearer $token", request)

    suspend fun acceptDelivery(token: String, id: Int) = 
        api.acceptDelivery("Bearer $token", id)

    suspend fun getDelivery(token: String, id: Int) = api.getDelivery("Bearer $token", id)

    suspend fun updateDeliveryStatus(token: String, id: Int, status: String) = 
        api.updateDeliveryStatus("Bearer $token", id, mapOf("status" to status))

    suspend fun getDriverDeliveries(token: String) = api.getDriverDeliveries("Bearer $token")

    suspend fun updateDriverLocation(token: String, lat: Double, lng: Double) = 
        api.updateDriverLocation("Bearer $token", mapOf("latitude" to lat, "longitude" to lng))
}
