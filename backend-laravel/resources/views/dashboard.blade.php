{{-- resources/views/dashboard.blade.php --}}

<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>GPS Delivery — Admin</title>

    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link href="https://fonts.googleapis.com/css2?family=Space+Mono:wght@400;700&family=Syne:wght@400;600;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />

    <style>
        :root {
            --bg: #0a0e1a;
            --surface: #111827;
            --surface-2: #1c2539;
            --border: #1e2d45;
            --accent: #00d4ff;
            --accent-2: #7c3aed;
            --success: #10b981;
            --warning: #f59e0b;
            --danger: #ef4444;
            --text: #e2e8f0;
            --muted: #64748b;
            --font-mono: 'Space Mono', monospace;
            --font-sans: 'Syne', sans-serif;
        }

        *{
            margin:0;
            padding:0;
            box-sizing:border-box;
        }

        body{
            background:var(--bg);
            color:var(--text);
            font-family:var(--font-sans);
            min-height:100vh;
        }

        .layout{
            display:flex;
            min-height:100vh;
        }

        .sidebar{
            width:240px;
            background:var(--surface);
            border-right:1px solid var(--border);
            display:flex;
            flex-direction:column;
            position:fixed;
            height:100vh;
            z-index:10;
        }

        .logo{
            padding:24px 20px;
            border-bottom:1px solid var(--border);
            font-size:18px;
            font-weight:800;
            display:flex;
            align-items:center;
            gap:10px;
        }

        .logo-dot{
            width:10px;
            height:10px;
            background:var(--accent);
            border-radius:50%;
            animation:pulse 2s infinite;
        }

        @keyframes pulse{
            0%,100%{
                opacity:1;
                transform:scale(1)
            }

            50%{
                opacity:.6;
                transform:scale(1.3)
            }
        }

        nav{
            padding:16px 0;
            flex:1;
        }

        .nav-item{
            display:flex;
            align-items:center;
            gap:12px;
            padding:12px 20px;
            color:var(--muted);
            text-decoration:none;
            font-size:14px;
            font-weight:600;
            border-left:3px solid transparent;
            transition:all .2s;
        }

        .nav-item:hover,
        .nav-item.active{
            color:var(--text);
            border-left-color:var(--accent);
            background:rgba(0,212,255,.05);
        }

        .main{
            margin-left:240px;
            flex:1;
            padding:32px;
        }

        .topbar{
            display:flex;
            justify-content:space-between;
            align-items:center;
            margin-bottom:32px;
            padding-bottom:24px;
            border-bottom:1px solid var(--border);
        }

        .page-title{
            font-size:28px;
            font-weight:800;
        }

        .page-title span{
            color:var(--accent);
        }

        .badge{
            background:var(--accent);
            color:#000;
            font-size:11px;
            font-weight:700;
            padding:3px 8px;
            border-radius:20px;
            font-family:var(--font-mono);
        }

        .stats-grid{
            display:grid;
            grid-template-columns:repeat(4,1fr);
            gap:16px;
            margin-bottom:32px;
        }

        .stat-card{
            background:var(--surface);
            border:1px solid var(--border);
            border-radius:12px;
            padding:20px;
            position:relative;
        }

        .stat-card::before{
            content:'';
            position:absolute;
            top:0;
            left:0;
            right:0;
            height:2px;
        }

        .cyan::before{
            background:var(--accent);
        }

        .green::before{
            background:var(--success);
        }

        .purple::before{
            background:var(--accent-2);
        }

        .orange::before{
            background:var(--warning);
        }

        .stat-label{
            font-size:12px;
            color:var(--muted);
            margin-bottom:8px;
            text-transform:uppercase;
        }

        .stat-value{
            font-size:36px;
            font-weight:800;
            font-family:var(--font-mono);
        }

        .stat-sub{
            font-size:12px;
            color:var(--muted);
            margin-top:8px;
        }

        .text-cyan{
            color:var(--accent);
        }

        .text-green{
            color:var(--success);
        }

        .text-purple{
            color:var(--accent-2);
        }

        .text-orange{
            color:var(--warning);
        }

        .card{
            background:var(--surface);
            border:1px solid var(--border);
            border-radius:12px;
            overflow:hidden;
            margin-bottom:24px;
        }

        .card-header{
            padding:16px 20px;
            border-bottom:1px solid var(--border);
            display:flex;
            justify-content:space-between;
            align-items:center;
        }

        .card-title{
            font-size:14px;
            font-weight:700;
            text-transform:uppercase;
        }

        table{
            width:100%;
            border-collapse:collapse;
        }

        th{
            font-size:11px;
            color:var(--muted);
            text-transform:uppercase;
            padding:12px 16px;
            text-align:left;
            border-bottom:1px solid var(--border);
        }

        td{
            padding:12px 16px;
            font-size:13px;
            border-bottom:1px solid rgba(30,45,69,.5);
        }

        tr:last-child td{
            border-bottom:none;
        }

        tr:hover td{
            background:rgba(0,212,255,.03);
        }

        .status-badge{
            display:inline-block;
            padding:3px 10px;
            border-radius:20px;
            font-size:11px;
            font-weight:700;
            font-family:var(--font-mono);
        }

        .status-pending{
            background:rgba(100,116,139,.2);
            color:#94a3b8;
        }

        .status-assigned{
            background:rgba(124,58,237,.2);
            color:#a78bfa;
        }

        .status-picked_up{
            background:rgba(245,158,11,.2);
            color:#fbbf24;
        }

        .status-in_transit{
            background:rgba(0,212,255,.15);
            color:var(--accent);
        }

        .status-delivered{
            background:rgba(16,185,129,.2);
            color:#34d399;
        }

        .status-failed{
            background:rgba(239,68,68,.2);
            color:#f87171;
        }

        .priority-normal{
            color:var(--muted);
        }

        .priority-express{
            color:var(--warning);
        }

        .priority-urgent{
            color:var(--danger);
        }

        .grid-2{
            display:grid;
            grid-template-columns:1fr 1fr;
            gap:20px;
        }

        .map-container{
            background:var(--surface-2);
            border-radius:8px;
            height:280px;
            position:relative;
            overflow:hidden;
        }

        .map-grid{
            position:absolute;
            inset:0;
            opacity:.15;
            background-image:
                linear-gradient(var(--accent) 1px, transparent 1px),
                linear-gradient(90deg, var(--accent) 1px, transparent 1px);
            background-size:40px 40px;
        }

        .driver-dot{
            position:absolute;
            width:12px;
            height:12px;
            border-radius:50%;
            background:var(--accent);
            box-shadow:0 0 0 4px rgba(0,212,255,.3);
            animation:pulse 2s infinite;
        }

        .driver-row{
            display:flex;
            align-items:center;
            gap:12px;
            padding:12px 16px;
            border-bottom:1px solid rgba(30,45,69,.5);
        }

        .driver-avatar{
            width:36px;
            height:36px;
            border-radius:50%;
            background:var(--surface-2);
            display:flex;
            align-items:center;
            justify-content:center;
            font-weight:700;
        }

        .dot{
            width:7px;
            height:7px;
            border-radius:50%;
            display:inline-block;
            margin-right:5px;
        }

        .dot-available{
            background:var(--success);
        }

        .dot-busy{
            background:var(--accent);
        }

        .dot-offline{
            background:var(--muted);
        }

        @media(max-width:1100px){
            .stats-grid{
                grid-template-columns:repeat(2,1fr);
            }

            .grid-2{
                grid-template-columns:1fr;
            }
        }
    </style>
