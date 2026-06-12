<?php

namespace App\Http\Controllers;

use App\Http\Controllers\Controller;
use App\Models\Delivery;
use App\Models\Driver;
use App\Models\Order;
use App\Models\User;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Contracts\View\View;

class AdminController extends Controller
{
    /**
     * GET /api/admin/dashboard — Stats générales
     */
    public function webDashboard(): View
    {
        // 1. Stats globales
        $stats = [
            'total_deliveries' => Delivery::count(),
            'today_count'      => Delivery::whereDate('created_at', today())->count(),
            'delivered'        => Delivery::where('status', 'delivered')->count(),
            'in_progress'      => Delivery::whereIn('status', ['assigned', 'picked_up', 'in_transit'])->count(),
            'revenue'          => Order::sum('delivery_fee'),
        ];

        // 2. Toutes les livraisons avec leurs détails (on limite à 50 pour la démo)
        $allDeliveries = Delivery::with(['order.client', 'driver.user', 'statuses', 'locations'])
            ->latest()
            ->take(50)
            ->get();

        // 3. Tous les clients
        $clients = User::role('client')->withCount('orders')->get();

        // 4. Liste complète des livreurs
        $drivers = Driver::with(['user', 'deliveries'])->get();

        // 5. LIVREURS POUR LA CARTE (disponibles et en mission)
        $activeDrivers = Driver::with('user')
            ->whereIn('status', ['available', 'busy'])
            ->whereNotNull('current_lat')
            ->get();

        // 6. Stats livreurs
        $driverStats = [
            'total'     => Driver::count(),
            'available' => Driver::where('status', 'available')->count(),
            'busy'      => Driver::where('status', 'busy')->count(),
            'offline'   => Driver::where('status', 'offline')->count(),
        ];

        return view('dashboard', compact('stats', 'allDeliveries', 'drivers', 'activeDrivers', 'driverStats', 'clients'));
    }
    public function dashboard(Request $request): JsonResponse
    {
        $now = now();
        $startOfMonth = $now->copy()->startOfMonth();

        $stats = [
            'deliveries' => [
                'total'      => Delivery::count(),
                'pending'    => Delivery::where('status', 'pending')->count(),
                'in_transit' => Delivery::where('status', 'in_transit')->count(),
                'delivered'  => Delivery::where('status', 'delivered')->count(),
                'failed'     => Delivery::where('status', 'failed')->count(),
                'today'      => Delivery::whereDate('created_at', today())->count(),
                'this_week'  => Delivery::whereBetween('created_at', [now()->startOfWeek(), now()])->count(),
                'avg_delivery_time_minutes' => Delivery::where('status', 'delivered')
                    ->whereNotNull('assigned_at')
                    ->get()
                    ->avg(fn($d) => now()->parse($d->assigned_at)->diffInMinutes($d->updated_at)),
            ],
            'drivers' => [
                'total'     => Driver::count(),
                'available' => Driver::where('status', 'available')->count(),
                'busy'      => Driver::where('status', 'busy')->count(),
                'offline'   => Driver::where('status', 'offline')->count(),
                'performance' => Driver::withCount(['deliveries' => fn($q) => $q->where('status', 'delivered')])
                    ->orderByDesc('deliveries_count')
                    ->take(5)
                    ->get()
                    ->map(fn($d) => [
                        'name' => $d->user->name,
                        'count' => $d->deliveries_count,
                        'rating' => $d->rating
                    ]),
            ],
            'zones' => [
                'most_active' => Order::select('recipient_address', \DB::raw('count(*) as total'))
                    ->groupBy('recipient_address')
                    ->orderByDesc('total')
                    ->take(5)
                    ->get()
                    ->map(fn($o) => [
                        'label' => str_contains($o->recipient_address, '(') ? explode('(', $o->recipient_address)[1] : 'Inconnue',
                        'total' => $o->total
                    ]),
            ],
            'revenue' => [
                'total'      => Order::sum('delivery_fee'),
                'this_month' => Order::whereMonth('created_at', $now->month)->sum('delivery_fee'),
                'today'      => Order::whereDate('created_at', today())->sum('delivery_fee'),
            ],
        ];

        // Livraisons récentes
        $recentDeliveries = Delivery::with(['order.client', 'driver.user'])
            ->latest()
            ->take(10)
            ->get()
            ->map(fn($d) => [
                'id'           => $d->id,
                'order_number' => $d->order->order_number,
                'status'       => $d->status,
                'status_label' => $d->status_label,
                'client'       => $d->order->client->name,
                'driver'       => $d->driver?->user?->name ?? 'Non assigné',
                'created_at'   => $d->created_at,
            ]);

        // Livreurs actifs avec leur position
        $activeDrivers = Driver::with('user')
            ->where('status', 'busy')
            ->whereNotNull('current_lat')
            ->get()
            ->map(fn($d) => [
                'id'            => $d->id,
                'name'          => $d->user->name,
                'vehicle_type'  => $d->vehicle_type,
                'vehicle_plate' => $d->vehicle_plate,
                'lat'           => $d->current_lat,
                'lng'           => $d->current_lng,
                'last_seen'     => $d->last_location_at,
                'rating'        => $d->rating,
            ]);


        return response()->json([
            'stats'             => $stats,
            'recent_deliveries' => $recentDeliveries,
            'active_drivers'    => $activeDrivers,
        ]);
}
    /**
     * GET /api/admin/drivers — Liste livreurs
     */
    public function drivers(Request $request): JsonResponse
    {
        $drivers = Driver::with('user')
            ->when($request->status, fn($q, $s) => $q->where('status', $s))
            ->paginate(20);

        return response()->json([
            'data' => $drivers->map(fn($d) => [
                'id'                => $d->id,
                'name'              => $d->user->name,
                'email'             => $d->user->email,
                'phone'             => $d->user->phone,
                'status'            => $d->status,
                'vehicle_type'      => $d->vehicle_type,
                'vehicle_plate'     => $d->vehicle_plate,
                'vehicle_model'     => $d->vehicle_model,
                'rating'            => $d->rating,
                'total_deliveries'  => $d->total_deliveries,
                'current_lat'       => $d->current_lat,
                'current_lng'       => $d->current_lng,
                'last_location_at'  => $d->last_location_at,
            ]),
            'meta' => [
                'total'        => $drivers->total(),
                'current_page' => $drivers->currentPage(),
                'last_page'    => $drivers->lastPage(),
            ],
        ]);
    }

