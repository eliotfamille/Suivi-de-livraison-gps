<?php

namespace App\Http\Controllers;

use App\Http\Controllers\Controller;
use App\Models\Delivery;
use App\Events\DriverLocationUpdated;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class DriverController extends Controller
{
    /**
     * POST /api/driver/location — Envoyer position GPS (appelé par l'app Kotlin)
     */
    public function updateLocation(Request $request): JsonResponse
    {
        $data = $request->validate([
            'lat'         => 'required|numeric|between:-90,90',
            'lng'         => 'required|numeric|between:-180,180',
            'accuracy'    => 'nullable|numeric',
            'speed'       => 'nullable|numeric',
            'heading'     => 'nullable|numeric',
            'altitude'    => 'nullable|numeric',
        ]);

        $driver = $request->user()->driver;

        if (! $driver) {
            return response()->json(['message' => 'Profil livreur introuvable'], 404);
        }

        $location = $driver->updateLocation(
            $data['lat'],
            $data['lng'],
            array_filter([
                'accuracy' => $data['accuracy'] ?? null,
                'speed'    => $data['speed'] ?? null,
                'heading'  => $data['heading'] ?? null,
                'altitude' => $data['altitude'] ?? null,
            ])
        );

        return response()->json([
            'message'  => 'Position mise à jour',
            'location' => [
                'lat'         => $location->lat,
                'lng'         => $location->lng,
                'recorded_at' => $location->recorded_at,
            ],
        ]);
    }

    /**
     * PUT /api/drivers/location — Alternative endpoint requested
     */
    public function updateLocationV2(Request $request): JsonResponse
    {
        $data = $request->validate([
            'latitude'  => 'required|numeric',
            'longitude' => 'required|numeric',
        ]);

        $driver = $request->user()->driver;
        if (!$driver) return response()->json(['message' => 'Livreur non trouvé'], 404);

        $driver->updateLocation($data['latitude'], $data['longitude']);

        broadcast(new DriverLocationUpdated($driver, $data['latitude'], $data['longitude']))->toOthers();

        return response()->json(['message' => 'Position mise à jour']);
    }

    /**
     * GET /api/driver/deliveries — Livraisons du livreur connecté
     */
    public function myDeliveries(Request $request): JsonResponse
    {
        $driver = $request->user()->driver;

        if (! $driver) {
            return response()->json(['message' => 'Profil livreur introuvable'], 404);
        }

        $deliveries = Delivery::with(['order.package', 'lastLocation'])
            ->where('driver_id', $driver->id)
            ->when($request->status, fn($q, $s) => $q->where('status', $s))
            ->latest()
            ->paginate(10);

        return response()->json([
            'driver' => [
                'id'                 => $driver->id,
                'status'             => $driver->status,
                'vehicle_type'       => $driver->vehicle_type,
                'total_deliveries'   => $driver->total_deliveries,
                'rating'             => $driver->rating,
            ],
            'deliveries' => $deliveries->map(fn($d) => [
                'id'           => $d->id,
                'status'       => $d->status,
                'status_label' => $d->status_label,
                'order'        => [
                    'order_number'      => $d->order->order_number,
                    'recipient_name'    => $d->order->recipient_name,
                    'recipient_address' => $d->order->recipient_address,
                    'recipient_lat'     => $d->order->recipient_lat,
                    'recipient_lng'     => $d->order->recipient_lng,
                    'priority'          => $d->order->priority,
                    'package'           => [
                        'tracking_code' => $d->order->package->tracking_code,
                        'description'   => $d->order->package->description,
                        'weight_kg'     => $d->order->package->weight_kg,
                        'fragile'       => $d->order->package->fragile,
                    ],
                ],
                'assigned_at'  => $d->assigned_at,
            ]),
            'meta' => [
                'total'        => $deliveries->total(),
                'current_page' => $deliveries->currentPage(),
            ],
        ]);
    }

    /**
     * PATCH /api/driver/status — Le livreur change son statut (available/offline)
     */
    public function updateStatus(Request $request): JsonResponse
    {
        $request->validate([
            'status' => 'required|in:available,offline',
        ]);

        $driver = $request->user()->driver;

        if (! $driver) {
            return response()->json(['message' => 'Profil livreur introuvable'], 404);
        }

        $driver->update(['status' => $request->status]);

        return response()->json([
            'message' => 'Statut mis à jour',
            'status'  => $driver->status,
        ]);
    }
}