</head>

<body>

<div class="layout">

    {{-- MAIN --}}
    <main class="main">

        {{-- TOPBAR --}}
        <div class="topbar">

            <div>
                <div class="page-title">
                    Dashboard <span>Admin</span>
                </div>

                <div style="font-size:13px;color:var(--muted);margin-top:4px">
                    Vue d'ensemble — GPS Delivery Tracker
                </div>
            </div>

            <div style="display:flex;gap:12px;align-items:center">

                <span class="badge">LIVE</span>

                <a href="#"
                   style="background:var(--accent);
                          color:#000;
                          text-decoration:none;
                          padding:8px 16px;
                          border-radius:8px;
                          font-weight:700;
                          font-size:12px;">
                    + Nouvelle livraison
                </a>

            </div>

        </div>

        {{-- STATS --}}
        <div class="stats-grid">

            <div class="stat-card cyan">
                <div class="stat-label">Total livraisons</div>

                <div class="stat-value text-cyan">
                    {{ $stats['total_deliveries'] }}
                </div>

                <div class="stat-sub">
                    +{{ $stats['today_count'] }} aujourd'hui
                </div>
            </div>

            <div class="stat-card green">
                <div class="stat-label">Livrées</div>

                <div class="stat-value text-green">
                    {{ $stats['delivered'] }}
                </div>

                <div class="stat-sub">
                    Taux :
                    {{ $stats['total_deliveries'] > 0
                        ? round(($stats['delivered'] / $stats['total_deliveries']) * 100)
                        : 0 }}%
                </div>
            </div>

            <div class="stat-card purple">
                <div class="stat-label">En cours</div>

                <div class="stat-value text-purple">
                    {{ $stats['in_progress'] }}
                </div>

                <div class="stat-sub">
                    assigned + transit
                </div>
            </div>

            <div class="stat-card orange">
                <div class="stat-label">Revenus (Ar)</div>

                <div class="stat-value text-orange" style="font-size:26px">
                    {{ number_format($stats['revenue'], 0, ',', ' ') }}
                </div>

                <div class="stat-sub">
                    Total
                </div>
            </div>

        </div>

        {{-- LIVRAISONS --}}
        <div class="card">

            <div class="card-header">
                <span class="card-title">
                    Livraisons récentes
                </span>
            </div>

            <table>

                <thead>
                <tr>
                    <th>N° Commande</th>
                    <th>Client</th>
                    <th>Destinataire</th>
                    <th>Livreur</th>
                    <th>Statut</th>
                </tr>
                </thead>

                <tbody>

                @forelse($recentDeliveries as $delivery)

                    <tr>

                        <td style="font-family:var(--font-mono)">
                            {{ $delivery->order->order_number ?? 'N/A' }}
                        </td>

                        <td>
                            {{ $delivery->order->client->name ?? 'N/A' }}
                        </td>

                        <td>
                            {{ $delivery->delivery_address ?? 'Destination inconnue' }}
                        </td>

                        <td>
                            {{ $delivery->driver?->user?->name ?? 'Non assigné' }}
                        </td>

                        <td>

                            <span class="status-badge status-{{ $delivery->status }}">
                                {{ $delivery->status_label ?? ucfirst($delivery->status) }}
                            </span>

                        </td>

                    </tr>

                @empty

                    <tr>
                        <td colspan="5">
                            Aucune livraison trouvée
                        </td>
                    </tr>

                @endforelse

                </tbody>

            </table>

        </div>

        {{-- GRID --}}
        <div class="grid-2">

            {{-- MAP --}}
            <div class="card">

                <div class="card-header">
                    <span class="card-title">
                        Livreurs en temps réel
                    </span>

                    <span class="badge">
                        GPS LIVE
                    </span>
                </div>

                <div style="padding:16px">

                    <div id="map" style="height: 280px; border-radius: 8px;"></div>

                </div>

            </div>

            {{-- DRIVERS --}}
            <div class="card">

                <div class="card-header">
                    <span class="card-title">
                        Statut des livreurs
                    </span>
                </div>

                <div style="padding:16px">

                    <div style="margin-bottom:20px">

                        <div style="margin-bottom:8px">
                            Disponible :
                            {{ $driverStats['available'] }}
                        </div>

                        <div style="margin-bottom:8px">
                            Occupé :
                            {{ $driverStats['busy'] }}
                        </div>

                        <div>
                            Hors ligne :
                            {{ $driverStats['offline'] }}
                        </div>

                    </div>

                </div>

                @foreach($drivers as $driver)

                    <div class="driver-row">

                        <div class="driver-avatar">

                            {{ strtoupper(substr($driver->user->name ?? 'DR', 0, 2)) }}

                        </div>

                        <div style="flex:1">

                            <div style="font-size:13px;font-weight:600">
                                {{ $driver->user->name ?? 'Livreur' }}
                            </div>

                            <div style="font-size:11px;color:var(--muted)">
                                {{ $driver->vehicle_plate ?? 'Aucune plaque' }}
                            </div>

                        </div>

                        <div style="font-size:11px">

                            @if($driver->status === 'available')

                                <span class="dot dot-available"></span>

                            @elseif($driver->status === 'busy')

                                <span class="dot dot-busy"></span>

                            @else

                                <span class="dot dot-offline"></span>

                            @endif

                            {{ ucfirst($driver->status) }}

                        </div>

                    </div>

                @endforeach

            </div>

        </div>

    </main>

