<?php

namespace App\Http\Controllers;

use App\Models\Delivery;
use App\Models\Order;
use App\Models\Package;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;

class DeliveryController extends Controller
{
    /**
     * GET /api/deliveries — Liste des livraisons selon le rôle
     */
    public function index(Request $request): JsonResponse
    {
        $user = $request->user();

        $query = Delivery::with(['order.package', 'driver.user', 'statuses']);

        // Si l'utilisateur est un client, il ne voit que ses commandes
        if ($user->hasRole('client')) {
            $query->whereHas('order', function($q) use ($user) {
                $q->where('client_id', $user->id);
            });
        }
        // Si c'est un livreur, il voit les missions disponibles (en attente) OU celles qui lui sont assignées
        elseif ($user->hasRole('driver')) {
            $query->where(function($q) use ($user) {
                $q->where('status', 'pending')
                  ->orWhere('driver_id', $user->driver->id ?? null);
            });
        }

        $deliveries = $query->latest()->get();

        return response()->json($deliveries->map(fn($d) => $this->deliveryResource($d)));
    }

    /**
     * POST /api/deliveries — Création d'une livraison
     */
    public function store(Request $request): JsonResponse
    {
        // LOG ÉTAPE 2 : Réception par Laravel
        \Log::info("Laravel a reçu une demande de création : ", $request->all());

        $request->validate([
            'description'       => 'required|string',
            'recipient_name'    => 'required|string',
            'recipient_phone'   => 'required|string',
            'recipient_address' => 'required|string',
            'sender_name'       => 'required|string',
            'sender_phone'      => 'required|string',
            'sender_address'    => 'required|string',
        ]);

        $delivery = DB::transaction(function () use ($request) {
            // LOG ÉTAPE 3 : Début enregistrement BDD
            \Log::info("Enregistrement en base de données pour l'utilisateur ID: " . $request->user()->id);

            // GESTION ZONES ET TARIFICATION
            $zones = [
                'Zone Urbaine'   => 5.00,
                'Zone Suburbaine' => 10.00,
                'Zone Rurale'    => 20.00
            ];

            // Logique de tarification simple basée sur le texte de l'adresse ou la zone envoyée
            $zone = $request->zone ?? 'Zone Urbaine';
            $fee  = $zones[$zone] ?? 5.00;

            $package = Package::create([
                'description' => $request->description,
                'weight_kg'   => $request->weight_kg ?? 0,
                'created_by'  => $request->user()->id,
            ]);

            $order = Order::create([
                'client_id'         => $request->user()->id,
                'package_id'        => $package->id,
                'sender_name'       => $request->sender_name,
                'sender_phone'      => $request->sender_phone,
                'sender_address'    => $request->sender_address,
                'sender_lat'        => $request->sender_lat,
                'sender_lng'        => $request->sender_lng,
                'recipient_name'    => $request->recipient_name,
                'recipient_phone'   => $request->recipient_phone,
                'recipient_address' => $request->recipient_address,
                'zone'              => $zone,
                'recipient_lat'     => $request->recipient_lat,
                'recipient_lng'     => $request->recipient_lng,
                'delivery_fee'      => $fee,
            ]);

            $delivery = Delivery::create([
                'order_id' => $order->id,
                'status'   => 'pending',
            ]);

            $delivery->statuses()->create([
                'status' => 'pending',
                'label'  => 'Commande créée',
                'note'   => 'En attente de prise en charge.',
                'lat'    => $request->sender_lat,
                'lng'    => $request->sender_lng,
            ]);

            broadcast(new \App\Events\DeliveryStatusUpdated($delivery))->toOthers();

            return $delivery;
        });

        // LOG ÉTAPE 4 : Succès et envoi de la réponse
        \Log::info("Livraison créée avec ID : " . $delivery->id);

        return response()->json([
            'message'  => "TEST REUSSI : Livraison #{$delivery->id} enregistrée en BDD !",
            'delivery' => $this->deliveryResource($delivery->load(['order.package'])),
        ], 201);
    }

    public function show(Delivery $delivery): JsonResponse
    {
        return response()->json($this->deliveryResource($delivery->load(['order.package', 'statuses', 'driver.user'])));
    }

    public function tracking($identifier): JsonResponse
    {
        $delivery = Delivery::whereHas('order', function($q) use ($identifier) {
            $q->where('order_number', $identifier);
        })->with(['order.package', 'statuses', 'driver.user'])->firstOrFail();

        return response()->json($this->deliveryResource($delivery));
    }

    public function accept(Request $request, Delivery $delivery): JsonResponse
    {
        if ($delivery->driver_id) {
            return response()->json(['message' => 'Cette mission est déjà assignée.'], 422);
        }

        $driver = $request->user()->driver;
        if (!$driver) return response()->json(['message' => 'Seuls les livreurs peuvent accepter des missions.'], 403);

        $delivery->update([
            'driver_id'   => $driver->id,
            'status'      => 'assigned',
            'assigned_at' => now(),
        ]);

        $delivery->statuses()->create([
            'status' => 'assigned',
            'label'  => 'Livreur assigné',
            'note'   => 'La mission a été acceptée par ' . $request->user()->name,
        ]);

        broadcast(new \App\Events\DeliveryStatusUpdated($delivery))->toOthers();

        return response()->json($this->deliveryResource($delivery));
    }

