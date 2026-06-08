package com.nenykely.front_kotlin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nenykely.front_kotlin.data.DeliveryRepository
import com.nenykely.front_kotlin.data.models.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    fun fetchDeliveries(token: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.getDeliveries(token)
                if (response.isSuccessful) {
                    _deliveries.value = response.body() ?: emptyList()
                    android.util.Log.d("DeliveryViewModel", "Fetched ${_deliveries.value.size} deliveries")
                } else {
                    android.util.Log.e("DeliveryViewModel", "Error fetching deliveries: ${response.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("DeliveryViewModel", "Exception fetching: ${e.message}")
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
                // Ignore
            }
        }
    }

    fun fetchTracking(token: String, deliveryId: Int) {
        viewModelScope.launch {
            try {
                val response = repository.getDelivery(token, deliveryId)
                if (response.isSuccessful) {
                    _currentDelivery.value = response.body()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun submitDelivery(token: String, data: Map<String, Any?>, onTestResult: (String) -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // LOG ÉTAPE 1 : Envoi par Kotlin
                android.util.Log.d("TEST_COMM", "ÉTAPE 1 : Kotlin envoie -> $data")
                
                val request = StoreDeliveryRequest(
                    description = data["description"] as? String ?: "",
                    weight_kg = (data["weight_kg"] as? Double) ?: 0.0,
                    recipient_name = data["recipient_name"] as? String ?: "",
                    recipient_phone = data["recipient_phone"] as? String ?: "",
                    recipient_address = data["recipient_address"] as? String ?: "",
                    recipient_lat = (data["recipient_lat"] as? Double) ?: 0.0,
                    recipient_lng = (data["recipient_lng"] as? Double) ?: 0.0,
                    sender_name = data["sender_name"] as? String ?: "",
                    sender_phone = data["sender_phone"] as? String ?: "",
                    sender_address = data["sender_address"] as? String ?: "",
                    sender_lat = (data["sender_lat"] as? Double) ?: 0.0,
                    sender_lng = (data["sender_lng"] as? Double) ?: 0.0
                )
                
                val response = repository.storeDelivery(token, request)
                if (response.isSuccessful) {
                    val body = response.body()
                    val msg = body?.message ?: "Succès sans message"
                    
                    // LOG ÉTAPE 5 : Retour dans Kotlin
                    android.util.Log.d("TEST_COMM", "ÉTAPE 5 : Kotlin a reçu réponse -> $msg")
                    onTestResult(msg)
                    
                    fetchDeliveries(token)
                } else {
                    val error = response.errorBody()?.string()
                    android.util.Log.e("TEST_COMM", "ERREUR ÉTAPE 2/3 : $error")
                    onTestResult("Erreur Serveur : $error")
                }
            } catch (e: Exception) {
                android.util.Log.e("TEST_COMM", "ERREUR CRITIQUE : ${e.message}")
                onTestResult("Crash : ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun acceptDelivery(token: String, deliveryId: Int, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                android.util.Log.d("DELIVERY_TEST", "TENTATIVE : Acceptation de la livraison #$deliveryId")
                val response = repository.acceptDelivery(token, deliveryId)
                if (response.isSuccessful) {
                    _currentDelivery.value = response.body()
                    android.util.Log.d("DELIVERY_TEST", "SUCCÈS : Livraison #$deliveryId acceptée. Nouveau statut: ${_currentDelivery.value?.status}")
                    onSuccess()
                } else {
                    android.util.Log.e("DELIVERY_TEST", "ERREUR : Échec acceptation #${deliveryId}. Code: ${response.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("DELIVERY_TEST", "CRASH acceptDelivery: ${e.message}")
            }
        }
    }

    fun updateStatus(token: String, deliveryId: Int, status: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                android.util.Log.d("DELIVERY_TEST", "TENTATIVE : Mise à jour statut livraison #$deliveryId vers -> $status")
                val response = repository.updateDeliveryStatus(token, deliveryId, status)
                if (response.isSuccessful) {
                    _currentDelivery.value = response.body()
                    android.util.Log.d("DELIVERY_TEST", "SUCCÈS : Nouveau statut pour #$deliveryId est: ${_currentDelivery.value?.status}")
                    onSuccess()
                    
                    // Start/Stop location sharing based on status
                    if (status == "in_transit") {
                        startLocationSharing(token)
                    } else if (status == "delivered" || status == "failed") {
                        stopLocationSharing()
                    }
                } else {
                    android.util.Log.e("DELIVERY_TEST", "ERREUR : Échec mise à jour statut #${deliveryId}. Code: ${response.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("DELIVERY_TEST", "CRASH updateStatus: ${e.message}")
            }
        }
    }

    private fun startLocationSharing(token: String) {
        if (_isSharingLocation.value) return
        _isSharingLocation.value = true
        
        viewModelScope.launch {
            while (_isSharingLocation.value) {
                // Simulation d'envoi de position GPS
                // En production, on utiliserait FusedLocationProviderClient
                repository.updateDriverLocation(token, -18.8792 + (Math.random() - 0.5) * 0.01, 47.5079 + (Math.random() - 0.5) * 0.01)
                delay(10000) // Toutes les 10 secondes
            }
        }
    }

    fun stopLocationSharing() {
        _isSharingLocation.value = false
    }
}
