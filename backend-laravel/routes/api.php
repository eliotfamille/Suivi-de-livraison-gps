<?php

use App\Http\Controllers\AdminController;
use App\Http\Controllers\AuthController;
use App\Http\Controllers\DeliveryController;
use App\Http\Controllers\DriverController;
use Illuminate\Support\Facades\Route;

Route::prefix('auth')->group(function () {
    Route::post('/register', [AuthController::class, 'register']);
    Route::post('/login',    [AuthController::class, 'login'])->name('login');
});

// Tracage
Route::get('/tracking/{identifier}', [DeliveryController::class, 'tracking']);

// Authentification
Route::middleware('auth:sanctum')->group(function () {

    // Auth
    Route::post('/auth/logout', [AuthController::class, 'logout']);
    Route::get('/auth/me',[AuthController::class, 'me']);


    // Livraison
    Route::prefix('deliveries')->group(function () {
        Route::get('/',                              [DeliveryController::class, 'index'])
            ->middleware('role:client|admin|driver');
        Route::post('/',                             [DeliveryController::class, 'store'])
            ->middleware('role:admin');
        Route::get('/{delivery}',                   [DeliveryController::class, 'show'])
            ->middleware('role:client|admin|driver');
        Route::patch('/{delivery}/status',          [DeliveryController::class, 'updateStatus'])
            ->middleware('role:driver|admin');
    });

    // Livreur
    Route::prefix('driver')->middleware('role:driver')->group(function () {
        Route::get('/deliveries',   [DriverController::class, 'myDeliveries']);
        Route::post('/location',    [DriverController::class, 'updateLocation']);
        Route::patch('/status',     [DriverController::class, 'updateStatus']);
    });

    // Admin
    Route::prefix('admin')->middleware('role:admin')->group(function () {
        Route::get('/dashboard',                        [AdminController::class, 'dashboard']);
        Route::get('/drivers',                          [AdminController::class, 'drivers']);
        Route::post('/deliveries/{delivery}/assign',    [AdminController::class, 'assignDriver']);
    });
});