    public function updateStatus(Request $request, Delivery $delivery): JsonResponse
    {
        $request->validate([
            'status'      => 'required|string',
            'proof_photo' => 'nullable|image|max:2048',
            'signature'   => 'nullable|string', // Base64 signature
            'latitude'    => 'nullable|numeric',
            'longitude'   => 'nullable|numeric',
            'note'        => 'nullable|string',
        ]);

        $data = ['status' => $request->status];

        if ($request->status === 'picked_up') {
            $data['picked_up_at'] = now();
        } elseif ($request->status === 'delivered') {
            $data['delivered_at'] = now();
        }

        if ($request->hasFile('proof_photo')) {
            $path = $request->file('proof_photo')->store('proofs', 'public');
            $data['proof_photo'] = $path;
        }

        if ($request->signature) {
            $data['signature'] = $request->signature;
        }

        $delivery->update($data);

        $delivery->statuses()->create([
            'status' => $request->status,
            'label'  => Delivery::STATUS_LABELS[$request->status] ?? ucfirst($request->status),
            'lat'    => $request->latitude,
            'lng'    => $request->longitude,
            'note'   => $request->note
        ]);

        broadcast(new \App\Events\DeliveryStatusUpdated($delivery))->toOthers();

        // Webhook notification (simulation)
        try {
            \Illuminate\Support\Facades\Http::post('https://webhook.site/external-transporter', [
                'delivery_id' => $delivery->id,
                'status' => $request->status,
                'timestamp' => now()
            ]);
        } catch (\Exception $e) {
            \Log::warning("Webhook failed: " . $e->getMessage());
        }

        // Push Notification (Simulation FCM)
        try {
            $clientToken = $delivery->order->client->fcm_token;
            if ($clientToken) {
                \Log::info("Push Notification envoyée à {$delivery->order->client->name} : Statut {$request->status}");
                // Appel API Firebase ici normalement
            }
        } catch (\Exception $e) {
            \Log::error("FCM failed: " . $e->getMessage());
        }

        return response()->json($this->deliveryResource($delivery));
    }

    /**
     * POST /api/deliveries/{id}/rate — Notation de la livraison
     */
    public function rate(Request $request, Delivery $delivery): JsonResponse
    {
        $request->validate(['rating' => 'required|integer|min:1|max:5']);

        if ($delivery->status !== 'delivered') {
            return response()->json(['message' => 'Vous ne pouvez noter qu\'une livraison terminée.'], 422);
        }

        if ($delivery->rating) {
            return response()->json(['message' => 'Cette livraison a déjà été notée.'], 422);
        }

        $delivery->update(['rating' => $request->rating]);

        // Mise à jour de la note moyenne du livreur
        if ($delivery->driver) {
            $driver = $delivery->driver;
            $newCount = $driver->rating_count + 1;
            $newRating = (($driver->rating * $driver->rating_count) + $request->rating) / $newCount;

            $driver->update([
                'rating' => $newRating,
                'rating_count' => $newCount
            ]);
        }

        return response()->json(['message' => 'Merci pour votre note !', 'new_rating' => $delivery->rating]);
    }

