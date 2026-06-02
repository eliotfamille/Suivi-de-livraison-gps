<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        // BLOC REPOUSSÉ DANS LA MIGRATION USERS -> SUPPRIMÉ D'ICI

        Schema::create('drivers', function (Blueprint $table) {
            $table->id();
            $table->foreignId('user_id')->unique()->constrained('users')->cascadeOnDelete();
            $table->string('license_number')->unique()->nullable();
            $table->string('vehicle_type')->nullable(); // moto, voiture, camion
            $table->string('vehicle_plate')->nullable();
            $table->string('vehicle_model')->nullable();
            $table->enum('status', ['available', 'busy', 'offline'])->default('offline');
            $table->decimal('current_lat', 10, 7)->nullable();
            $table->decimal('current_lng', 10, 7)->nullable();
            $table->timestamp('last_location_at')->nullable();
            $table->decimal('rating', 3, 2)->default(5.00);
            $table->integer('total_deliveries')->default(0);
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('drivers');
    }
};
