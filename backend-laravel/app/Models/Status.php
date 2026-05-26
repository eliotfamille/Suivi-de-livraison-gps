<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Status extends Model
{
    protected $fillable = [
        'delivery_id', 'status', 'label', 'note',
        'lat', 'lng', 'updated_by', 'occurred_at',
    ];

    protected $casts = [
        'lat'         => 'decimal:7',
        'lng'         => 'decimal:7',
        'occurred_at' => 'datetime',
    ];

    public function delivery(): \Illuminate\Database\Eloquent\Relations\BelongsTo
    {
        return $this->belongsTo(Delivery::class);
    }

    public function updatedBy(): \Illuminate\Database\Eloquent\Relations\BelongsTo
    {
        return $this->belongsTo(User::class, 'updated_by');
    }
}
