<?php

namespace App\Http\Controllers;

use App\Models\Delivery;
use App\Models\Order;
use App\Models\Package;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;

class DeliveryController extends Controller
{
    /**
     * GET /api/deliveries — Liste des livraisons selon le rôle de l'utilisateur
     */
    public function index(Request $requete): JsonResponse
    {
        $utilisateur = $requete->user();

        $constructionRequete = Delivery::with(['order.package', 'driver.user', 'statuses']);

        // Si l'utilisateur est un client, il ne voit que ses commandes
        if ($utilisateur->hasRole('client')) {
            $constructionRequete->whereHas('order', function($q) use ($utilisateur) {
                $q->where('client_id', $utilisateur->id);
            });
        }
        // Si c'est un livreur, il voit les missions disponibles (en attente) OU celles qui lui sont déjà assignées
        elseif ($utilisateur->hasRole('driver')) {
            $constructionRequete->where(function($q) use ($utilisateur) {
                $q->where('status', 'pending')
                  ->orWhere('driver_id', $utilisateur->driver->id ?? null);
            });
        }

        $livraisons = $constructionRequete->latest()->get();

        return response()->json($livraisons->map(fn($l) => $this->ressourceLivraison($l)));
    }

    /**
     * POST /api/deliveries — Création d'une nouvelle livraison
     */
    public function store(Request $requete): JsonResponse
    {
        // LOG ÉTAPE 2 : Réception de la demande par le backend
        Log::info("Demande de création de livraison reçue : ", $requete->all());

        $requete->validate([
            'description'       => 'required|string',
            'recipient_name'    => 'required|string',
            'recipient_phone'   => 'required|string',
            'recipient_address' => 'required|string',
            'sender_name'       => 'required|string',
            'sender_phone'      => 'required|string',
            'sender_address'    => 'required|string',
        ]);

        $livraison = DB::transaction(function () use ($requete) {
            // LOG ÉTAPE 3 : Début du traitement en base de données
            Log::info("Enregistrement en BDD pour l'utilisateur ID : " . $requete->user()->id);

            // GESTION DES ZONES ET TARIFICATION
            $zonesTarifaires = [
                'Zone Urbaine'   => 5.00,
                'Zone Suburbaine' => 10.00,
                'Zone Rurale'    => 20.00
            ];

            // Logique de tarification basée sur la zone fournie ou par défaut
            $zone = $requete->zone ?? 'Zone Urbaine';
            $frais = $zonesTarifaires[$zone] ?? 5.00;

            $colis = Package::create([
                'description' => $requete->description,
                'weight_kg'   => $requete->weight_kg ?? 0,
                'created_by'  => $requete->user()->id,
            ]);

            $commande = Order::create([
                'client_id'         => $requete->user()->id,
                'package_id'        => $colis->id,
                'sender_name'       => $requete->sender_name,
                'sender_phone'      => $requete->sender_phone,
                'sender_address'    => $requete->sender_address,
                'sender_lat'        => $requete->sender_lat,
                'sender_lng'        => $requete->sender_lng,
                'recipient_name'    => $requete->recipient_name,
                'recipient_phone'   => $requete->recipient_phone,
                'recipient_address' => $requete->recipient_address,
                'zone'              => $zone,
                'recipient_lat'     => $requete->recipient_lat,
                'recipient_lng'     => $requete->recipient_lng,
                'delivery_fee'      => $frais,
            ]);

            $livraison = Delivery::create([
                'order_id' => $commande->id,
                'status'   => 'pending',
            ]);

            $livraison->statuses()->create([
                'status' => 'pending',
                'label'  => 'Commande créée',
                'note'   => 'En attente de prise en charge par un livreur.',
                'lat'    => $requete->sender_lat,
                'lng'    => $requete->sender_lng,
            ]);

            // Notification en temps réel
            broadcast(new \App\Events\DeliveryStatusUpdated($livraison))->toOthers();

            return $livraison;
        });

        // LOG ÉTAPE 4 : Confirmation du succès
        Log::info("Livraison créée avec succès. ID : " . $livraison->id);

        return response()->json([
            'message'  => "Succès : Livraison #{$livraison->id} enregistrée en base de données !",
            'delivery' => $this->ressourceLivraison($livraison->load(['order.package'])),
        ], 201);
    }

