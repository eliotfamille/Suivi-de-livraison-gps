package com.nenykely.front_kotlin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nenykely.front_kotlin.data.DeliveryRepository
import com.nenykely.front_kotlin.data.models.RegisterRequest
import com.nenykely.front_kotlin.data.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

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
                    when (response.code()) {
                        401 -> _error.value = "Mot de passe incorrect"
                        404 -> _error.value = "Compte inexistant"
                        else -> _error.value = "Email ou mot de passe incorrect"
                    }
                }
            } catch (e: Exception) {
                _error.value = "Erreur de connexion au serveur"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        _user.value = null
        _token.value = null
    }

    fun fetchProfile() {
        val currentToken = _token.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.me(currentToken)
                if (response.isSuccessful) {
                    _user.value = response.body()
                }
            } catch (e: Exception) {
                _error.value = "Erreur de chargement du profil"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateProfile(
        name: String? = null,
        phone: String? = null,
        domicile: String? = null,
        domicile_lat: Double? = null,
        domicile_lng: Double? = null,
        bureau: String? = null,
        bureau_lat: Double? = null,
        bureau_lng: Double? = null,
        imageFile: File? = null
    ) {
        val currentToken = _token.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = if (imageFile != null) {
                    val namePart = name?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val phonePart = phone?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val domicilePart = domicile?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val latPart = domicile_lat?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val lngPart = domicile_lng?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
                    
                    val imagePart = MultipartBody.Part.createFormData(
                        "avatar",
                        imageFile.name,
                        imageFile.asRequestBody("image/*".toMediaTypeOrNull())
                    )
                    
                    repository.updateProfileMultipart(
                        currentToken, namePart, phonePart, domicilePart, latPart, lngPart, imagePart
                    )
                } else {
                    val body = mutableMapOf<String, String?>()
                    name?.let { body["name"] = it }
                    phone?.let { body["phone"] = it }
                    domicile?.let { body["domicile"] = it }
                    domicile_lat?.let { body["domicile_lat"] = it.toString() }
                    domicile_lng?.let { body["domicile_lng"] = it.toString() }
                    bureau?.let { body["bureau"] = it }
                    bureau_lat?.let { body["bureau_lat"] = it.toString() }
                    bureau_lng?.let { body["bureau_lng"] = it.toString() }
                    repository.updateProfile(currentToken, body)
                }

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
                    when (response.code()) {
                        422 -> _error.value = "Email déjà utilisé"
                        else -> _error.value = "Données invalides ou erreur serveur"
                    }
                }
            } catch (e: Exception) {
                _error.value = "Erreur de connexion au serveur"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
