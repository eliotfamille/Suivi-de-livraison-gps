package com.nenykely.front_kotlin.data.models

data class User(
    val id: Int = 0,
    val name: String = "",
    val email: String = "",
    val phone: String? = null,
    val avatar: String? = null,
    val roles: List<String>? = null,
    val role: String? = null,
    val domicile: String? = null,
    val domicile_lat: Double? = null,
    val domicile_lng: Double? = null,
    val bureau: String? = null,
    val bureau_lat: Double? = null,
    val bureau_lng: Double? = null,
    val driver: DriverProfile? = null
) {
    fun isDriver(): Boolean {
        return role?.equals("driver", ignoreCase = true) == true || 
               roles?.any { it.equals("driver", ignoreCase = true) } == true
    }
}

data class AuthResponse(
    val message: String,
    val user: User,
    val token: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val phone: String? = null,
    val password: String,
    val password_confirmation: String,
    val role: String = "client",
    val vehicle_type: String? = null,
    val vehicle_model: String? = null,
    val vehicle_plate: String? = null
)

data class Delivery(
    val id: Int,
    val order_id: Int,
    val driver_id: Int? = null,
    val status: String,
    val proof_photo: String? = null,
    val signature: String? = null,
    val assigned_at: String? = null,
    val picked_up_at: String? = null,
    val delivered_at: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val estimated_arrival: String? = null,
    val rating: Int? = null,
    val order: Order? = null,
    val driver: Driver? = null,
    val statuses: List<Status>? = emptyList()
)

data class Order(
    val id: Int,
    val order_number: String? = null,
    val client_id: Int,
    val package_id: Int,
    val sender_name: String,
    val sender_phone: String? = null,
    val sender_address: String,
    val sender_lat: Double? = null,
    val sender_lng: Double? = null,
    val recipient_name: String,
    val recipient_phone: String? = null,
    val recipient_address: String,
    val recipient_lat: Double? = null,
    val recipient_lng: Double? = null,
    val priority: String? = "normal",
    val delivery_fee: Double? = 0.0,
    val `package`: Package? = null
)

data class Package(
    val id: Int? = null,
    val tracking_code: String? = null,
    val description: String,
    val weight_kg: Double? = null,
    val dimensions: String? = null,
    val fragile: String? = "no"
)

data class Driver(
    val id: Int,
    val user_id: Int? = null,
    val status: String? = null,
    val vehicle_type: String? = null,
    val vehicle_model: String? = null,
    val vehicle_plate: String? = null,
    val rating: Double? = 0.0,
    val rating_count: Int? = 0,
    val total_deliveries: Int? = 0,
    val joined_at: String? = null,
    val current_lat: Double? = null,
    val current_lng: Double? = null,
    val user: User? = null
)

data class DriverProfile(
    val id: Int,
    val status: String? = null,
    val vehicle_type: String? = null,
    val vehicle_model: String? = null,
    val vehicle_plate: String? = null,
    val rating: Double? = 0.0,
    val rating_count: Int? = 0
)

data class Status(
    val id: Int? = null,
    val delivery_id: Int? = null,
    val status: String,
    val label: String,
    val note: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val created_at: String? = null
)

data class DeliveryResponse(
    val message: String,
    val delivery: Delivery
)

data class StoreDeliveryRequest(
    val description: String,
    val weight_kg: Double,
    val recipient_name: String,
    val recipient_phone: String,
    val recipient_address: String,
    val recipient_lat: Double,
    val recipient_lng: Double,
    val sender_name: String,
    val sender_phone: String,
    val sender_address: String,
    val sender_lat: Double,
    val sender_lng: Double
)