    /**
     * GET /api/deliveries/{id} — Détails d'une livraison spécifique
     */
    public function show(Delivery $livraison): JsonResponse
    {
        return response()->json($this->ressourceLivraison($livraison->load(['order.package', 'statuses', 'driver.user'])));
    }

    /**
     * GET /api/tracking/{identifier} — Suivi d'une livraison via son numéro de commande
     */
    public function tracking($identifiant): JsonResponse
    {
        $livraison = Delivery::whereHas('order', function($q) use ($identifiant) {
            $q->where('order_number', $identifiant);
        })->with(['order.package', 'statuses', 'driver.user'])->firstOrFail();

        return response()->json($this->ressourceLivraison($livraison));
    }

    /**
     * POST /api/deliveries/{id}/accept — Acceptation d'une mission par un livreur
     */
    public function accept(Request $requete, Delivery $livraison): JsonResponse
    {
        if ($livraison->driver_id) {
            return response()->json(['message' => 'Cette mission est déjà assignée à un autre livreur.'], 422);
        }

        $livreur = $requete->user()->driver;
        if (!$livreur) {
            return response()->json(['message' => 'Seuls les livreurs peuvent accepter des missions.'], 403);
        }

        $livraison->update([
            'driver_id'   => $livreur->id,
            'status'      => 'assigned',
            'assigned_at' => now(),
        ]);

        $livraison->statuses()->create([
            'status' => 'assigned',
            'label'  => 'Livreur assigné',
            'note'   => 'La mission a été acceptée par ' . $requete->user()->name,
        ]);

        broadcast(new \App\Events\DeliveryStatusUpdated($livraison))->toOthers();

        return response()->json($this->ressourceLivraison($livraison));
    }

    /**
     * PATCH /api/deliveries/{id}/status — Mise à jour du statut d'une livraison
     */
    public function updateStatus(Request $requete, Delivery $livraison): JsonResponse
    {
        $requete->validate([
            'status'      => 'required|string',
            'proof_photo' => 'nullable|image|max:2048',
            'signature'   => 'nullable|string', // Signature en Base64
            'latitude'    => 'nullable|numeric',
            'longitude'   => 'nullable|numeric',
            'note'        => 'nullable|string',
        ]);

        $donnees = ['status' => $requete->status];

        if ($requete->status === 'picked_up') {
            $donnees['picked_up_at'] = now();
        } elseif ($requete->status === 'delivered') {
            $donnees['delivered_at'] = now();
        }

        if ($requete->hasFile('proof_photo')) {
            $chemin = $requete->file('proof_photo')->store('proofs', 'public');
            $donnees['proof_photo'] = $chemin;
        }

        if ($requete->signature) {
            $donnees['signature'] = $requete->signature;
        }

        $livraison->update($donnees);

        $livraison->statuses()->create([
            'status' => $requete->status,
            'label'  => Delivery::STATUS_LABELS[$requete->status] ?? ucfirst($requete->status),
            'lat'    => $requete->latitude,
            'lng'    => $requete->longitude,
            'note'   => $requete->note
        ]);

        broadcast(new \App\Events\DeliveryStatusUpdated($livraison))->toOthers();

        // Notification Webhook (simulation)
        try {
            \Illuminate\Support\Facades\Http::post('https://webhook.site/external-transporter', [
                'delivery_id' => $livraison->id,
                'status' => $requete->status,
                'timestamp' => now()
            ]);
        } catch (\Exception $e) {
            Log::warning("Échec du Webhook : " . $e->getMessage());
        }

        // Notification Push (Simulation FCM)
        try {
            $jetonClient = $livraison->order->client->fcm_token;
            if ($jetonClient) {
                Log::info("Notification Push envoyée à {$livraison->order->client->name} : Nouveau statut {$requete->status}");
            }
        } catch (\Exception $e) {
            Log::error("Échec de la notification FCM : " . $e->getMessage());
        }

        return response()->json($this->ressourceLivraison($livraison));
    }