</div>

<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<script src="https://js.pusher.com/8.2.0/pusher.min.js"></script>
<script>
    // Initialisation de la carte
    var map = L.map('map').setView([-18.8792, 47.5079], 12);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap contributors'
    }).addTo(map);

    var markers = {};

    // Ajouter les livreurs déjà actifs
    @foreach($activeDrivers as $driver)
        @if($driver->current_lat && $driver->current_lng)
            markers[{{ $driver->id }}] = L.marker([{{ $driver->current_lat }}, {{ $driver->current_lng }}])
                .addTo(map)
                .bindPopup('{{ $driver->user->name }}');
        @endif
    @endforeach

    // Configuration Pusher (si configuré)
    var pusher = new Pusher('{{ env('PUSHER_APP_KEY') }}', {
        cluster: '{{ env('PUSHER_APP_CLUSTER') }}'
    });

    var channel = pusher.subscribe('driver-locations');
    channel.bind('App\\Events\\DriverLocationUpdated', function(data) {
        if (markers[data.driver_id]) {
            markers[data.driver_id].setLatLng([data.lat, data.lng]);
        } else {
            markers[data.driver_id] = L.marker([data.lat, data.lng])
                .addTo(map)
                .bindPopup('Livreur #' + data.driver_id);
        }
    });

    // Recharger la page si statut livraison change
    var statusChannel = pusher.subscribe('admin-deliveries');
    statusChannel.bind('App\\Events\\DeliveryStatusUpdated', function(data) {
        // Optionnel: Recharger discrètement ou notifier
        console.log('Livraison mise à jour:', data);
    });
</script>

</body>
</html>
