<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('locations', function (Blueprint $table) {
            $table->id();
            $table->foreignId('driver_id')->constrained('drivers')->cascadeOnDelete();
            $table->foreignId('delivery_id')->nullable()->constrained('deliveries')->nullOnDelete();
            $table->decimal('lat', 10, 7);
            $table->decimal('lng', 10, 7);
            $table->decimal('accuracy', 8, 2)->nullable(); // précision GPS en mètres
            $table->decimal('speed', 8, 2)->nullable(); // km/h
            $table->decimal('heading', 5, 2)->nullable(); // direction en degrés
            $table->decimal('altitude', 10, 2)->nullable();
            $table->timestamp('recorded_at')->useCurrent();

            $table->index(['driver_id', 'recorded_at']);
            $table->index(['delivery_id', 'recorded_at']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('locations');
    }
};
