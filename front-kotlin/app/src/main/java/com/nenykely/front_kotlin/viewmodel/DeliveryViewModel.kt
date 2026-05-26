package com.nenykely.front_kotlin.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nenykely.front_kotlin.data.DeliveryRepository
import com.nenykely.front_kotlin.data.models.Delivery
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

    fun fetchDeliveries(token: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.getDeliveries(token)
                if (response.isSuccessful) {
                    _deliveries.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
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
}
