{{-- resources/views/dashboard.blade.php --}}

<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>GPS Delivery — Admin Panel</title>
    <meta http-equiv="refresh" content="60">

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

        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { background: var(--bg); color: var(--text); font-family: var(--font-sans); min-height: 100vh; }

        .layout { display: flex; min-height: 100vh; }

        /* Sidebar */
        .sidebar {
            width: 240px;
            background: var(--surface);
            border-right: 1px solid var(--border);
            display: flex;
            flex-direction: column;
            position: fixed;
            height: 100vh;
            z-index: 100;
        }
        .logo { padding: 24px 20px; border-bottom: 1px solid var(--border); font-size: 18px; font-weight: 800; display: flex; align-items: center; gap: 10px; }
        .logo-dot { width: 10px; height: 10px; background: var(--accent); border-radius: 50%; animation: pulse 2s infinite; }
        @keyframes pulse { 0%, 100% { opacity: 1; transform: scale(1); } 50% { opacity: .6; transform: scale(1.3); } }

        nav { padding: 16px 0; flex: 1; }
        .nav-item {
            display: flex; align-items: center; gap: 12px; padding: 12px 20px; color: var(--muted);
            text-decoration: none; font-size: 14px; font-weight: 600; border-left: 3px solid transparent;
            cursor: pointer; transition: all .2s;
        }
        .nav-item:hover, .nav-item.active { color: var(--text); border-left-color: var(--accent); background: rgba(0, 212, 255, .05); }

        /* Main Content */
        .main { margin-left: 240px; flex: 1; padding: 32px; }
        .topbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 32px; padding-bottom: 24px; border-bottom: 1px solid var(--border); }
        .page-title { font-size: 28px; font-weight: 800; }
        .page-title span { color: var(--accent); }

        /* Tabs */
        .tab-content { display: none; }
        .tab-content.active { display: block; animation: fadeIn 0.3s ease; }
        @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }

        /* Stats Grid */
        .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; margin-bottom: 32px; }
        .stat-card { background: var(--surface); border: 1px solid var(--border); border-radius: 12px; padding: 20px; position: relative; }
        .stat-card::before { content: ''; position: absolute; top: 0; left: 0; right: 0; height: 2px; }
        .cyan::before { background: var(--accent); }
        .green::before { background: var(--success); }
        .purple::before { background: var(--accent-2); }
        .orange::before { background: var(--warning); }
        .stat-label { font-size: 12px; color: var(--muted); margin-bottom: 8px; text-transform: uppercase; }
        .stat-value { font-size: 32px; font-weight: 800; font-family: var(--font-mono); }
        .text-cyan { color: var(--accent); }
        .text-green { color: var(--success); }
        .text-purple { color: var(--accent-2); }
        .text-orange { color: var(--warning); }

        /* Cards & Tables */
        .card { background: var(--surface); border: 1px solid var(--border); border-radius: 12px; overflow: hidden; margin-bottom: 24px; }
        .card-header { padding: 16px 20px; border-bottom: 1px solid var(--border); display: flex; justify-content: space-between; align-items: center; }
        .card-title { font-size: 14px; font-weight: 700; text-transform: uppercase; }
        table { width: 100%; border-collapse: collapse; }
        th { font-size: 11px; color: var(--muted); text-transform: uppercase; padding: 12px 16px; text-align: left; border-bottom: 1px solid var(--border); }
        td { padding: 12px 16px; font-size: 13px; border-bottom: 1px solid rgba(30, 45, 69, .5); }
        tr:hover td { background: rgba(0, 212, 255, .03); }

        .status-badge { display: inline-block; padding: 3px 10px; border-radius: 20px; font-size: 10px; font-weight: 700; font-family: var(--font-mono); text-transform: uppercase; }
        .status-pending { background: rgba(100, 116, 139, .2); color: #94a3b8; }
        .status-assigned { background: rgba(124, 58, 237, .2); color: #a78bfa; }
        .status-picked_up { background: rgba(245, 158, 11, .2); color: #fbbf24; }
        .status-in_transit { background: rgba(0, 212, 255, .15); color: var(--accent); }
        .status-delivered { background: rgba(16, 185, 129, .2); color: #34d399; }
        .status-failed { background: rgba(239, 68, 68, .2); color: #f87171; }

        /* Details Modal / View */
        .modal {
            display: none; position: fixed; inset: 0; background: rgba(0,0,0,0.8);
            z-index: 1000; align-items: center; justify-content: center; padding: 20px;
        }
        .modal-content {
            background: var(--surface); border: 1px solid var(--border); border-radius: 16px;
            max-width: 900px; width: 100%; max-height: 90vh; overflow-y: auto; padding: 24px;
        }
        .close-modal { float: right; cursor: pointer; color: var(--muted); font-size: 24px; }

        .detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-top: 20px; }
        .proof-img { width: 100%; border-radius: 12px; border: 1px solid var(--border); margin-top: 10px; }
        .signature-box { background: white; border-radius: 12px; padding: 10px; margin-top: 10px; display: flex; justify-content: center; }
        .signature-img { max-height: 150px; filter: contrast(150%); }

        /* Map styles */
        #map-main, #map-detail, #map-drivers { height: 400px; border-radius: 12px; background: #0e1217; }
        .btn {
            background: var(--accent); color: #000; border: none; padding: 6px 12px;
            border-radius: 6px; font-size: 11px; font-weight: 700; cursor: pointer; transition: 0.2s;
        }
        .btn:hover { opacity: 0.8; transform: translateY(-1px); }
        .btn-secondary { background: var(--surface-2); color: var(--text); border: 1px solid var(--border); }

        .badge { background: var(--accent); color: #000; font-size: 10px; font-weight: 700; padding: 2px 6px; border-radius: 4px; }
    </style>
</head>

<body>

<div class="layout">

    <aside class="sidebar">
        <div class="logo">
            <div class="logo-dot"></div>
            GPS DELIVERY
        </div>
        <nav id="sidebar-nav">
            <a class="nav-item active" data-tab="overview">
                <span>Dashboard</span>
            </a>
            <a class="nav-item" data-tab="deliveries">
                <span>Livraisons</span>
            </a>
            <a class="nav-item" data-tab="drivers">
                <span>Livreurs</span>
            </a>
            <a class="nav-item" data-tab="clients">
                <span>Clients</span>
            </a>
        </nav>
        <div style="padding: 20px; border-top: 1px solid var(--border); font-size: 10px; color: var(--muted);">
            ADMIN PANEL v2.0
        </div>
    </aside>

    <main class="main">

        {{-- TOPBAR --}}
        <div class="topbar">
            <div>
                <div class="page-title" id="page-title">Tableau de <span>Bord</span></div>
                <div style="font-size:13px;color:var(--muted);margin-top:4px" id="page-subtitle">Aperçu temps réel des opérations</div>
            </div>
            <div style="display:flex;gap:12px;align-items:center">
                <span class="badge">LIVE CONNECTED</span>
            </div>
        </div>

        {{-- TAB: OVERVIEW --}}
        <div id="overview" class="tab-content active">
            <div class="stats-grid">
                <div class="stat-card cyan">
                    <div class="stat-label">Total Livraisons</div>
                    <div class="stat-value text-cyan">{{ $stats['total_deliveries'] }}</div>
                </div>
                <div class="stat-card green">
                    <div class="stat-label">Livrées</div>
                    <div class="stat-value text-green">{{ $stats['delivered'] }}</div>
                </div>
                <div class="stat-card purple">
                    <div class="stat-label">En cours</div>
                    <div class="stat-value text-purple">{{ $stats['in_progress'] }}</div>
                </div>
            </div>

            <div class="card">
                <div class="card-header">
                    <span class="card-title">Localisation des livreurs actifs</span>
                </div>
                <div style="padding: 16px;">
                    <div id="map-main"></div>
                </div>
            </div>
        </div>

        {{-- TAB: DELIVERIES --}}
        <div id="deliveries" class="tab-content">
            <div class="card">
                <div class="card-header">
                    <span class="card-title">Historique des Livraisons</span>
                </div>
                <table>
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>N° Commande</th>
                            <th>Client</th>
                            <th>Livreur</th>
                            <th>Statut</th>
                            <th>Date</th>
                            <th>Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        @foreach($allDeliveries as $delivery)
                        <tr>
                            <td style="font-family: var(--font-mono);">#{{ $delivery->id }}</td>
                            <td>{{ $delivery->order->order_number }}</td>
                            <td>{{ $delivery->order->client->name }}</td>
                            <td>{{ $delivery->driver?->user?->name ?? '—' }}</td>
                            <td>
                                <span class="status-badge status-{{ $delivery->status }}">
                                    {{ $delivery->status_label }}
                                </span>
                            </td>
                            <td>{{ $delivery->created_at->format('d/m/Y H:i') }}</td>
                            <td>
                                <button class="btn btn-show-details" data-delivery='@json($delivery)'>Détails</button>
                            </td>
                        </tr>
                        @endforeach
                    </tbody>
                </table>
            </div>
        </div>

        {{-- TAB: DRIVERS --}}
        <div id="drivers" class="tab-content">
            <div style="display: grid; grid-template-columns: 1fr 350px; gap: 20px;">
                <div class="card">
                    <div class="card-header"><span class="card-title">Liste des Livreurs</span></div>
                    <table>
                        <thead>
                            <tr>
                                <th>Livreur</th>
                                <th>Véhicule</th>
                                <th>Status</th>
                                <th>Livraisons</th>
                                <th>Note</th>
                            </tr>
                        </thead>
                        <tbody>
                            @foreach($drivers as $driver)
                            <tr>
                                <td>
                                    <div style="font-weight: 700;">{{ $driver->user->name }}</div>
                                    <div style="font-size: 11px; color: var(--muted);">{{ $driver->user->phone }}</div>
                                </td>
                                <td>
                                    <div>{{ $driver->vehicle_model }}</div>
                                    <div style="font-size: 11px; color: var(--muted);">{{ $driver->vehicle_plate }}</div>
                                </td>
                                <td>
                                    <span class="status-badge {{ $driver->status === 'available' ? 'status-delivered' : ($driver->status === 'busy' ? 'status-in_transit' : 'status-pending') }}">
                                        {{ $driver->status }}
                                    </span>
                                </td>
                                <td>{{ $driver->deliveries->count() }}</td>
                                <td style="color: var(--warning); font-weight: 700;">{{ $driver->rating ?? 'N/A' }} ★</td>
                            </tr>
                            @endforeach
                        </tbody>
                    </table>
                </div>
                <div>
                    <div class="card">
                        <div class="card-header"><span class="card-title">Stats Livreurs</span></div>
                        <div style="padding: 20px;">
                            <div style="margin-bottom: 12px; display: flex; justify-content: space-between;">
                                <span>Disponible</span>
                                <span class="text-green">{{ $driverStats['available'] }}</span>
                            </div>
                            <div style="margin-bottom: 12px; display: flex; justify-content: space-between;">
                                <span>En service</span>
                                <span class="text-cyan">{{ $driverStats['busy'] }}</span>
                            </div>
                            <div style="display: flex; justify-content: space-between;">
                                <span>Hors ligne</span>
                                <span class="text-muted">{{ $driverStats['offline'] }}</span>
                            </div>
                        </div>
                    </div>
                    <div class="card">
                        <div class="card-header"><span class="card-title">Carte des livreurs</span></div>
                        <div style="padding: 10px;">
                            <div id="map-drivers" style="height: 250px;"></div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        {{-- TAB: CLIENTS --}}
        <div id="clients" class="tab-content">
            <div class="card">
                <div class="card-header"><span class="card-title">Annuaire Clients</span></div>
                <table>
                    <thead>
                        <tr>
                            <th>Nom</th>
                            <th>Email</th>
                            <th>Téléphone</th>
                            <th>Total Commandes</th>
                            <th>Dernière activité</th>
                        </tr>
                    </thead>
                    <tbody>
                        @foreach($clients as $client)
                        <tr>
                            <td style="font-weight: 700;">{{ $client->name }}</td>
                            <td>{{ $client->email }}</td>
                            <td>{{ $client->phone ?? '—' }}</td>
                            <td>{{ $client->orders_count }}</td>
                            <td>{{ $client->updated_at->diffForHumans() }}</td>
                        </tr>
                        @endforeach
                    </tbody>
                </table>
            </div>
        </div>

    </main>
</div>

{{-- MODAL DETAILS --}}
<div id="delivery-modal" class="modal">
    <div class="modal-content">
        <span class="close-modal">&times;</span>
        <h2 id="modal-title" style="margin-bottom: 10px;">Détails Livraison #000</h2>
        <div id="modal-actions" style="display: flex; gap: 10px; margin-bottom: 20px; align-items: center;">
            <span id="modal-status" class="status-badge">STATUS</span>
            <span id="modal-date" style="font-size: 13px; color: var(--muted);">Date</span>
            <a id="btn-download-pdf" href="#" target="_blank" class="btn btn-secondary" style="font-size: 10px; text-decoration: none;">
                📄 Télécharger Bon (PDF)
            </a>
        </div>

        <div class="detail-grid">
            <div>
                <div class="card">
                    <div class="card-header"><span class="card-title">Informations</span></div>
                    <div style="padding: 16px; font-size: 13px;">
                        <p style="margin-bottom: 10px;"><strong>Client :</strong> <span id="det-client"></span></p>
                        <p style="margin-bottom: 10px;"><strong>Livreur :</strong> <span id="det-driver"></span></p>
                        <p style="margin-bottom: 10px;"><strong>Expéditeur :</strong> <span id="det-sender"></span></p>
                        <p style="margin-bottom: 10px;"><strong>Destinataire :</strong> <span id="det-recipient"></span></p>
                        <p><strong>Adresse :</strong> <span id="det-address"></span></p>
                    </div>
                </div>

                <div id="proof-section" style="display: none;">
                    <div class="card">
                        <div class="card-header"><span class="card-title">Preuves de livraison</span></div>
                        <div style="padding: 16px;">
                            <div id="photo-container">
                                <label style="font-size: 11px; color: var(--muted);">PHOTO :</label>
                                <img id="det-photo" src="" class="proof-img">
                            </div>
                            <div id="signature-container" style="margin-top: 20px;">
                                <label style="font-size: 11px; color: var(--muted);">SIGNATURE :</label>
                                <div class="signature-box">
                                    <img id="det-signature" src="" class="signature-img">
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div>
                <div class="card">
                    <div class="card-header"><span class="card-title">Parcours & Localisation</span></div>
                    <div style="padding: 10px;">
                        <div id="map-detail"></div>
                        <div style="margin-top: 10px; font-size: 11px; color: var(--muted);" id="det-path-info">
                            Chargement du trajet...
                        </div>
                    </div>
                </div>

                <div class="card">
                    <div class="card-header"><span class="card-title">Historique d'étapes</span></div>
                    <div id="det-history" style="padding: 16px; font-size: 12px; max-height: 200px; overflow-y: auto;">
                        <!-- Timeline will be injected here -->
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<script src="https://js.pusher.com/8.2.0/pusher.min.js"></script>
<script>
    // TAB SYSTEM
    const navItems = document.querySelectorAll('.nav-item');
    const tabContents = document.querySelectorAll('.tab-content');
    const pageTitle = document.querySelector('#page-title');
    const pageSubtitle = document.querySelector('#page-subtitle');

    const tabData = {
        overview: { title: 'Tableau de <span>Bord</span>', subtitle: 'Aperçu temps réel des opérations' },
        deliveries: { title: 'Toutes les <span>Livraisons</span>', subtitle: 'Historique et détails des commandes' },
        drivers: { title: 'Gestion des <span>Livreurs</span>', subtitle: 'Disponibilité et performances' },
        clients: { title: 'Répertoire <span>Clients</span>', subtitle: 'Liste des utilisateurs et statistiques' }
    };

    navItems.forEach(item => {
        item.addEventListener('click', () => {
            const tabId = item.getAttribute('data-tab');

            navItems.forEach(i => i.classList.remove('active'));
            tabContents.forEach(c => c.classList.remove('active'));

            item.classList.add('active');
            document.getElementById(tabId).classList.add('active');

            pageTitle.innerHTML = tabData[tabId].title;
            pageSubtitle.innerText = tabData[tabId].subtitle;

            // Invalidate map size if visible
            if (tabId === 'overview') mapMain.invalidateSize();
            if (tabId === 'drivers') mapDrivers.invalidateSize();
        });
    });

    // MAPS INITIALIZATION
    const center = [-18.8792, 47.5079];
    const mapMain = L.map('map-main').setView(center, 12);
    const mapDrivers = L.map('map-drivers').setView(center, 11);
    const mapDetail = L.map('map-detail').setView(center, 13);

    const tileUrl = 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';
    const tileAttr = '© OpenStreetMap';

    L.tileLayer(tileUrl, { attribution: tileAttr }).addTo(mapMain);
    L.tileLayer(tileUrl, { attribution: tileAttr }).addTo(mapDrivers);
    L.tileLayer(tileUrl, { attribution: tileAttr }).addTo(mapDetail);

    let mainMarkers = {};
    let driverMarkers = {};
    let detailPathLine = null;
    let detailMarkers = [];

    // DATA FOR ACTIVE DRIVERS
    const activeDrivers = @json($activeDrivers);
    activeDrivers.forEach(d => {
        if(d.current_lat) {
            const m = L.marker([d.current_lat, d.current_lng]).addTo(mapMain).bindPopup(d.user.name);
            mainMarkers[d.id] = m;

            const m2 = L.marker([d.current_lat, d.current_lng]).addTo(mapDrivers).bindPopup(d.user.name);
            driverMarkers[d.id] = m2;
        }
    });

    // PUSHER REAL-TIME
    const pusher = new Pusher('{{ env('PUSHER_APP_KEY') }}', { cluster: '{{ env('PUSHER_APP_CLUSTER') }}' });
    const channel = pusher.subscribe('driver-locations');
    channel.bind('App\\Events\\DriverLocationUpdated', function(data) {
        const pos = [data.lat, data.lng];
        if (mainMarkers[data.driver_id]) mainMarkers[data.driver_id].setLatLng(pos);
        else mainMarkers[data.driver_id] = L.marker(pos).addTo(mapMain).bindPopup('Livreur #' + data.driver_id);

        if (driverMarkers[data.driver_id]) driverMarkers[data.driver_id].setLatLng(pos);
        else driverMarkers[data.driver_id] = L.marker(pos).addTo(mapDrivers).bindPopup('Livreur #' + data.driver_id);
    });

    // ÉCOUTE DES MISES À JOUR DE LIVRAISON (REFRAICHISSEMENT AUTO)
    const adminChannel = pusher.subscribe('admin-deliveries');
    adminChannel.bind('App\\Events\\DeliveryStatusUpdated', function(data) {
        console.log('Livraison mise à jour, rechargement...', data);
        location.reload();
    });

    // MODAL DELIVERY DETAILS
    const modal = document.getElementById('delivery-modal');
    const closeBtn = document.querySelector('.close-modal');

    document.querySelectorAll('.btn-show-details').forEach(btn => {
        btn.addEventListener('click', () => {
            const delivery = JSON.parse(btn.getAttribute('data-delivery'));
            showDeliveryDetails(delivery);
        });
    });

    closeBtn.onclick = () => modal.style.display = "none";
    window.onclick = (e) => { if(e.target == modal) modal.style.display = "none"; }

    function showDeliveryDetails(d) {
        modal.style.display = "flex";
        document.getElementById('modal-title').innerText = 'Livraison #' + d.id + ' (' + d.order.order_number + ')';
        document.getElementById('modal-status').innerText = d.status_label;
        document.getElementById('modal-status').className = 'status-badge status-' + d.status;
        document.getElementById('modal-date').innerText = new Date(d.created_at).toLocaleString();

        // PDF Link
        document.getElementById('btn-download-pdf').href = '/admin/deliveries/' + d.id + '/receipt';

        document.getElementById('det-client').innerText = d.order.client.name;
        document.getElementById('det-driver').innerText = d.driver ? d.driver.user.name : 'Non assigné';
        document.getElementById('det-sender').innerText = d.order.sender_name;
        document.getElementById('det-recipient').innerText = d.order.recipient_name;
        document.getElementById('det-address').innerText = d.order.recipient_address;

        // Proofs
        if (d.status === 'delivered') {
            document.getElementById('proof-section').style.display = 'block';
            if (d.proof_photo) {
                document.getElementById('photo-container').style.display = 'block';
                document.getElementById('det-photo').src = getImageUrl(d.proof_photo);
            } else {
                document.getElementById('photo-container').style.display = 'none';
            }

            if (d.signature) {
                document.getElementById('signature-container').style.display = 'block';
                document.getElementById('det-signature').src = getImageUrl(d.signature);
            } else {
                document.getElementById('signature-container').style.display = 'none';
            }
        } else {
            document.getElementById('proof-section').style.display = 'none';
        }

        // Timeline
        const historyDiv = document.getElementById('det-history');
        historyDiv.innerHTML = '';
        if (d.statuses && d.statuses.length > 0) {
            d.statuses.sort((a,b) => new Date(b.occurred_at) - new Date(a.occurred_at)).forEach(s => {
                const item = document.createElement('div');
                item.style.marginBottom = '10px';
                item.style.paddingLeft = '12px';
                item.style.borderLeft = '2px solid var(--accent)';
                item.innerHTML = `
                    <div style="font-weight:700;">${s.label}</div>
                    <div style="font-size:10px; color:var(--muted);">${new Date(s.occurred_at).toLocaleString()}</div>
                    ${s.note ? `<div style="font-style:italic; margin-top:2px;">${s.note}</div>` : ''}
                `;
                historyDiv.appendChild(item);
            });
        }

        // Map & Path
        mapDetail.invalidateSize();
        detailMarkers.forEach(m => mapDetail.removeLayer(m));
        detailMarkers = [];
        if (detailPathLine) mapDetail.removeLayer(detailPathLine);

        const latLngs = [];
        if (d.locations && d.locations.length > 0) {
            d.locations.forEach(l => latLngs.push([l.lat, l.lng]));
            detailPathLine = L.polyline(latLngs, {color: 'var(--accent)', weight: 4}).addTo(mapDetail);
            document.getElementById('det-path-info').innerText = latLngs.length + ' points enregistrés sur le parcours.';
        } else {
            document.getElementById('det-path-info').innerText = 'Aucun point GPS enregistré pour cette livraison.';
        }

        // Markers for start/end
        if (d.order.sender_lat) {
            const startM = L.marker([d.order.sender_lat, d.order.sender_lng], {title: 'Départ'}).addTo(mapDetail).bindPopup('Expéditeur: ' + d.order.sender_name);
            detailMarkers.push(startM);
        }
        if (d.order.recipient_lat) {
            const endM = L.marker([d.order.recipient_lat, d.order.recipient_lng], {title: 'Arrivée'}).addTo(mapDetail).bindPopup('Destinataire: ' + d.order.recipient_name);
            detailMarkers.push(endM);
        }

        if (latLngs.length > 0 || detailMarkers.length > 0) {
            const group = new L.featureGroup([...(detailPathLine ? [detailPathLine] : []), ...detailMarkers]);
            mapDetail.fitBounds(group.getBounds(), {padding: [30, 30]});
        } else {
            mapDetail.setView(center, 12);
        }
    }

    function getImageUrl(path) {
        if (!path) return '';
        if (path.startsWith('http')) return path;
        if (path.startsWith('data:image')) return path;
        const cleanPath = path.replace(/\\/g, '/').replace(/^\/+/, '');
        return '/storage/' + cleanPath;
    }
</script>

</body>
</html>
