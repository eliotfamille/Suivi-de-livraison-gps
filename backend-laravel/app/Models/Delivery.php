<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class Delivery extends Model
{
    use HasFactory;

    protected $fillable = [
        'order_id', 'driver_id', 'status', 'failure_reason',
        'proof_photo', 'signature', 'assigned_at', 'picked_up_at',
        'delivered_at', 'estimated_arrival', 'rating'
    ];

    protected $casts = [
        'assigned_at'       => 'datetime',
        'picked_up_at'      => 'datetime',
        'delivered_at'      => 'datetime',
        'estimated_arrival' => 'datetime',
    ];

    // Mapping statut → label FR
    public const STATUS_LABELS = [
        'pending'    => 'En attente',
        'assigned'   => 'Livreur assigné',
        'picked_up'  => 'Colis récupéré',
        'in_transit' => 'En cours de livraison',
        'delivered'  => 'Livré',
        'failed'     => 'Échec de livraison',
        'returned'   => 'Retourné',
    ];

    public function order(): \Illuminate\Database\Eloquent\Relations\BelongsTo
    {
        return $this->belongsTo(Order::class);
    }

    public function driver(): \Illuminate\Database\Eloquent\Relations\BelongsTo
    {
        return $this->belongsTo(Driver::class);
    }

    public function statuses(): \Illuminate\Database\Eloquent\Relations\HasMany
    {
        return $this->hasMany(Status::class)->orderBy('occurred_at');
    }

    public function locations(): \Illuminate\Database\Eloquent\Relations\HasMany
    {
        return $this->hasMany(Location::class)->orderBy('recorded_at');
    }

    public function lastLocation(): \Illuminate\Database\Eloquent\Relations\HasOne
    {
        return $this->hasOne(Location::class)->latestOfMany('recorded_at');
    }

    // Change le statut et enregistre dans l'historique
    public function changeStatus(string $status, ?string $note = null, ?float $lat = null, ?float $lng = null): Status
    {
        $this->update([
            'status'        => $status,
            'picked_up_at'  => $status === 'picked_up' ? now() : $this->picked_up_at,
            'delivered_at'  => $status === 'delivered' ? now() : $this->delivered_at,
        ]);

        $statusRecord = $this->statuses()->create([
            'status'      => $status,
            'label'       => self::STATUS_LABELS[$status] ?? $status,
            'note'        => $note,
            'lat'         => $lat,
            'lng'         => $lng,
            'occurred_at' => now(),
        ]);

        broadcast(new \App\Events\DeliveryStatusUpdated($this))->toOthers();

        return $statusRecord;
    }

    public function getStatusLabelAttribute(): string
    {
        return self::STATUS_LABELS[$this->status] ?? $this->status;
    }

    /**
     * Calculer l'ETA basé sur la distance entre le livreur et le destinataire.
     */
    public function calculateETA(): ?int
    {
        if (!$this->driver || !$this->driver->current_lat || !$this->order->recipient_lat) {
            return null;
        }

        $distance = $this->haversineDistance(
            $this->driver->current_lat,
            $this->driver->current_lng,
            $this->order->recipient_lat,
            $this->order->recipient_lng
        );

        // Vitesse moyenne estimée : 20 km/h en ville
        $speedKmH = 20;
        $timeHours = $distance / $speedKmH;
        $timeMinutes = round($timeHours * 60);

        // Ajouter un tampon de 5 minutes
        $eta = $timeMinutes + 5;

        $this->update(['estimated_arrival' => now()->addMinutes($eta)]);

        return $eta;
    }

    private function haversineDistance($lat1, $lon1, $lat2, $lon2)
    {
        $earthRadius = 6371; // km
        $dLat = deg2rad($lat2 - $lat1);
        $dLon = deg2rad($lon2 - $lon1);
        $a = sin($dLat / 2) * sin($dLat / 2) +
             cos(deg2rad($lat1)) * cos(deg2rad($lat2)) *
             sin($dLon / 2) * sin($dLon / 2);
        $c = 2 * atan2(sqrt($a), sqrt(1 - $a));
        return $earthRadius * $c;
    }
}
