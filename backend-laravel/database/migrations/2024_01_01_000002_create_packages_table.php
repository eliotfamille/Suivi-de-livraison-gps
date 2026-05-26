<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('packages', function (Blueprint $table) {
            $table->id();
            $table->string('tracking_code')->unique();
            $table->string('description');
            $table->decimal('weight_kg', 8, 2)->nullable();
            $table->string('dimensions')->nullable(); // "30x20x15 cm"
            $table->enum('fragile', ['yes', 'no'])->default('no');
            $table->decimal('declared_value', 10, 2)->nullable();
            $table->text('notes')->nullable();
            $table->foreignId('created_by')->constrained('users');
            $table->timestamps();
            $table->softDeletes();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('packages');
    }
};
