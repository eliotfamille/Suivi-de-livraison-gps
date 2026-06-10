package com.nenykely.front_kotlin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nenykely.front_kotlin.data.DeliveryRepository
import com.nenykely.front_kotlin.data.models.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File

class DeliveryViewModel(private val repository: DeliveryRepository = DeliveryRepository()) : ViewModel() {
    private val _deliveries = MutableStateFlow<List<Delivery>>(emptyList())
    val deliveries = _deliveries.asStateFlow()

    private val _currentDelivery = MutableStateFlow<Delivery?>(null)
    val currentDelivery = _currentDelivery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users = _users.asStateFlow()

    private val _isSharingLocation = MutableStateFlow(false)
    val isSharingLocation = _isSharingLocation.asStateFlow()

    private val _proofPhoto = MutableStateFlow<File?>(null)
    val proofPhoto = _proofPhoto.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private var lastLat: Double? = null
    private var lastLng: Double? = null

    fun setProofPhoto(file: File?) {
        _proofPhoto.value = file
    }

    fun fetchDeliveries(token: String) {
        if (token.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.getDeliveries(token)
                if (response.isSuccessful) {
                    _deliveries.value = response.body() ?: emptyList()
                } else if (response.code() == 401) {
                    _error.value = "Session expirée"
                }
            } catch (e: Exception) {
                android.util.Log.e("DeliveryViewModel", "Fetch error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchUsers(token: String, query: String) {
        viewModelScope.launch {
            try {
                val response = repository.getUsers(token, query)
                if (response.isSuccessful) {
                    _users.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("DeliveryViewModel", "Search error: ${e.message}")
            }
        }
    }

    fun fetchTracking(token: String, deliveryId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.getDelivery(token, deliveryId)
                if (response.isSuccessful) {
                    _currentDelivery.value = response.body()
                }
            } catch (e: Exception) {
                android.util.Log.e("DeliveryViewModel", "Tracking error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitDelivery(token: String, data: Map<String, Any?>, onResult: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val req = StoreDeliveryRequest(
                    description = data["description"] as String,
                    weight_kg = data["weight_kg"] as Double,
                    recipient_name = data["recipient_name"] as String,
                    recipient_phone = data["recipient_phone"] as String,
                    recipient_address = data["recipient_address"] as String,
                    recipient_lat = data["recipient_lat"] as Double,
                    recipient_lng = data["recipient_lng"] as Double,
                    sender_name = data["sender_name"] as String,
                    sender_phone = data["sender_phone"] as String,
                    sender_address = data["sender_address"] as String,
                    sender_lat = data["sender_lat"] as Double,
                    sender_lng = data["sender_lng"] as Double
                )
                val response = repository.storeDelivery(token, req)
                if (response.isSuccessful) {
                    fetchDeliveries(token)
                    onResult("Livraison enregistrée avec succès")
                } else {
                    onResult("Erreur lors de l'enregistrement: ${response.code()}")
                }
            } catch (e: Exception) {
                onResult("Erreur de connexion: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun acceptDelivery(token: String, deliveryId: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val response = repository.acceptDelivery(token, deliveryId)
                if (response.isSuccessful) {
                    fetchDeliveries(token)
                    onSuccess()
                } else {
                    android.util.Log.e("DELIVERY_TEST", "Error accept: ${response.code()}")
                    if (response.code() == 401) _error.value = "Erreur d'authentification (401)"
                }
            } catch (e: Exception) {
                android.util.Log.e("DELIVERY_TEST", "Exception acceptDelivery: ${e.message}")
            }
        }
    }

    fun updateStatus(
        token: String, 
        deliveryId: Int, 
        status: String, 
        lat: Double? = null,
        lng: Double? = null,
        photoFile: File? = null,
        signatureBase64: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        val finalLat = lat ?: lastLat
        val finalLng = lng ?: lastLng

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = if (photoFile != null || !signatureBase64.isNullOrBlank()) {
                    val statusRB = status.toRequestBody("text/plain".toMediaTypeOrNull())
                    val latRB = finalLat?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val lngRB = finalLng?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val noteRB = "Mise à jour via application".toRequestBody("text/plain".toMediaTypeOrNull())
                    
                    // ON ENVOIE LA SIGNATURE EN TANT QUE STRING (Base64) car le serveur l'exige
                    val signatureRB = signatureBase64?.toRequestBody("text/plain".toMediaTypeOrNull())

                    val photoPart = photoFile?.let {
                        MultipartBody.Part.createFormData(
                            "proof_photo", it.name,
                            it.asRequestBody("image/*".toMediaTypeOrNull())
                        )
                    }
                    
                    repository.updateDeliveryStatusMultipart(token, deliveryId, statusRB, signatureRB, latRB, lngRB, noteRB, photoPart)
                } else {
                    val body = mutableMapOf<String, String?>(
                        "status" to status,
                        "lat" to finalLat?.toString(),
                        "lng" to finalLng?.toString()
                    )
                    repository.updateDeliveryStatus(token, deliveryId, body)
                }
                
                if (response.isSuccessful) {
                    _currentDelivery.value = response.body()
                    onSuccess()
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("DELIVERY_ERROR", "Code ${response.code()}: $errorBody")
                    _error.value = "Erreur ${response.code()}: Vérifiez la validation serveur"
                }
            } catch (e: Exception) {
                _error.value = "Erreur connexion"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun rateDelivery(token: String, deliveryId: Int, rating: Int, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.rateDelivery(token, deliveryId, rating)
                if (response.isSuccessful) {
                    fetchTracking(token, deliveryId)
                    onSuccess()
                }
            } catch (e: Exception) {
                android.util.Log.e("DELIVERY_TEST", "Erreur rating: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateLocation(token: String, lat: Double, lng: Double) {
        if (token.isBlank() || lat == 0.0) return
        lastLat = lat
        lastLng = lng
        viewModelScope.launch {
            try {
                repository.updateDriverLocation(token, lat, lng)
            } catch (e: Exception) {
                android.util.Log.e("DeliveryViewModel", "Location update error")
            }
        }
    }
}
