<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Location extends Model
{
    public $timestamps = false;

    protected $fillable = [
        'driver_id', 'delivery_id', 'lat', 'lng',
        'accuracy', 'speed', 'heading', 'altitude', 'recorded_at',
    ];

    protected $casts = [
        'lat'         => 'decimal:7',
        'lng'         => 'decimal:7',
        'accuracy'    => 'decimal:2',
        'speed'       => 'decimal:2',
        'heading'     => 'decimal:2',
        'altitude'    => 'decimal:2',
        'recorded_at' => 'datetime',
    ];

    public function driver(): \Illuminate\Database\Eloquent\Relations\BelongsTo
    {
        return $this->belongsTo(Driver::class);
    }

    public function delivery(): \Illuminate\Database\Eloquent\Relations\BelongsTo
    {
        return $this->belongsTo(Delivery::class);
    }
}
