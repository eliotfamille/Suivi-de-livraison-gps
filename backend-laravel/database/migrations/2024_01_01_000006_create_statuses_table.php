<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('statuses', function (Blueprint $table) {
            $table->id();
            $table->foreignId('delivery_id')->constrained('deliveries')->cascadeOnDelete();
            $table->string('status');
            $table->string('label'); // message lisible
            $table->text('note')->nullable();
            $table->decimal('lat', 10, 7)->nullable();
            $table->decimal('lng', 10, 7)->nullable();
            $table->foreignId('updated_by')->nullable()->constrained('users');
            $table->timestamp('occurred_at')->useCurrent();
            $table->timestamps();

            $table->index(['delivery_id', 'occurred_at']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('statuses');
    }
};
