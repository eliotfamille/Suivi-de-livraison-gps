<?php

namespace App\Events;

use App\Models\Driver;
use Illuminate\Broadcasting\Channel;
use Illuminate\Broadcasting\InteractsWithSockets;
use Illuminate\Contracts\Broadcasting\ShouldBroadcast;
use Illuminate\Foundation\Events\Dispatchable;
use Illuminate\Queue\SerializesModels;

class DriverLocationUpdated implements ShouldBroadcast
{
    use Dispatchable, InteractsWithSockets, SerializesModels;

    public $driver;
    public $lat;
    public $lng;

    public function __construct(Driver $driver, $lat, $lng)
    {
        $this->driver = $driver;
        $this->lat = $lat;
        $this->lng = $lng;
    }

    public function broadcastOn(): array
    {
        return [
            new Channel('driver-locations'),
            new Channel('delivery.' . ($this->driver->activeDelivery?->id ?? 'none')),
        ];
    }

    public function broadcastWith(): array
    {
        return [
            'driver_id' => $this->driver->id,
            'lat'       => $this->lat,
            'lng'       => $this->lng,
            'updated_at'=> now()->toDateTimeString(),
        ];
    }
}