    /**
     * POST /api/deliveries/{id}/rate — Notation d'une livraison terminée
     */
    public function rate(Request $requete, Delivery $livraison): JsonResponse
    {
        $requete->validate(['rating' => 'required|integer|min:1|max:5']);

        if ($livraison->status !== 'delivered') {
            return response()->json(['message' => 'Vous ne pouvez noter qu\'une livraison terminée.'], 422);
        }

        if ($livraison->rating) {
            return response()->json(['message' => 'Cette livraison a déjà été notée.'], 422);
        }

        $livraison->update(['rating' => $requete->rating]);

        // Mise à jour de la note moyenne du livreur
        if ($livraison->driver) {
            $livreur = $livraison->driver;
            $nouveauCompte = $livreur->rating_count + 1;
            $nouvelleNote = (($livreur->rating * $livreur->rating_count) + $requete->rating) / $nouveauCompte;

            $livreur->update([
                'rating' => $nouvelleNote,
                'rating_count' => $nouveauCompte
            ]);
        }

        return response()->json(['message' => 'Merci pour votre note !', 'new_rating' => $livraison->rating]);
    }

    /**
     * GET /api/deliveries/{id}/receipt — Téléchargement du bon de livraison PDF
     */
    public function downloadReceipt(Delivery $livraison)
    {
        $livraison->load(['order.package', 'driver.user', 'statuses']);

        $donnees = [
            'delivery' => $livraison,
            'qrCode' => "https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=" . urlencode($livraison->order->order_number)
        ];

        $pdf = \App::make('dompdf.wrapper');

        $html = '
        <style>
            body { font-family: sans-serif; color: #333; }
            .header { text-align: center; border-bottom: 2px solid #00d4ff; padding-bottom: 10px; }
            .section { margin-top: 20px; }
            .section-title { font-weight: bold; text-transform: uppercase; color: #64748b; font-size: 12px; margin-bottom: 10px; }
            .grid { width: 100%; border-collapse: collapse; }
            .grid td { vertical-align: top; padding: 5px; width: 50%; }
            .info-box { background: #f8fafc; padding: 10px; border-radius: 5px; }
            .status { display: inline-block; padding: 5px 10px; background: #00d4ff; color: #000; font-weight: bold; border-radius: 15px; font-size: 10px; }
            .footer { margin-top: 50px; text-align: center; font-size: 10px; color: #94a3b8; }
            .signature-img { max-width: 200px; max-height: 100px; border: 1px solid #e2e8f0; }
            .proof-img { max-width: 300px; border-radius: 10px; }
        </style>
        <div class="header">
            <h1>BON DE LIVRAISON</h1>
            <p>Commande #' . ($livraison->order->order_number ?? $livraison->id) . '</p>
        </div>

        <div class="section">
            <table class="grid">
                <tr>
                    <td>
                        <div class="section-title">Expéditeur</div>
                        <div class="info-box">
                            <strong>' . $livraison->order->sender_name . '</strong><br/>
                            ' . $livraison->order->sender_address . '<br/>
                            Tél: ' . $livraison->order->sender_phone . '
                        </div>
                    </td>
                    <td>
                        <div class="section-title">Destinataire</div>
                        <div class="info-box">
                            <strong>' . $livraison->order->recipient_name . '</strong><br/>
                            ' . $livraison->order->recipient_address . '<br/>
                            Tél: ' . $livraison->order->recipient_phone . '
                        </div>
                    </td>
                </tr>
            </table>
        </div>

        <div class="section">
            <div class="section-title">Détails du Colis</div>
            <div class="info-box">
                Description: ' . ($livraison->order->package->description ?? 'N/A') . '<br/>
                Poids: ' . ($livraison->order->package->weight_kg ?? '0') . ' kg<br/>
                Statut Final: <span class="status">' . strtoupper($livraison->status_label) . '</span>
            </div>
        </div>

        <div class="section">
            <div class="section-title">Coordonnées GPS</div>
            <div class="info-box">
                <table style="width:100%; font-size: 11px;">
                    <tr>
                        <td><strong>Départ :</strong><br/> Lat: ' . ($livraison->order->sender_lat ?? 'N/A') . ' / Lng: ' . ($livraison->order->sender_lng ?? 'N/A') . '</td>
                        <td><strong>Arrivée :</strong><br/> Lat: ' . ($livraison->order->recipient_lat ?? 'N/A') . ' / Lng: ' . ($livraison->order->recipient_lng ?? 'N/A') . '</td>
                    </tr>
                </table>
            </div>
        </div>';

        if ($livraison->status === 'delivered') {
            $html .= '
            <div class="section">
                <table class="grid">
                    <tr>
                        <td>
                            <div class="section-title">Signature du destinataire</div>';
            if ($livraison->signature) {
                $html .= '<img src="' . $livraison->signature . '" class="signature-img">';
            } else {
                $html .= '<p>Non signée</p>';
            }

            if ($livraison->proof_photo) {
                $cheminPhoto = storage_path('app/public/' . $livraison->proof_photo);
                if (file_exists($cheminPhoto)) {
                    $html .= '
                    <div style="margin-top:20px;">
                        <div class="section-title">Photo de preuve</div>
                        <img src="data:image/jpeg;base64,' . base64_encode(file_get_contents($cheminPhoto)) . '" class="proof-img">
                    </div>';
                }
            }

            $html .= '
                        </td>
                        <td>
                            <div class="section-title">Informations de livraison</div>
                            <p>Livré le : ' . ($livraison->delivered_at ? $livraison->delivered_at->format('d/m/Y H:i') : 'N/A') . '</p>
                            <p>Livreur : ' . ($livraison->driver?->user?->name ?? 'N/A') . '</p>
                        </td>
                    </tr>
                </table>
            </div>';
        }

        $html .= '
        <div class="footer">
            Document généré automatiquement - ' . now()->format('d/m/Y H:i') . '<br/>
            <img src="' . $donnees['qrCode'] . '" width="80" />
        </div>';

        $pdf->loadHTML($html);
        return $pdf->download("Bon_Livraison_{$livraison->order->order_number}.pdf");
    }

    /**
     * Formate la ressource de livraison pour l'API
     */
    private function ressourceLivraison($livraison): array
    {
        $urlPhotoPreuve = null;
        if ($livraison->proof_photo) {
            $urlPhotoPreuve = filter_var($livraison->proof_photo, FILTER_VALIDATE_URL)
                ? $livraison->proof_photo
                : url('storage/' . $livraison->proof_photo);
        }

        return [
            'id'                => $livraison->id,
            'status'            => $livraison->status,
            'proof_photo'       => $urlPhotoPreuve,
            'signature'         => $livraison->signature,
            'assigned_at'       => $livraison->assigned_at?->toIso8601String(),
            'picked_up_at'      => $livraison->picked_up_at?->toIso8601String(),
            'delivered_at'      => $livraison->delivered_at?->toIso8601String(),
            'estimated_arrival' => $livraison->estimated_arrival?->format('H:i'),
            'rating'            => $livraison->rating,
            'created_at'        => $livraison->created_at?->toIso8601String(),
            'order' => $livraison->order ? [
                'id'                => $livraison->order->id,
                'sender_name'       => $livraison->order->sender_name,
                'sender_address'    => $livraison->order->sender_address,
                'sender_lat'        => $livraison->order->sender_lat,
                'sender_lng'        => $livraison->order->sender_lng,
                'recipient_name'    => $livraison->order->recipient_name,
                'recipient_address' => $livraison->order->recipient_address,
                'recipient_lat'     => $livraison->order->recipient_lat,
                'recipient_lng'     => $livraison->order->recipient_lng,
                'package' => $livraison->order->package ? [
                    'description' => $livraison->order->package->description,
                    'weight_kg'   => $livraison->order->package->weight_kg,
                ] : null,
            ] : null,
            'statuses' => $livraison->statuses->map(fn($s) => [
                'status' => $s->status,
                'label'  => $s->label,
                'lat'    => $s->lat,
                'lng'    => $s->lng,
                'created_at' => $s->created_at?->toIso8601String() ?? $s->occurred_at?->toIso8601String(),
            ]),
            'driver' => $livraison->driver ? [
                'id'   => $livraison->driver->id,
                'user' => [
                    'name' => $livraison->driver->user->name ?? 'N/A',
                    'avatar' => $livraison->driver->user->avatar ? url('storage/' . $livraison->driver->user->avatar) : null,
                ],
                'status'            => $livraison->driver->status,
                'rating'            => (float) $livraison->driver->rating,
                'rating_count'      => $livraison->driver->rating_count,
                'total_deliveries'  => $livraison->driver->total_deliveries,
                'vehicle_model'     => $livraison->driver->vehicle_model,
                'vehicle_type'      => $livraison->driver->vehicle_type,
                'vehicle_plate'     => $livraison->driver->vehicle_plate,
                'current_lat'       => $livraison->driver->current_lat,
                'current_lng'       => $livraison->driver->current_lng,
                'joined_at'         => $livraison->driver->created_at->toIso8601String(),
            ] : null,
        ];
    }
}
