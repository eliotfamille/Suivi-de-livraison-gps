<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('orders', function (Blueprint $table) {
            $table->id();
            $table->string('order_number')->unique();
            $table->foreignId('client_id')->constrained('users');

            // LIGNE CORRIGÉE : Liaison propre avec cascade
            $table->foreignId('package_id')->constrained('packages')->cascadeOnDelete();

            // Adresse expéditeur
            $table->string('sender_name');
            $table->string('sender_phone');
            $table->text('sender_address');
            $table->decimal('sender_lat', 10, 7)->nullable();
            $table->decimal('sender_lng', 10, 7)->nullable();

            // Adresse destinataire
            $table->string('recipient_name');
            $table->string('recipient_phone');
            $table->text('recipient_address');
            $table->decimal('recipient_lat', 10, 7)->nullable();
            $table->decimal('recipient_lng', 10, 7)->nullable();

            $table->enum('priority', ['normal', 'express', 'urgent'])->default('normal');
            $table->decimal('delivery_fee', 10, 2)->default(0);
            $table->timestamp('scheduled_at')->nullable();

            $table->timestamps();
            $table->softDeletes();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('orders');
    }
};
