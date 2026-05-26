<?php

namespace Database\Seeders;

use App\Models\Delivery;
use App\Models\Driver;
use App\Models\Order;
use App\Models\Package;
use App\Models\Status;
use App\Models\User;
use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\Hash;
use Spatie\Permission\Models\Role;
use Spatie\Permission\Models\Permission;

class DatabaseSeeder extends Seeder
{
    public function run(): void
    {
        // role
        $roles = ['admin', 'driver', 'client'];
        foreach ($roles as $role) {
            Role::firstOrCreate(['name' => $role]);
        }

        // admin
        $admin = User::firstOrCreate(
            ['email' => 'admin@delivery.mg'],
            [
                'name'     => 'Administrateur',
                'password' => Hash::make('password'),
                'phone'    => '+261 34 00 000 00',
            ]
        );
        $admin->assignRole('admin');

        // livreur
        $driversData = [
            ['name' => 'Rakoto Jean',   'email' => 'rakoto@delivery.mg',   'phone' => '+261 34 11 111 11', 'vehicle' => 'moto',    'plate' => 'IMM-001-T', 'lat' => -18.9101, 'lng' => 47.5362],
            ['name' => 'Rabe Marie',    'email' => 'rabe@delivery.mg',     'phone' => '+261 34 22 222 22', 'vehicle' => 'voiture', 'plate' => 'IMM-002-T', 'lat' => -18.8932, 'lng' => 47.5167],
            ['name' => 'Razafy Paul',   'email' => 'razafy@delivery.mg',   'phone' => '+261 34 33 333 33', 'vehicle' => 'moto',    'plate' => 'IMM-003-T', 'lat' => -18.9228, 'lng' => 47.5500],
            ['name' => 'Randria Eline', 'email' => 'randria@delivery.mg',  'phone' => '+261 34 44 444 44', 'vehicle' => 'camion',  'plate' => 'IMM-004-T', 'lat' => -18.9050, 'lng' => 47.5280],
        ];

        $drivers = [];
        foreach ($driversData as $i => $d) {
            $user = User::firstOrCreate(
                ['email' => $d['email']],
                ['name' => $d['name'], 'password' => Hash::make('password'), 'phone' => $d['phone']]
            );
            $user->assignRole('driver');

            $driver = Driver::firstOrCreate(
                ['user_id' => $user->id],
                [
                    'license_number'   => 'LIC-' . str_pad($i + 1, 4, '0', STR_PAD_LEFT),
                    'vehicle_type'     => $d['vehicle'],
                    'vehicle_plate'    => $d['plate'],
                    'vehicle_model'    => 'Yamaha FZ150',
                    'status'           => $i < 2 ? 'available' : 'offline',
                    'current_lat'      => $d['lat'],
                    'current_lng'      => $d['lng'],
                    'last_location_at' => now()->subMinutes(rand(1, 30)),
                    'rating'           => round(rand(380, 500) / 100, 2),
                    'total_deliveries' => rand(10, 150),
                ]
            );
            $drivers[] = $driver;
        }

        // client
        $clientsData = [
            ['name' => 'Rasoa Hanta',    'email' => 'rasoa@gmail.com'],
            ['name' => 'Andry Lalao',    'email' => 'andry@gmail.com'],
            ['name' => 'Fara Nirina',    'email' => 'fara@gmail.com'],
            ['name' => 'Solo Manana',    'email' => 'solo@gmail.com'],
            ['name' => 'Nivo Patrick',   'email' => 'nivo@gmail.com'],
        ];

        $clients = [];
        foreach ($clientsData as $c) {
            $user = User::firstOrCreate(
                ['email' => $c['email']],
                ['name' => $c['name'], 'password' => Hash::make('password')]
            );
            $user->assignRole('client');
            $clients[] = $user;
        }


        $statusFlow = [
            'pending'    => ['pending'],
            'assigned'   => ['pending', 'assigned'],
            'picked_up'  => ['pending', 'assigned', 'picked_up'],
            'in_transit' => ['pending', 'assigned', 'picked_up', 'in_transit'],
            'delivered'  => ['pending', 'assigned', 'picked_up', 'in_transit', 'delivered'],
            'failed'     => ['pending', 'assigned', 'picked_up', 'failed'],
        ];

        $scenarios = [
            ['status' => 'delivered',  'driver' => 0, 'priority' => 'normal'],
            ['status' => 'delivered',  'driver' => 1, 'priority' => 'express'],
            ['status' => 'delivered',  'driver' => 2, 'priority' => 'normal'],
            ['status' => 'in_transit', 'driver' => 0, 'priority' => 'urgent'],
            ['status' => 'in_transit', 'driver' => 1, 'priority' => 'express'],
            ['status' => 'in_transit', 'driver' => 3, 'priority' => 'normal'],
            ['status' => 'picked_up',  'driver' => 2, 'priority' => 'normal'],
            ['status' => 'assigned',   'driver' => 0, 'priority' => 'express'],
            ['status' => 'assigned',   'driver' => 1, 'priority' => 'normal'],
            ['status' => 'pending',    'driver' => null, 'priority' => 'normal'],
            ['status' => 'delivered',  'driver' => 0, 'priority' => 'normal'],
            ['status' => 'delivered',  'driver' => 3, 'priority' => 'urgent'],
            ['status' => 'failed',     'driver' => 2, 'priority' => 'normal'],
            ['status' => 'in_transit', 'driver' => 1, 'priority' => 'express'],
            ['status' => 'pending',    'driver' => null, 'priority' => 'urgent'],
            ['status' => 'assigned',   'driver' => 0, 'priority' => 'normal'],
            ['status' => 'delivered',  'driver' => 1, 'priority' => 'normal'],
            ['status' => 'picked_up',  'driver' => 3, 'priority' => 'express'],
            ['status' => 'pending',    'driver' => null, 'priority' => 'normal'],
            ['status' => 'in_transit', 'driver' => 2, 'priority' => 'urgent'],
        ];

        $descriptions = [
            'Téléphone portable Samsung Galaxy', 'Vêtements boutique mode',
            'Pièces électroniques', 'Médicaments pharmacie', 'Livres scolaires',
            'Chaussures Nike', 'Accessoires informatique', 'Alimentation épicerie fine',
            'Matériel bureau', 'Jouets enfants', 'Cosmétiques beauté',
            'Documents administratifs', 'Bijoux artisanaux', 'Équipement sportif',
            'Textile maison', 'Produits bio', 'Matériel médical', 'Instruments musique',
            'Matériaux construction', 'Articles ménager',
        ];

        $addresses = [
            ['address' => 'Analakely, Antananarivo',          'lat' => -18.9101, 'lng' => 47.5362],
            ['address' => 'Tsimbazaza, Antananarivo',         'lat' => -18.9170, 'lng' => 47.5186],
            ['address' => 'Andohalo, Antananarivo',           'lat' => -18.9137, 'lng' => 47.5267],
            ['address' => 'Isotry, Antananarivo',             'lat' => -18.9048, 'lng' => 47.5154],
            ['address' => 'Mahamasina, Antananarivo',         'lat' => -18.9228, 'lng' => 47.5325],
            ['address' => 'Behoririka, Antananarivo',         'lat' => -18.9086, 'lng' => 47.5298],
            ['address' => 'Anosy, Antananarivo',              'lat' => -18.9198, 'lng' => 47.5388],
            ['address' => 'Ankadifotsy, Antananarivo',        'lat' => -18.9012, 'lng' => 47.5411],
            ['address' => 'Tana-Ville Haute, Antananarivo',   'lat' => -18.9145, 'lng' => 47.5362],
            ['address' => 'Ambohijanaka, Antananarivo',       'lat' => -18.9300, 'lng' => 47.5450],
        ];

        foreach ($scenarios as $i => $scenario) {
            $client    = $clients[$i % count($clients)];
            $fromAddr  = $addresses[$i % count($addresses)];
            $toAddr    = $addresses[($i + 3) % count($addresses)];
            $driverObj = $scenario['driver'] !== null ? $drivers[$scenario['driver']] : null;

            // Package
            $package = Package::create([
                'description'   => $descriptions[$i],
                'weight_kg'     => round(rand(1, 200) / 10, 1),
                'fragile'       => $i % 4 === 0 ? 'yes' : 'no',
                'declared_value' => rand(5, 500) * 1000,
                'created_by'    => $admin->id,
            ]);

            // Order
            $order = Order::create([
                'client_id'         => $client->id,
                'package_id'        => $package->id,
                'sender_name'       => 'Boutique Express MG',
                'sender_phone'      => '+261 20 22 000 00',
                'sender_address'    => $fromAddr['address'],
                'sender_lat'        => $fromAddr['lat'],
                'sender_lng'        => $fromAddr['lng'],
                'recipient_name'    => $client->name,
                'recipient_phone'   => '+261 34 ' . str_pad($i * 11, 8, '0', STR_PAD_LEFT),
                'recipient_address' => $toAddr['address'],
                'recipient_lat'     => $toAddr['lat'],
                'recipient_lng'     => $toAddr['lng'],
                'priority'          => $scenario['priority'],
                'delivery_fee'      => rand(3, 25) * 1000,
                'scheduled_at'      => now()->addHours(rand(1, 48)),
            ]);

            // Delivery
            $delivery = Delivery::create([
                'order_id'          => $order->id,
                'driver_id'         => $driverObj?->id,
                'status'            => $scenario['status'],
                'assigned_at'       => $driverObj ? now()->subHours(rand(1, 8)) : null,
                'picked_up_at'      => in_array($scenario['status'], ['picked_up', 'in_transit', 'delivered', 'failed'])
                    ? now()->subHours(rand(1, 6)) : null,
                'delivered_at'      => $scenario['status'] === 'delivered' ? now()->subMinutes(rand(10, 120)) : null,
                'estimated_arrival' => now()->addHours(rand(1, 4)),
                'failure_reason'    => $scenario['status'] === 'failed' ? 'Destinataire absent' : null,
            ]);

            // Historique des statuts
            foreach ($statusFlow[$scenario['status']] as $j => $step) {
                Status::create([
                    'delivery_id' => $delivery->id,
                    'status'      => $step,
                    'label'       => Delivery::STATUS_LABELS[$step],
                    'note'        => $step === 'assigned' ? "Assigné à {$driverObj?->user->name}" : null,
                    'lat'         => $fromAddr['lat'] + ($j * 0.002),
                    'lng'         => $fromAddr['lng'] + ($j * 0.002),
                    'occurred_at' => now()->subHours(8 - $j),
                ]);
            }
        }

        $this->command->info('✅ Seeder terminé : 20 livraisons créées avec statuts variés');
        $this->command->info('👤 Admin: admin@delivery.mg / password');
        $this->command->info('🚴 Driver: rakoto@delivery.mg / password');
        $this->command->info('👥 Client: rasoa@gmail.com / password');
    }
}
