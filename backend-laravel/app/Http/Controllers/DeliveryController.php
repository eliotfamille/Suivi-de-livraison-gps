<?php

namespace App\Http\Controllers;

use App\Http\Controllers\Controller;
use App\Models\Delivery;
use App\Models\Driver;
use App\Models\Order;
use App\Models\Package;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class DeliveryController extends Controller
{
    /**
     * GET /api/deliveries
     * Admin → toutes | Client → les siennes
     */
    public function index(Request $request): JsonResponse
    {
        $user = $request->user();

        $query = Delivery::with([
            'order.client',
            'order.package',
            'driver.user',
            'lastLocation',
        ])->latest();

        // Filtre par rôle
        if ($user->isClient()) {
            $query->whereHas('order', fn($q) => $q->where('client_id', $user->id));
        }

        // Filtres optionnels
        if ($request->status) {
            $query->where('status', $request->status);
        }
        if ($request->driver_id) {
            $query->where('driver_id', $request->driver_id);
        }

        $deliveries = $query->paginate($request->per_page ?? 15);

        return response()->json([
            'data'  => $deliveries->map(fn($d) => $this->deliveryResource($d)),
            'meta'  => [
                'total'        => $deliveries->total(),
                'current_page' => $deliveries->currentPage(),
                'last_page'    => $deliveries->lastPage(),
            ],
        ]);
    }

    /**
     * POST /api/deliveries — Admin crée une livraison
     */
    public function store(Request $request): JsonResponse
    {
        $data = $request->validate([
            'sender_name'       => 'required|string',
            'sender_phone'      => 'required|string',
            'sender_address'    => 'required|string',
            'sender_lat'        => 'nullable|numeric',
            'sender_lng'        => 'nullable|numeric',
            'recipient_name'    => 'required|string',
            'recipient_phone'   => 'required|string',
            'recipient_address' => 'required|string',
            'recipient_lat'     => 'nullable|numeric',
            'recipient_lng'     => 'nullable|numeric',
            'description'       => 'required|string',
            'weight_kg'         => 'nullable|numeric',
            'fragile'           => 'nullable|in:yes,no',
            'priority'          => 'nullable|in:normal,express,urgent',
            'delivery_fee'      => 'nullable|numeric',
            'driver_id'         => 'nullable|exists:drivers,id',
            'client_id'         => 'nullable|exists:users,id',
        ]);

        // Créer le colis
        $package = Package::create([
            'description' => $data['description'],
            'weight_kg'   => $data['weight_kg'] ?? null,
            'fragile'     => $data['fragile'] ?? 'no',
            'created_by'  => $request->user()->id,
        ]);

        // Créer la commande
        $order = Order::create([
            'client_id'         => $data['client_id'] ?? $request->user()->id,
            'package_id'        => $package->id,
            'sender_name'       => $data['sender_name'],
            'sender_phone'      => $data['sender_phone'],
            'sender_address'    => $data['sender_address'],
            'sender_lat'        => $data['sender_lat'] ?? null,
            'sender_lng'        => $data['sender_lng'] ?? null,
            'recipient_name'    => $data['recipient_name'],
            'recipient_phone'   => $data['recipient_phone'],
            'recipient_address' => $data['recipient_address'],
            'recipient_lat'     => $data['recipient_lat'] ?? null,
            'recipient_lng'     => $data['recipient_lng'] ?? null,
            'priority'          => $data['priority'] ?? 'normal',
            'delivery_fee'      => $data['delivery_fee'] ?? 0,
        ]);

        // Créer la livraison
        $delivery = Delivery::create([
            'order_id'  => $order->id,
            'driver_id' => $data['driver_id'] ?? null,
            'status'    => $data['driver_id'] ? 'assigned' : 'pending',
        ]);

        // Enregistrer le statut initial
        $delivery->changeStatus($delivery->status);

        if ($data['driver_id'] ?? null) {
            $delivery->update(['assigned_at' => now()]);
        }

        $delivery->load(['order.package', 'driver.user']);

        return response()->json([
            'message'  => 'Livraison créée',
            'delivery' => $this->deliveryResource($delivery),
        ], 201);
    }

    /**
     * GET /api/deliveries/{id}
     */
    public function show(Request $request, Delivery $delivery): JsonResponse
    {
        $user = $request->user();

        // Client ne peut voir que ses livraisons
        if ($user->isClient() && $delivery->order->client_id !== $user->id) {
            return response()->json(['message' => 'Accès refusé'], 403);
        }

        $delivery->load([
            'order.client', 'order.package',
            'driver.user', 'statuses', 'lastLocation',
        ]);

        return response()->json([
            'delivery' => $this->deliveryResource($delivery, detailed: true),
        ]);
    }

    /**
     * PATCH /api/deliveries/{id}/status — Driver change statut
     */
    public function updateStatus(Request $request, Delivery $delivery): JsonResponse
    {
        $user   = $request->user();
        $driver = $user->driver;

        // Vérifier que c'est bien le livreur assigné ou un admin
        if ($user->isDriver() && $delivery->driver_id !== $driver?->id) {
            return response()->json(['message' => 'Ce n\'est pas votre livraison'], 403);
        }

        $data = $request->validate([
            'status'  => 'required|in:picked_up,in_transit,delivered,failed',
            'note'    => 'nullable|string',
            'lat'     => 'nullable|numeric',
            'lng'     => 'nullable|numeric',
            'failure_reason' => 'required_if:status,failed|nullable|string',
        ]);

        if ($data['status'] === 'failed') {
            $delivery->update(['failure_reason' => $data['failure_reason']]);
        }

        $status = $delivery->changeStatus(
            $data['status'],
            $data['note'] ?? null,
            $data['lat'] ?? null,
            $data['lng'] ?? null,
        );

        // Mettre à jour statut du livreur si livraison terminée
        if (in_array($data['status'], ['delivered', 'failed'])) {
            $driver?->update(['status' => 'available']);
            if ($data['status'] === 'delivered') {
                $driver?->increment('total_deliveries');
            }
        }

        return response()->json([
            'message' => 'Statut mis à jour',
            'status'  => [
                'id'          => $status->id,
                'status'      => $status->status,
                'label'       => $status->label,
                'occurred_at' => $status->occurred_at,
            ],
        ]);
    }

    /**
     * GET /api/tracking/{id} — Public tracking par ID ou tracking_code
     */
    public function tracking(string $identifier): JsonResponse
    {
        $delivery = Delivery::with([
            'order.package',
            'statuses',
            'lastLocation',
        ])
        ->whereHas('order', function ($q) use ($identifier) {
            $q->where('order_number', $identifier)
              ->orWhereHas('package', fn($p) => $p->where('tracking_code', $identifier));
        })
        ->orWhere('id', is_numeric($identifier) ? $identifier : 0)
        ->first();

        if (! $delivery) {
            return response()->json(['message' => 'Livraison introuvable'], 404);
        }

        return response()->json([
            'tracking' => [
                'order_number'      => $delivery->order->order_number,
                'tracking_code'     => $delivery->order->package->tracking_code,
                'status'            => $delivery->status,
                'status_label'      => $delivery->status_label,
                'recipient_name'    => $delivery->order->recipient_name,
                'recipient_address' => $delivery->order->recipient_address,
                'estimated_arrival' => $delivery->estimated_arrival,
                'delivered_at'      => $delivery->delivered_at,
                'current_position'  => $delivery->lastLocation ? [
                    'lat'         => $delivery->lastLocation->lat,
                    'lng'         => $delivery->lastLocation->lng,
                    'recorded_at' => $delivery->lastLocation->recorded_at,
                ] : null,
                'history' => $delivery->statuses->map(fn($s) => [
                    'status'      => $s->status,
                    'label'       => $s->label,
                    'note'        => $s->note,
                    'occurred_at' => $s->occurred_at,
                ]),
            ],
        ]);
    }

    private function deliveryResource(Delivery $delivery, bool $detailed = false): array
    {
        $base = [
            'id'                => $delivery->id,
            'status'            => $delivery->status,
            'status_label'      => $delivery->status_label,
            'assigned_at'       => $delivery->assigned_at,
            'picked_up_at'      => $delivery->picked_up_at,
            'delivered_at'      => $delivery->delivered_at,
            'estimated_arrival' => $delivery->estimated_arrival,
            'order'             => $delivery->order ? [
                'id'                => $delivery->order->id,
                'order_number'      => $delivery->order->order_number,
                'priority'          => $delivery->order->priority,
                'sender_address'    => $delivery->order->sender_address,
                'recipient_name'    => $delivery->order->recipient_name,
                'recipient_address' => $delivery->order->recipient_address,
                'recipient_lat'     => $delivery->order->recipient_lat,
                'recipient_lng'     => $delivery->order->recipient_lng,
                'package'           => $delivery->order->package ? [
                    'tracking_code' => $delivery->order->package->tracking_code,
                    'description'   => $delivery->order->package->description,
                    'weight_kg'     => $delivery->order->package->weight_kg,
                    'fragile'       => $delivery->order->package->fragile,
                ] : null,
            ] : null,
            'driver' => $delivery->driver ? [
                'id'            => $delivery->driver->id,
                'name'          => $delivery->driver->user->name,
                'phone'         => $delivery->driver->user->phone,
                'vehicle_type'  => $delivery->driver->vehicle_type,
                'vehicle_plate' => $delivery->driver->vehicle_plate,
                'current_lat'   => $delivery->driver->current_lat,
                'current_lng'   => $delivery->driver->current_lng,
            ] : null,
            'last_location' => $delivery->lastLocation ? [
                'lat'         => $delivery->lastLocation->lat,
                'lng'         => $delivery->lastLocation->lng,
                'speed'       => $delivery->lastLocation->speed,
                'recorded_at' => $delivery->lastLocation->recorded_at,
            ] : null,
        ];

        if ($detailed) {
            $base['history'] = $delivery->statuses->map(fn($s) => [
                'status'      => $s->status,
                'label'       => $s->label,
                'note'        => $s->note,
                'lat'         => $s->lat,
                'lng'         => $s->lng,
                'occurred_at' => $s->occurred_at,
            ]);
        }

        return $base;
    }
}
