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

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

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
                    android.util.Log.d("AuthViewModel", "User logged in: ${authResponse?.user?.name}, roles: ${authResponse?.user?.roles}")
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

    fun updateProfile(domicile: String?, bureau: String?) {
        val currentToken = _token.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.updateProfile(
                    currentToken,
                    mapOf("domicile" to domicile, "bureau" to bureau)
                )
                if (response.isSuccessful) {
                    _user.value = response.body()
                } else {
                    _error.value = "Update failed: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(name: String, email: String, password: String, phone: String?, role: String = "client", onSuccess: () -> Unit) {
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
                        phone = if (phone.isNullOrBlank()) null else phone,
                        role = role
                    )
                )
                if (response.isSuccessful) {
                    val authResponse = response.body()
                    _user.value = authResponse?.user
                    _token.value = authResponse?.token
                    android.util.Log.d("AuthViewModel", "User logged in: ${authResponse?.user?.name}, roles: ${authResponse?.user?.roles}")
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
