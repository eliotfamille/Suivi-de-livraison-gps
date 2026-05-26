<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class Driver extends Model
{
    use HasFactory;

    protected $fillable = [
        'user_id', 'license_number', 'vehicle_type', 'vehicle_plate',
        'vehicle_model', 'status', 'current_lat', 'current_lng',
        'last_location_at', 'rating', 'total_deliveries',
    ];

    protected $casts = [
        'current_lat'      => 'decimal:7',
        'current_lng'      => 'decimal:7',
        'last_location_at' => 'datetime',
        'rating'           => 'decimal:2',
    ];

    public function user(): \Illuminate\Database\Eloquent\Relations\BelongsTo
    {
        return $this->belongsTo(User::class);
    }

    public function deliveries(): \Illuminate\Database\Eloquent\Relations\HasMany
    {
        return $this->hasMany(Delivery::class);
    }

    public function locations(): \Illuminate\Database\Eloquent\Relations\HasMany
    {
        return $this->hasMany(Location::class);
    }

    public function activeDelivery(): \Illuminate\Database\Eloquent\Relations\HasOne
    {
        return $this->hasOne(Delivery::class)
            ->whereIn('status', ['assigned', 'picked_up', 'in_transit']);
    }

    // Met à jour position GPS du livreur
    public function updateLocation(float $lat, float $lng, array $extra = []): Location
    {
        // Mettre à jour la position courante
        $this->update([
            'current_lat'      => $lat,
            'current_lng'      => $lng,
            'last_location_at' => now(),
            'status'           => 'busy',
        ]);

        // Enregistrer dans l'historique
        return $this->locations()->create(array_merge([
            'lat'         => $lat,
            'lng'         => $lng,
            'delivery_id' => $this->activeDelivery?->id,
            'recorded_at' => now(),
        ], $extra));
    }
}