    /**
     * POST /api/admin/deliveries/{id}/assign — Assigner un livreur
     */
    public function assignDriver(Request $request, Delivery $delivery): JsonResponse
    {
        $data = $request->validate([
            'driver_id' => 'required|exists:drivers,id',
        ]);

        $driver = Driver::findOrFail($data['driver_id']);

        if ($driver->status === 'offline') {
            return response()->json(['message' => 'Ce livreur est hors ligne'], 422);
        }

        $delivery->update([
            'driver_id'   => $driver->id,
            'status'      => 'assigned',
            'assigned_at' => now(),
        ]);

        $delivery->changeStatus('assigned', "Assigné à {$driver->user->name}");
        $driver->update(['status' => 'busy']);

        return response()->json([
            'message' => "Livraison assignée à {$driver->user->name}",
        ]);
    }

    /**
     * Export Excel (CSV pour éviter de créer une classe Export)
     */
    public function exportPerformance()
    {
        $drivers = Driver::with('user')->withCount(['deliveries' => fn($q) => $q->where('status', 'delivered')])->get();

        $callback = function() use ($drivers) {
            $file = fopen('php://output', 'w');
            fputcsv($file, ['ID', 'Nom', 'Livraisons Realisees', 'Note Moyenne', 'Email']);

            foreach ($drivers as $driver) {
                fputcsv($file, [
                    $driver->id,
                    $driver->user->name,
                    $driver->deliveries_count,
                    $driver->rating,
                    $driver->user->email
                ]);
            }
            fclose($file);
        };

        return response()->stream($callback, 200, [
            "Content-type"        => "text/csv",
            "Content-Disposition" => "attachment; filename=performance_livreurs_" . date('Y-m-d') . ".csv",
            "Pragma"              => "no-cache",
            "Cache-Control"       => "must-revalidate, post-check=0, pre-check=0",
            "Expires"             => "0"
        ]);
    }
}
