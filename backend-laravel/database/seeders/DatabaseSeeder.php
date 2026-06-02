<?php

namespace Database\Seeders;

use App\Models\User;
use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\Hash;
use Spatie\Permission\Models\Role;

class DatabaseSeeder extends Seeder
{
    /**
     * Seed the application's database.
     * Cette version ne contient PAS de données de test, uniquement l'initialisation obligatoire.
     */
    public function run(): void
    {
        // 1. Création des rôles obligatoires pour le système
        $roles = ['admin', 'driver', 'client'];
        foreach ($roles as $roleName) {
            Role::firstOrCreate(['name' => $roleName, 'guard_name' => 'web']);
        }

        // 2. Création du compte Administrateur par défaut
        $admin = User::firstOrCreate(
            ['email' => 'admin@delivery.mg'],
            [
                'name'     => 'Administrateur Système',
                'password' => Hash::make('password'),
                'phone'    => '+261 34 00 000 00',
            ]
        );

        if (!$admin->hasRole('admin')) {
            $admin->assignRole('admin');
        }

        $this->command->info('✅ Système initialisé avec succès.');
        $this->command->info('🚀 Rôles créés : admin, driver, client');
        $this->command->info('👤 Admin par défaut : admin@delivery.mg / password');
    }
}
