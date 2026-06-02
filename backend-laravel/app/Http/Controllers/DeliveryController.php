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
                'recipient_lat'     => $request->recipient_lat,
                'recipient_lng'     => $request->recipient_lng,
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

        return response()->json($this->deliveryResource($delivery));
    }

    public function updateStatus(Request $request, Delivery $delivery): JsonResponse
    {
        $request->validate(['status' => 'required|string']);

        $delivery->update(['status' => $request->status]);

        $delivery->statuses()->create([
            'status' => $request->status,
            'label'  => ucfirst($request->status),
            'lat'    => $request->latitude,
            'lng'    => $request->longitude,
        ]);

        return response()->json($this->deliveryResource($delivery));
    }

    private function deliveryResource($delivery): array
    {
        return [
            'id'                => $delivery->id,
            'status'            => $delivery->status,
            'assigned_at'       => $delivery->assigned_at,
            'created_at'        => $delivery->created_at,
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
                'created_at' => $s->created_at,
            ]),
            'driver' => $delivery->driver ? [
                'id'   => $delivery->driver->id,
                'user' => ['name' => $delivery->driver->user->name ?? 'N/A']
            ] : null,
        ];
    }
}
