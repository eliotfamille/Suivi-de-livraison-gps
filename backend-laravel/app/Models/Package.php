<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\SoftDeletes;

class Package extends Model
{
    use HasFactory, SoftDeletes;

    protected $fillable = [
        'tracking_code', 'description', 'weight_kg', 'dimensions',
        'fragile', 'declared_value', 'notes', 'created_by',
    ];

    protected $casts = [
        'weight_kg'      => 'decimal:2',
        'declared_value' => 'decimal:2',
    ];

    protected static function boot(): void
    {
        parent::boot();
        static::creating(function ($package) {
            if (empty($package->tracking_code)) {
                $package->tracking_code = 'PKG-' . strtoupper(uniqid());
            }
        });
    }

    public function order(): \Illuminate\Database\Eloquent\Relations\HasOne
    {
        return $this->hasOne(Order::class);
    }

    public function creator(): \Illuminate\Database\Eloquent\Relations\BelongsTo
    {
        return $this->belongsTo(User::class, 'created_by');
    }
}