    /**
     * Rapport PDF de livraison
     */
    public function downloadReceipt(Delivery $delivery)
    {
        $delivery->load(['order.package', 'driver.user', 'statuses']);

        $data = [
            'delivery' => $delivery,
            'qrCode' => "https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=" . urlencode($delivery->order->order_number)
        ];

        $pdf = \App::make('dompdf.wrapper');

        // CSS inline pour un rendu propre sans fichier externe
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
            <p>Commande #' . ($delivery->order->order_number ?? $delivery->id) . '</p>
        </div>

        <div class="section">
            <table class="grid">
                <tr>
                    <td>
                        <div class="section-title">Expéditeur</div>
                        <div class="info-box">
                            <strong>' . $delivery->order->sender_name . '</strong><br/>
                            ' . $delivery->order->sender_address . '<br/>
                            Tél: ' . $delivery->order->sender_phone . '
                        </div>
                    </td>
                    <td>
                        <div class="section-title">Destinataire</div>
                        <div class="info-box">
                            <strong>' . $delivery->order->recipient_name . '</strong><br/>
                            ' . $delivery->order->recipient_address . '<br/>
                            Tél: ' . $delivery->order->recipient_phone . '
                        </div>
                    </td>
                </tr>
            </table>
        </div>

        <div class="section">
            <div class="section-title">Détails du Colis</div>
            <div class="info-box">
                Description: ' . ($delivery->order->package->description ?? 'N/A') . '<br/>
                Poids: ' . ($delivery->order->package->weight_kg ?? '0') . ' kg<br/>
                Statut Final: <span class="status">' . strtoupper($delivery->status_label) . '</span>
            </div>
        </div>

        <div class="section">
            <div class="section-title">Coordonnées GPS des points</div>
            <div class="info-box">
                <table style="width:100%; font-size: 11px;">
                    <tr>
                        <td><strong>Départ (Expéditeur) :</strong><br/> Lat: ' . ($delivery->order->sender_lat ?? 'N/A') . ' / Lng: ' . ($delivery->order->sender_lng ?? 'N/A') . '</td>
                        <td><strong>Arrivée (Destinataire) :</strong><br/> Lat: ' . ($delivery->order->recipient_lat ?? 'N/A') . ' / Lng: ' . ($delivery->order->recipient_lng ?? 'N/A') . '</td>
                    </tr>
                </table>
            </div>
        </div>';

        if ($delivery->status === 'delivered') {
            $html .= '
            <div class="section">
                <table class="grid">
                    <tr>
                        <td>
                            <div class="section-title">Signature du destinataire</div>';
            if ($delivery->signature) {
                $html .= '<img src="' . $delivery->signature . '" class="signature-img">';
            } else {
                $html .= '<p>Non signée</p>';
            }

            if ($delivery->proof_photo) {
                $photoPath = storage_path('app/public/' . $delivery->proof_photo);
                if (file_exists($photoPath)) {
                    $html .= '
                    <div style="margin-top:20px;">
                        <div class="section-title">Photo de preuve</div>
                        <img src="data:image/jpeg;base64,' . base64_encode(file_get_contents($photoPath)) . '" class="proof-img">
                    </div>';
                }
            }

            $html .= '
                        </td>
                        <td>
                            <div class="section-title">Informations de livraison</div>
                            <p>Livré le: ' . ($delivery->delivered_at ? $delivery->delivered_at->format('d/m/Y H:i') : 'N/A') . '</p>
                            <p>Livreur: ' . ($delivery->driver?->user?->name ?? 'N/A') . '</p>
                        </td>
                    </tr>
                </table>
            </div>';
        }

        $html .= '
        <div class="footer">
            Document généré automatiquement par GPS Delivery Tracker - ' . now()->format('d/m/Y H:i') . '<br/>
            <img src="' . $data['qrCode'] . '" width="80" />
        </div>';

        $pdf->loadHTML($html);
        return $pdf->download("Bon_Livraison_{$delivery->order->order_number}.pdf");
    }

    private function deliveryResource($delivery): array
    {
        $proofPhotoUrl = null;
        if ($delivery->proof_photo) {
            $proofPhotoUrl = filter_var($delivery->proof_photo, FILTER_VALIDATE_URL)
                ? $delivery->proof_photo
                : url('storage/' . $delivery->proof_photo);
        }

        return [
            'id'                => $delivery->id,
            'status'            => $delivery->status,
            'proof_photo'       => $proofPhotoUrl,
            'signature'         => $delivery->signature, // Base64 usually
            'assigned_at'       => $delivery->assigned_at?->toIso8601String(),
            'picked_up_at'      => $delivery->picked_up_at?->toIso8601String(),
            'delivered_at'      => $delivery->delivered_at?->toIso8601String(),
            'estimated_arrival' => $delivery->estimated_arrival?->format('H:i'),
            'rating'            => $delivery->rating,
            'created_at'        => $delivery->created_at?->toIso8601String(),
            'order' => $delivery->order ? [
                'id'                => $delivery->order->id,
                'sender_name'       => $delivery->order->sender_name,
                'sender_address'    => $delivery->order->sender_address,
                'sender_lat'        => $delivery->order->sender_lat,
                'sender_lng'        => $delivery->order->sender_lng,
                'recipient_name'    => $delivery->order->recipient_name,
                'recipient_address' => $delivery->order->recipient_address,
                'recipient_lat'     => $delivery->order->recipient_lat,
                'recipient_lng'     => $delivery->order->recipient_lng,
                'package' => $delivery->order->package ? [
                    'description' => $delivery->order->package->description,
                    'weight_kg'   => $delivery->order->package->weight_kg,
                ] : null,
            ] : null,
            'statuses' => $delivery->statuses->map(fn($s) => [
                'status' => $s->status,
                'label'  => $s->label,
                'lat'    => $s->lat,
                'lng'    => $s->lng,
                'created_at' => $s->created_at?->toIso8601String() ?? $s->occurred_at?->toIso8601String(),
            ]),
            'driver' => $delivery->driver ? [
                'id'   => $delivery->driver->id,
                'user' => [
                    'name' => $delivery->driver->user->name ?? 'N/A',
                    'avatar' => $delivery->driver->user->avatar ? url('storage/' . $delivery->driver->user->avatar) : null,
                ],
                'status'            => $delivery->driver->status,
                'rating'            => (float) $delivery->driver->rating,
                'rating_count'      => $delivery->driver->rating_count,
                'total_deliveries'  => $delivery->driver->total_deliveries,
                'vehicle_model'     => $delivery->driver->vehicle_model,
                'vehicle_type'      => $delivery->driver->vehicle_type,
                'vehicle_plate'     => $delivery->driver->vehicle_plate,
                'current_lat'       => $delivery->driver->current_lat,
                'current_lng'       => $delivery->driver->current_lng,
                'joined_at'         => $delivery->driver->created_at->toIso8601String(),
            ] : null,
        ];
    }
}
