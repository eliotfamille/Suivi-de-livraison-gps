<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\SoftDeletes;

class Order extends Model
{
    use HasFactory, SoftDeletes;

    protected $fillable = [
        'order_number', 'client_id', 'package_id',
        'sender_name', 'sender_phone', 'sender_address', 'sender_lat', 'sender_lng',
        'recipient_name', 'recipient_phone', 'recipient_address', 'recipient_lat', 'recipient_lng',
        'priority', 'delivery_fee', 'scheduled_at',
    ];

    protected $casts = [
        'sender_lat'   => 'decimal:7',
        'sender_lng'   => 'decimal:7',
        'recipient_lat' => 'decimal:7',
        'recipient_lng' => 'decimal:7',
        'delivery_fee' => 'decimal:2',
        'scheduled_at' => 'datetime',
    ];

    protected static function boot(): void
    {
        parent::boot();
        static::creating(function ($order) {
            if (empty($order->order_number)) {
                $order->order_number = 'ORD-' . date('Ymd') . '-' . strtoupper(substr(uniqid(), -5));
            }
        });
    }

    public function client(): \Illuminate\Database\Eloquent\Relations\BelongsTo
    {
        return $this->belongsTo(User::class, 'client_id');
    }

    public function package(): \Illuminate\Database\Eloquent\Relations\BelongsTo
    {
        return $this->belongsTo(Package::class);
    }

    public function delivery(): \Illuminate\Database\Eloquent\Relations\HasOne
    {
        return $this->hasOne(Delivery::class);
    }
}
