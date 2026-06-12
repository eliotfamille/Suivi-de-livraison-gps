package com.nenykely.front_kotlin.data

import android.content.Context
import com.google.gson.Gson
import com.nenykely.front_kotlin.data.api.RetrofitClient
import com.nenykely.front_kotlin.data.models.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response

class DeliveryRepository(context: Context? = null) {
    private val api = RetrofitClient.instance
    private val dao = context?.let { AppDatabase.getDatabase(it).deliveryDao() }
    private val gson = Gson()

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
        vehicle_type: okhttp3.RequestBody? = null,
        vehicle_model: okhttp3.RequestBody? = null,
        vehicle_plate: okhttp3.RequestBody? = null,
        avatar: okhttp3.MultipartBody.Part?
    ) = api.updateProfileMultipart(
        "Bearer $token", name, phone, domicile, domicile_lat, domicile_lng, 
        vehicle_type, vehicle_model, vehicle_plate, avatar,
        "PATCH".toRequestBody("text/plain".toMediaTypeOrNull())
    )

    suspend fun getUsers(token: String, query: String? = null) = 
        api.getUsers("Bearer $token", query)

    suspend fun tracking(identifier: String) = api.tracking(identifier)

    suspend fun getDeliveries(token: String): Response<List<Delivery>> {
        return try {
            val response = api.getDeliveries("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                // Save to local cache
                dao?.let { d ->
                    val entities = response.body()!!.map { 
                        DeliveryEntity(it.id, it.status, gson.toJson(it))
                    }
                    d.clearAll()
                    d.insertDeliveries(entities)
                }
            }
            response
        } catch (e: Exception) {
            // Load from cache if network fails
            val cached = dao?.getAllDeliveries()
            if (!cached.isNullOrEmpty()) {
                val deliveries = cached.map { gson.fromJson(it.deliveryJson, Delivery::class.java) }
                Response.success(deliveries)
            } else {
                throw e
            }
        }
    }

    suspend fun storeDelivery(token: String, request: StoreDeliveryRequest): Response<DeliveryResponse> =
        api.storeDelivery("Bearer $token", request)

    suspend fun acceptDelivery(token: String, id: Int) = 
        api.acceptDelivery("Bearer $token", id)

    suspend fun getDelivery(token: String, id: Int): Response<Delivery> {
        return try {
            api.getDelivery("Bearer $token", id)
        } catch (e: Exception) {
            val cached = dao?.getAllDeliveries()?.find { it.id == id }
            if (cached != null) {
                Response.success(gson.fromJson(cached.deliveryJson, Delivery::class.java))
            } else {
                throw e
            }
        }
    }

    suspend fun updateDeliveryStatus(token: String, id: Int, body: Map<String, String?>) = 
        api.updateDeliveryStatus("Bearer $token", id, body)

    suspend fun updateDeliveryStatusMultipart(
        token: String,
        id: Int,
        status: okhttp3.RequestBody,
        signature: okhttp3.RequestBody?,
        lat: okhttp3.RequestBody?,
        lng: okhttp3.RequestBody?,
        note: okhttp3.RequestBody?,
        photo: okhttp3.MultipartBody.Part?
    ) = api.updateDeliveryStatusMultipart(
        "Bearer $token", id, status, signature, lat, lng, note, photo,
        "PATCH".toRequestBody("text/plain".toMediaTypeOrNull())
    )

    suspend fun rateDelivery(token: String, id: Int, rating: Int) =
        api.rateDelivery("Bearer $token", id, mapOf("rating" to rating))

    suspend fun downloadReceipt(token: String, id: Int) = 
        api.downloadReceipt("Bearer $token", id)

    suspend fun getDriverDeliveries(token: String) = api.getDriverDeliveries("Bearer $token")

    suspend fun updateDriverLocation(token: String, lat: Double, lng: Double) = 
        api.updateDriverLocation("Bearer $token", mapOf("latitude" to lat, "longitude" to lng))
}
