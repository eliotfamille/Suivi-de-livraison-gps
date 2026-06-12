<?php

use App\Http\Controllers\AdminController;
use App\Http\Controllers\AuthController;
use App\Http\Controllers\DeliveryController;
use App\Http\Controllers\DriverController;
use Illuminate\Support\Facades\Route;

Route::prefix('auth')->group(function () {
    Route::post('/register', [AuthController::class, 'register']);
    Route::post('/login',    [AuthController::class, 'login'])->name('login');
    Route::post('/forgot-password', [AuthController::class, 'forgotPassword']);
    Route::post('/reset-password', [AuthController::class, 'resetPassword']);
});

// Traçage public sécurisé
Route::get('/tracking/{identifier}', [DeliveryController::class, 'tracking']);

// Authentification requise
Route::middleware('auth:sanctum')->group(function () {

    // Auth
    Route::post('/auth/logout', [AuthController::class, 'logout']);
    Route::get('/auth/me', [AuthController::class, 'me']);
    Route::get('/users', [AuthController::class, 'search']);
    Route::match(['patch', 'post'], '/auth/profile', [AuthController::class, 'updateProfile']);

    // Livraisons
    Route::prefix('deliveries')->group(function () {
        Route::get('/',                              [DeliveryController::class, 'index'])->middleware('role:client|admin|driver');
        Route::post('/',                             [DeliveryController::class, 'store'])->middleware('role:admin|client');
        Route::get('/{delivery}',                   [DeliveryController::class, 'show'])->middleware('role:client|admin|driver');
        Route::patch('/{delivery}/status',          [DeliveryController::class, 'updateStatus'])->middleware('role:driver|admin');
        Route::post('/{delivery}/accept',           [DeliveryController::class, 'accept'])->middleware('role:driver');
        Route::post('/{delivery}/rate',             [DeliveryController::class, 'rate'])->middleware('role:client');
        Route::get('/{delivery}/receipt',            [DeliveryController::class, 'downloadReceipt']);
    });

    // Espace Livreur
    Route::prefix('driver')->middleware('role:driver')->group(function () {
        Route::get('/deliveries',   [DriverController::class, 'myDeliveries']);
        Route::match(['post', 'put'], '/location', [DriverController::class, 'updateLocationV2']);
        Route::patch('/status',     [DriverController::class, 'updateStatus']);
    });

    // Espace Admin
    Route::prefix('admin')->middleware('role:admin')->group(function () {
        Route::get('/dashboard',                        [AdminController::class, 'dashboard']);
        Route::get('/drivers',                          [AdminController::class, 'drivers']);
        Route::post('/deliveries/{delivery}/assign',    [AdminController::class, 'assignDriver']);
        Route::get('/performance/export',               [AdminController::class, 'exportPerformance']);
    });
});
