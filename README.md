# Système de Suivi de Livraison GPS (Full-Stack)

Ce projet est une solution complète de gestion et de suivi de livraisons en temps réel, composée d'un backend robuste en Laravel et d'une application mobile moderne en Kotlin/Android.

---

## 📱 Partie Frontend : Android (Kotlin)

L'application mobile est conçue pour offrir une expérience fluide tant pour les clients que pour les livreurs.

### 🏗️ Architecture & Travail Technique
*   **MVVM (Model-View-ViewModel)** : Séparation stricte de la logique métier (ViewModel) et de l'interface (UI).
    *   **ViewModel** : Gère l'état de l'application via `StateFlow` et effectue les appels asynchrones.
    *   **Repository** : Centralise les sources de données (API Retrofit).
*   **Jetpack Compose** : Interface utilisateur 100% déclarative, permettant une réactivité instantanée aux changements de données.
*   **Coroutines & Flows** : Gestion asynchrone des appels réseau et des flux de données en temps réel (comme la position GPS).
*   **Dependency Injection** : Gestion simplifiée des instances via les ViewModels natifs d'Android.

### 🗺️ Cartographie & Temps Réel
*   **osmdroid (OpenStreetMap)** : Alternative gratuite et open-source à Google Maps, intégrée via un `AndroidView`.
*   **Système de Polling** : Mise à jour automatique toutes les 5 secondes sur l'écran de suivi pour simuler le temps réel.
*   **OSRM API** : Utilisation du service de routage Open Source pour tracer les trajectoires routières exactes entre le livreur et le client.
*   **Témoin de Synchro** : Indicateur visuel (Pastille Vert/Rouge) confirmant la fraîcheur des données GPS reçues.

### 🛠️ Actions Techniques Spécifiques
*   **Signature & Photo** : Capture de signature numérique et prise de photo via l'appareil (CameraX/Intents) pour prouver la livraison.
*   **Ngrok Integration** : Détection intelligente de l'environnement (Émulateur vs Physique) pour basculer dynamiquement l'URL de l'API.

---

## ⚙️ Partie Backend : Laravel (API PHP)

Le serveur centralise toute l'intelligence du système, les calculs de tarifs et la sécurité.

### 🚀 Architecture API
*   **REST API** : Communication standardisée en JSON pour une compatibilité universelle.
*   **Laravel Sanctum** : Authentification sécurisée par jetons (Tokens). Chaque mobile possède une clé unique.
*   **Middlewares de Rôles** : Système de filtrage (`RoleMiddleware`) garantissant qu'un client ne peut pas accéder aux fonctions d'un livreur ou d'un admin.

### 📊 Dashboard Admin & KPIs
*   **Temps Moyen de Livraison** : Calcul automatique de la performance globale.
*   **Zones de Livraison & Tarification** :
    *   **Zone Urbaine** (5€) | **Zone Suburbaine** (10€) | **Zone Rurale** (20€).
    *   Calcul dynamique du tarif lors de la création de la commande.
*   **Export Performance** : Génération de rapports CSV/Excel natifs pour l'analyse des livreurs.
*   **Webhooks** : Notification automatique des transporteurs externes lors des changements de statut.

### 📡 Real-Time (WebSockets)
*   **Pusher / Laravel Echo** : Diffusion instantanée de la position des livreurs sur la carte de l'administrateur sans rafraîchissement de page.

---

## 🧪 Qualité & Tests

*   **Tests PHPUnit (≥ 30 tests)** : Couverture complète du système (Auth, Commandes, GPS, Admin) via `php artisan test`.
*   **Database Refresh** : Utilisation de bases de données en mémoire pour des tests ultra-rapides et isolés.

---

## 🛠️ Technologies & Outils

| Domaine | Technologie |
| :--- | :--- |
| **Langages** | Kotlin, PHP 8.2+ |
| **Frameworks** | Jetpack Compose, Laravel 11 |
| **Base de données** | SQLite / MySQL (Eloquent ORM) |
| **Cartographie** | osmdroid, OSRM, Leaflet (Web) |
| **Communication** | Retrofit, OkHttp, Pusher |
| **DevOps** | Ngrok, Git (Seance-3 branch) |

---

## 📖 Termes Techniques à Savoir

*   **Endpoint** : Une adresse URL spécifique pointant vers une fonction du serveur.
*   **Payload** : Le corps des données envoyées (ex: coordonnées GPS).
*   **StateFlow** : Un flux de données "observable" qui met à jour l'écran Android dès qu'une valeur change.
*   **ORM (Eloquent)** : Permet de parler à la base de données comme si c'était des objets simples en code.
*   **Webhook** : Un signal envoyé automatiquement d'un serveur à un autre.
