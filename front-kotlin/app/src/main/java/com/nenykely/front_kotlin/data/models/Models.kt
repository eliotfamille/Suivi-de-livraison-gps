package com.nenykely.front_kotlin.data.models

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String? = null,
    val avatar: String? = null,
    val roles: List<String>? = null
)

data class Package(
    val id: Int,
    val weight_kg: Double,
    val description: String,
    val fragile: String? = "no"
)

data class Status(
    val id: Int,
    val delivery_id: Int,
    val status: String,
    val label: String,
    val note: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val occurred_at: String? = null
)

data class Location(
    val id: Int,
    val latitude: Double,
    val longitude: Double,
    val address: String? = null
)

data class Order(
    val id: Int,
    val client_id: Int,
    val package_id: Int,
    val sender_name: String? = null,
    val sender_phone: String? = null,
    val sender_address: String? = null,
    val sender_lat: Double? = null,
    val sender_lng: Double? = null,
    val recipient_name: String? = null,
    val recipient_phone: String? = null,
    val recipient_address: String? = null,
    val recipient_lat: Double? = null,
    val recipient_lng: Double? = null,
    val priority: String? = "normal",
    val delivery_fee: Double? = 0.0,
    val status: String? = "pending",
    val `package`: Package? = null
)

data class Driver(
    val id: Int,
    val user_id: Int,
    val vehicle_type: String? = null,
    val vehicle_plate: String? = null,
    val vehicle_model: String? = null,
    val status: String? = "offline",
    val rating: Double? = 0.0,
    val total_deliveries: Int? = 0,
    val user: User? = null
)

data class Delivery(
    val id: Int,
    val order_id: Int,
    val driver_id: Int? = null,
    val status: String,
    val assigned_at: String? = null,
    val picked_up_at: String? = null,
    val delivered_at: String? = null,
    val estimated_arrival: String? = null,
    val order: Order? = null,
    val driver: Driver? = null,
    val statuses: List<Status>? = emptyList()
)

data class AuthResponse(
    val token: String,
    val user: User
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val password_confirmation: String,
    val phone: String? = null
)
