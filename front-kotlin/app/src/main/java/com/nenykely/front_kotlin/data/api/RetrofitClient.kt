package com.nenykely.front_kotlin.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val NGROK_URL = "https://unsuspectful-freestanding-ozie.ngrok-free.dev"
    private const val EMULATOR_IP = "10.0.2.2"
    private const val PORT = "8000"

    private val BASE_URL: String by lazy {
        if (isEmulator()) "http://$EMULATOR_IP:$PORT/" else "$NGROK_URL/"
    }

    private fun isEmulator(): Boolean {
        return (android.os.Build.BRAND.startsWith("generic") ||
                android.os.Build.DEVICE.startsWith("generic") ||
                android.os.Build.MODEL.contains("google_sdk") ||
                android.os.Build.MODEL.contains("Emulator") ||
                android.os.Build.MODEL.contains("Android SDK built for x86") ||
                android.os.Build.PRODUCT.contains("sdk_google") ||
                android.os.Build.PRODUCT.contains("google_sdk") ||
                android.os.Build.PRODUCT.contains("sdk") ||
                android.os.Build.PRODUCT.contains("sdk_x86") ||
                android.os.Build.PRODUCT.contains("vbox86p") ||
                android.os.Build.PRODUCT.contains("emulator") ||
                android.os.Build.PRODUCT.contains("simulator"))
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()

        retrofit.create(ApiService::class.java)
    }
}
