package com.nenykely.front_kotlin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nenykely.front_kotlin.data.DeliveryRepository
import com.nenykely.front_kotlin.data.models.RegisterRequest
import com.nenykely.front_kotlin.data.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: DeliveryRepository = DeliveryRepository()) : ViewModel() {
    private val _user = MutableStateFlow<User?>(null)
    val user = _user.asStateFlow()

    private val _token = MutableStateFlow<String?>(null)
    val token = _token.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = repository.login(email, password)
                if (response.isSuccessful) {
                    val authResponse = response.body()
                    _user.value = authResponse?.user
                    _token.value = authResponse?.token
                    onSuccess()
                } else {
                    val errorBody = response.errorBody()?.string()
                    _error.value = "Login failed: ${response.code()}\n$errorBody"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        _user.value = null
        _token.value = null
    }

    fun register(name: String, email: String, password: String, phone: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = repository.register(
                    RegisterRequest(
                        name = name,
                        email = email,
                        password = password,
                        password_confirmation = password,
                        phone = if (phone.isNullOrBlank()) null else phone
                    )
                )
                if (response.isSuccessful) {
                    val authResponse = response.body()
                    _user.value = authResponse?.user
                    _token.value = authResponse?.token
                    onSuccess()
                } else {
                    val errorBody = response.errorBody()?.string()
                    _error.value = "Registration failed: ${response.code()}\n$errorBody"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
}
