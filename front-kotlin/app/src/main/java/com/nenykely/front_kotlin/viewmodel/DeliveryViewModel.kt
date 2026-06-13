package com.nenykely.front_kotlin.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nenykely.front_kotlin.data.DeliveryRepository
import com.nenykely.front_kotlin.data.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class DeliveryViewModel(application: android.app.Application) : AndroidViewModel(application) {
    private val depot: DeliveryRepository = DeliveryRepository(application)

    private val _livraisons = MutableStateFlow<List<Delivery>>(emptyList())
    val livraisons = _livraisons.asStateFlow()

    private val _livraisonActuelle = MutableStateFlow<Delivery?>(null)
    val livraisonActuelle = _livraisonActuelle.asStateFlow()

    private val _estEnChargement = MutableStateFlow(false)
    val estEnChargement = _estEnChargement.asStateFlow()

    private val _utilisateurs = MutableStateFlow<List<User>>(emptyList())
    val utilisateurs = _utilisateurs.asStateFlow()

    private val _partageLocalisation = MutableStateFlow(false)
    val partageLocalisation = _partageLocalisation.asStateFlow()

    private val _photoPreuve = MutableStateFlow<File?>(null)
    val photoPreuve = _photoPreuve.asStateFlow()

    private val _erreur = MutableStateFlow<String?>(null)
    val erreur = _erreur.asStateFlow()

    private var derniereLat: Double? = null
    private var derniereLng: Double? = null

    fun definirPhotoPreuve(fichier: File?) {
        _photoPreuve.value = fichier
    }

    fun recupererLivraisons(jeton: String) {
        if (jeton.isBlank()) return
        viewModelScope.launch {
            chargerLivraisons(jeton)
        }
    }

    private suspend fun chargerLivraisons(jeton: String) {
        _estEnChargement.value = true
        try {
            val reponse = depot.getDeliveries(jeton)
            if (reponse.isSuccessful) {
                _livraisons.value = reponse.body() ?: emptyList()
            } else if (reponse.code() == 401) {
                _erreur.value = "Session expirée"
            }
        } catch (e: Exception) {
            android.util.Log.e("DeliveryViewModel", "Erreur de récupération : ${e.message}")
        } finally {
            _estEnChargement.value = false
        }
    }

    fun rechercherUtilisateurs(jeton: String, requete: String) {
        viewModelScope.launch {
            try {
                val reponse = depot.getUsers(jeton, requete)
                if (reponse.isSuccessful) {
                    _utilisateurs.value = reponse.body() ?: emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("DeliveryViewModel", "Erreur de recherche : ${e.message}")
            }
        }
    }

    fun recupererSuivi(jeton: String, livraisonId: Int) {
        viewModelScope.launch {
            _estEnChargement.value = true
            try {
                val reponse = depot.getDelivery(jeton, livraisonId)
                if (reponse.isSuccessful) {
                    _livraisonActuelle.value = reponse.body()
                }
            } catch (e: Exception) {
                android.util.Log.e("DeliveryViewModel", "Erreur de suivi : ${e.message}")
            } finally {
                _estEnChargement.value = false
            }
        }
    }

    fun soumettreLivraison(jeton: String, donnees: Map<String, Any?>, lorsResultat: (String) -> Unit) {
        viewModelScope.launch {
            _estEnChargement.value = true
            try {
                val req = StoreDeliveryRequest(
                    description = donnees["description"] as String,
                    weight_kg = donnees["weight_kg"] as Double,
                    recipient_name = donnees["recipient_name"] as String,
                    recipient_phone = donnees["recipient_phone"] as String,
                    recipient_address = donnees["recipient_address"] as String,
                    recipient_lat = donnees["recipient_lat"] as Double,
                    recipient_lng = donnees["recipient_lng"] as Double,
                    sender_name = donnees["sender_name"] as String,
                    sender_phone = donnees["sender_phone"] as String,
                    sender_address = donnees["sender_address"] as String,
                    sender_lat = donnees["sender_lat"] as Double,
                    sender_lng = donnees["sender_lng"] as Double
                )
                val reponse = depot.storeDelivery(jeton, req)
                if (reponse.isSuccessful) {
                    chargerLivraisons(jeton)
                    lorsResultat("Livraison enregistrée avec succès")
                } else {
                    lorsResultat("Erreur lors de l'enregistrement : ${reponse.code()}")
                }
            } catch (e: Exception) {
                lorsResultat("Erreur de connexion : ${e.message}")
            } finally {
                _estEnChargement.value = false
            }
        }
    }

    fun accepterLivraison(jeton: String, livraisonId: Int, lorsSucces: () -> Unit) {
        viewModelScope.launch {
            _estEnChargement.value = true
            try {
                val reponse = depot.acceptDelivery(jeton, livraisonId)
                if (reponse.isSuccessful) {
                    chargerLivraisons(jeton)
                    lorsSucces()
                } else {
                    if (reponse.code() == 401) _erreur.value = "Erreur d'authentification (401)"
                    else _erreur.value = "Erreur ${reponse.code()} lors de l'acceptation"
                }
            } catch (e: Exception) {
                android.util.Log.e("DeliveryViewModel", "Erreur acceptation : ${e.message}")
                _erreur.value = "Erreur de connexion"
            } finally {
                _estEnChargement.value = false
            }
        }
    }

    fun mettreAJourStatut(
        jeton: String, 
        livraisonId: Int, 
        statut: String, 
        lat: Double? = null,
        lng: Double? = null,
        fichierPhoto: File? = null,
        signatureBase64: String? = null,
        lorsSucces: () -> Unit = {}
    ) {
        val latFinale = lat ?: derniereLat
        val lngFinale = lng ?: derniereLng

        viewModelScope.launch {
            _estEnChargement.value = true
            _erreur.value = null
            try {
                val reponse = if (fichierPhoto != null || !signatureBase64.isNullOrBlank()) {
                    val statutRB = statut.toRequestBody("text/plain".toMediaTypeOrNull())
                    val latRB = latFinale?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val lngRB = lngFinale?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val noteRB = "Mise à jour via application".toRequestBody("text/plain".toMediaTypeOrNull())
                    val signatureRB = signatureBase64?.toRequestBody("text/plain".toMediaTypeOrNull())

                    val partiePhoto = fichierPhoto?.let {
                        MultipartBody.Part.createFormData(
                            "proof_photo", it.name,
                            it.asRequestBody("image/*".toMediaTypeOrNull())
                        )
                    }
                    
                    depot.updateDeliveryStatusMultipart(jeton, livraisonId, statutRB, signatureRB, latRB, lngRB, noteRB, partiePhoto)
                } else {
                    val corps = mutableMapOf<String, String?>(
                        "status" to statut,
                        "latitude" to latFinale?.toString(),
                        "longitude" to lngFinale?.toString()
                    )
                    depot.updateDeliveryStatus(jeton, livraisonId, corps)
                }
                
                if (reponse.isSuccessful) {
                    _livraisonActuelle.value = reponse.body()
                    lorsSucces()
                } else {
                    _erreur.value = "Erreur ${reponse.code()} : Vérifiez la validation serveur"
                }
            } catch (e: Exception) {
                _erreur.value = "Erreur de connexion"
            } finally {
                _estEnChargement.value = false
            }
        }
    }

    fun noterLivraison(jeton: String, livraisonId: Int, note: Int, lorsSucces: () -> Unit = {}) {
        viewModelScope.launch {
            _estEnChargement.value = true
            try {
                val reponse = depot.rateDelivery(jeton, livraisonId, note)
                if (reponse.isSuccessful) {
                    recupererSuivi(jeton, livraisonId)
                    lorsSucces()
                }
            } catch (e: Exception) {
                android.util.Log.e("DeliveryViewModel", "Erreur notation : ${e.message}")
            } finally {
                _estEnChargement.value = false
            }
        }
    }

    fun mettreAJourLocalisation(jeton: String, lat: Double, lng: Double) {
        if (jeton.isBlank() || lat == 0.0) return
        derniereLat = lat
        derniereLng = lng
        viewModelScope.launch {
            try {
                val reponse = depot.updateDriverLocation(jeton, lat, lng)
                if (!reponse.isSuccessful) {
                    android.util.Log.e("DeliveryViewModel", "Mise à jour localisation échouée : ${reponse.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("DeliveryViewModel", "Erreur localisation : ${e.message}")
            }
        }
    }

    fun telechargerRecu(contexte: android.content.Context, jeton: String, livraisonId: Int, nomFichier: String) {
        viewModelScope.launch {
            try {
                val reponse = depot.downloadReceipt(jeton, livraisonId)
                if (reponse.isSuccessful) {
                    val corps = reponse.body()
                    if (corps != null) {
                        val fichier = File(contexte.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), nomFichier)
                        val fluxEntree = corps.byteStream()
                        val fluxSortie = FileOutputStream(fichier)
                        val tampon = ByteArray(4096)
                        var lu: Int
                        while (fluxEntree.read(tampon).also { lu = it } != -1) {
                            fluxSortie.write(tampon, 0, lu)
                        }
                        fluxSortie.flush()
                        fluxSortie.close()
                        fluxEntree.close()
                        
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            contexte,
                            "${contexte.packageName}.provider",
                            fichier
                        )
                        val intention = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/pdf")
                            flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        contexte.startActivity(intention)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("DeliveryViewModel", "Erreur téléchargement : ${e.message}")
            }
        }
    }
}
