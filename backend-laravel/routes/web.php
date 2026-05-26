<?php

use App\Http\Controllers\AdminController;
use Illuminate\Support\Facades\Route;

Route::get('/', function () {
    return view('welcome');
});
// Cette route sera accessible directement sur http://localhost:8000/admin/dashboard
Route::get('/admin/dashboard', [AdminController::class, 'webDashboard'])->name('dashboard');