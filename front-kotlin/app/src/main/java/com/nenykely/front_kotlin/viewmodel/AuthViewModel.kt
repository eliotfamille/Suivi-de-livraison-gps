package com.nenykely.front_kotlin.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nenykely.front_kotlin.data.DeliveryRepository
import com.nenykely.front_kotlin.data.models.RegisterRequest
import com.nenykely.front_kotlin.data.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class AuthViewModel(application: android.app.Application) : AndroidViewModel(application) {
    private val depot: DeliveryRepository = DeliveryRepository(application)

    private val _utilisateur = MutableStateFlow<User?>(null)
    val utilisateur = _utilisateur.asStateFlow()

    private val _jeton = MutableStateFlow<String?>(null)
    val jeton = _jeton.asStateFlow()

    private val _estEnChargement = MutableStateFlow(false)
    val estEnChargement = _estEnChargement.asStateFlow()

    private val _erreur = MutableStateFlow<String?>(null)
    val erreur = _erreur.asStateFlow()

    private val _estModeSombre = MutableStateFlow(false)
    val estModeSombre = _estModeSombre.asStateFlow()

    fun basculerModeSombre() {
        _estModeSombre.value = !_estModeSombre.value
    }

    fun seConnecter(courriel: String, motDePasse: String, lorsSucces: () -> Unit) {
        viewModelScope.launch {
            _estEnChargement.value = true
            _erreur.value = null
            try {
                val reponse = depot.login(courriel, motDePasse)
                if (reponse.isSuccessful) {
                    val reponseAuth = reponse.body()
                    _utilisateur.value = reponseAuth?.user
                    _jeton.value = reponseAuth?.token
                    android.util.Log.d("AuthViewModel", "Utilisateur connecté : ${reponseAuth?.user?.name}")
                    lorsSucces()
                } else {
                    when (reponse.code()) {
                        401 -> _erreur.value = "Mot de passe incorrect"
                        404 -> _erreur.value = "Compte inexistant"
                        else -> _erreur.value = "Email ou mot de passe incorrect"
                    }
                }
            } catch (e: Exception) {
                _erreur.value = "Erreur de connexion au serveur"
            } finally {
                _estEnChargement.value = false
            }
        }
    }

    fun reinitialiserMotDePasse(courriel: String, nouveauMdp: String, lorsSucces: () -> Unit) {
        viewModelScope.launch {
            _estEnChargement.value = true
            _erreur.value = null
            try {
                val reponse = depot.resetPassword(courriel, nouveauMdp)
                if (reponse.isSuccessful) {
                    lorsSucces()
                } else {
                    _erreur.value = "Email non trouvé ou erreur lors de la mise à jour"
                }
            } catch (e: Exception) {
                _erreur.value = "Erreur de connexion"
            } finally {
                _estEnChargement.value = false
            }
        }
    }

    fun seDeconnecter() {
        _utilisateur.value = null
        _jeton.value = null
        _erreur.value = null
    }

    fun recupererProfil() {
        val jetonActuel = _jeton.value ?: return
        viewModelScope.launch {
            _estEnChargement.value = true
            try {
                val reponse = depot.me(jetonActuel)
                if (reponse.isSuccessful) {
                    _utilisateur.value = reponse.body()
                }
            } catch (e: Exception) {
                _erreur.value = "Erreur de chargement du profil"
            } finally {
                _estEnChargement.value = false
            }
        }
    }

    fun mettreAJourProfil(
        nom: String? = null,
        telephone: String? = null,
        domicile: String? = null,
        domicile_lat: Double? = null,
        domicile_lng: Double? = null,
        bureau: String? = null,
        bureau_lat: Double? = null,
        bureau_lng: Double? = null,
        type_vehicule: String? = null,
        modele_vehicule: String? = null,
        plaque_vehicule: String? = null,
        fichierImage: File? = null
    ) {
        val jetonActuel = _jeton.value ?: return
        viewModelScope.launch {
            _estEnChargement.value = true
            try {
                val reponse = if (fichierImage != null) {
                    val partieNom = nom?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val partieTel = telephone?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val partieDomicile = domicile?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val partieLat = domicile_lat?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val partieLng = domicile_lng?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val partieTypeV = type_vehicule?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val partieModelV = modele_vehicule?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val partiePlaqueV = plaque_vehicule?.toRequestBody("text/plain".toMediaTypeOrNull())
                    
                    val partieImage = MultipartBody.Part.createFormData(
                        "avatar",
                        fichierImage.name,
                        fichierImage.asRequestBody("image/*".toMediaTypeOrNull())
                    )
                    
                    depot.updateProfileMultipart(
                        jetonActuel, partieNom, partieTel, partieDomicile, partieLat, partieLng, 
                        partieTypeV, partieModelV, partiePlaqueV, partieImage
                    )
                } else {
                    val corps = mutableMapOf<String, String?>()
                    nom?.let { corps["name"] = it }
                    telephone?.let { corps["phone"] = it }
                    domicile?.let { corps["domicile"] = it }
                    domicile_lat?.let { corps["domicile_lat"] = it.toString() }
                    domicile_lng?.let { corps["domicile_lng"] = it.toString() }
                    bureau?.let { corps["bureau"] = it }
                    bureau_lat?.let { corps["bureau_lat"] = it.toString() }
                    bureau_lng?.let { corps["bureau_lng"] = it.toString() }
                    type_vehicule?.let { corps["vehicle_type"] = it }
                    modele_vehicule?.let { corps["vehicle_model"] = it }
                    plaque_vehicule?.let { corps["vehicle_plate"] = it }
                    depot.updateProfile(jetonActuel, corps)
                }

                if (reponse.isSuccessful) {
                    _utilisateur.value = reponse.body()
                } else {
                    _erreur.value = "Échec de la mise à jour : ${reponse.code()}"
                }
            } catch (e: Exception) {
                _erreur.value = e.message
            } finally {
                _estEnChargement.value = false
            }
        }
    }

    fun sinscrire(
        nom: String, 
        courriel: String, 
        motDePasse: String, 
        telephone: String?, 
        role: String = "client",
        typeVehicule: String? = null,
        modeleVehicule: String? = null,
        plaqueVehicule: String? = null,
        lorsSucces: () -> Unit
    ) {
        viewModelScope.launch {
            _estEnChargement.value = true
            _erreur.value = null
            try {
                val reponse = depot.register(
                    RegisterRequest(
                        name = nom,
                        email = courriel,
                        password = motDePasse,
                        password_confirmation = motDePasse,
                        phone = if (telephone.isNullOrBlank()) null else telephone,
                        role = role,
                        vehicle_type = typeVehicule,
                        vehicle_model = modeleVehicule,
                        vehicle_plate = plaqueVehicule
                    )
                )
                if (reponse.isSuccessful) {
                    val reponseAuth = reponse.body()
                    _utilisateur.value = reponseAuth?.user
                    _jeton.value = reponseAuth?.token
                    android.util.Log.d("AuthViewModel", "Compte créé : ${reponseAuth?.user?.name}")
                    lorsSucces()
                } else {
                    when (reponse.code()) {
                        422 -> _erreur.value = "Email déjà utilisé"
                        else -> _erreur.value = "Données invalides ou erreur serveur"
                    }
                }
            } catch (e: Exception) {
                _erreur.value = "Erreur de connexion au serveur"
            } finally {
                _estEnChargement.value = false
            }
        }
    }
}
