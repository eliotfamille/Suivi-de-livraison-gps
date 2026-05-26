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
        'delivered_at', 'estimated_arrival',
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

        return $this->statuses()->create([
            'status'      => $status,
            'label'       => self::STATUS_LABELS[$status] ?? $status,
            'note'        => $note,
            'lat'         => $lat,
            'lng'         => $lng,
            'occurred_at' => now(),
        ]);
    }

    public function getStatusLabelAttribute(): string
    {
        return self::STATUS_LABELS[$this->status] ?? $this->status;
    }
}
