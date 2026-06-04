<?php

namespace Tests\Feature;

use App\Models\User;
use App\Models\Driver;
use App\Models\Delivery;
use App\Models\Order;
use App\Models\Package;
use Spatie\Permission\Models\Role;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class ExampleTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        Role::firstOrCreate(['name' => 'client']);
        Role::firstOrCreate(['name' => 'driver']);
        Role::firstOrCreate(['name' => 'admin']);
    }

    private function createDeliveryFlow($client = null)
    {
        $client = $client ?? User::create(['name'=>'C', 'email'=>'c@t.com', 'password'=>'p']);
        $package = Package::create(['description'=>'P', 'created_by'=>$client->id]);
        $order = Order::create([
            'client_id' => $client->id, 'package_id' => $package->id,
            'sender_name' => 'S', 'sender_phone' => '1', 'sender_address' => 'A',
            'recipient_name' => 'R', 'recipient_phone' => '2', 'recipient_address' => 'B'
        ]);
        return Delivery::create(['order_id' => $order->id, 'status' => 'pending']);
    }

    public function test_1_homepage_accessible(): void { $this->get('/')->assertStatus(200); }

    public function test_2_register() {
        $this->postJson('/api/auth/register', ['name'=>'J', 'email'=>'j@t.com', 'password'=>'password', 'password_confirmation'=>'password'])->assertStatus(201);
    }

    public function test_3_login() {
        User::create(['name'=>'T', 'email'=>'t@e.com', 'password'=>bcrypt('pass')]);
        $this->postJson('/api/auth/login', ['email'=>'t@e.com', 'password'=>'pass'])->assertStatus(200);
    }

    public function test_4_login_fail() {
        $this->postJson('/api/auth/login', ['email'=>'x@e.com', 'password'=>'x'])->assertStatus(422);
    }

    public function test_5_profile() {
        $u = User::create(['name'=>'T', 'email'=>'t@e.com', 'password'=>'p']);
        $this->actingAs($u)->getJson('/api/auth/me')->assertStatus(200);
    }

    public function test_6_update_profile() {
        $u = User::create(['name'=>'Old', 'email'=>'t@e.com', 'password'=>'p']);
        $this->actingAs($u)->patchJson('/api/auth/profile', ['name'=>'New'])->assertJsonFragment(['name'=>'New']);
    }

    public function test_7_logout() {
        $u = User::create(['name'=>'T', 'email'=>'t@e.com', 'password'=>'p']);
        $token = $u->createToken('test')->plainTextToken;
        $this->withHeader('Authorization', 'Bearer '.$token)->postJson('/api/auth/logout')->assertStatus(200);
    }

    public function test_8_reg_val() { $this->postJson('/api/auth/register', [])->assertStatus(422); }

    public function test_9_dup_email() {
        User::create(['name'=>'T', 'email'=>'t@e.com', 'password'=>'p']);
        $this->postJson('/api/auth/register', ['name'=>'X', 'email'=>'t@e.com', 'password'=>'pass'])->assertStatus(422);
    }

    public function test_10_role_driver() {
        $this->postJson('/api/auth/register', ['name'=>'D', 'email'=>'d@t.com', 'password'=>'password', 'password_confirmation'=>'password', 'role'=>'driver'])->assertStatus(201);
        $this->assertDatabaseHas('drivers', ['user_id' => User::where('email','d@t.com')->first()->id]);
    }

    public function test_11_create_delivery_pricing() {
        $u = User::create(['name'=>'C', 'email'=>'c@t.com', 'password'=>'p']);
        $u->assignRole('client');
        $this->actingAs($u)->postJson('/api/deliveries', [
            'description'=>'P', 'weight_kg'=>1, 'recipient_name'=>'R', 'recipient_phone'=>'1', 'recipient_address'=>'Addr',
            'sender_name'=>'S', 'sender_phone'=>'2', 'sender_address'=>'A', 'zone'=>'Zone Rurale'
        ])->assertStatus(201);
        $this->assertEquals(20.00, Order::first()->delivery_fee);
    }

    public function test_12_driver_accept() {
        $u = User::create(['name'=>'D', 'email'=>'d@t.com', 'password'=>'p']);
        $u->assignRole('driver');
        $d = Driver::create(['user_id'=>$u->id, 'status'=>'available', 'vehicle_type'=>'m']);
        $del = $this->createDeliveryFlow();
        $this->actingAs($u)->postJson("/api/deliveries/{$del->id}/accept")->assertStatus(200);
    }

    public function test_13_update_status() {
        $u = User::create(['name'=>'D', 'email'=>'d@t.com', 'password'=>'p']);
        $u->assignRole('driver');
        $d = Driver::create(['user_id'=>$u->id, 'status'=>'busy', 'vehicle_type'=>'m']);
        $del = $this->createDeliveryFlow();
        $del->update(['driver_id'=>$d->id, 'status'=>'assigned']);
        $this->actingAs($u)->patchJson("/api/deliveries/{$del->id}/status", ['status'=>'in_transit'])->assertStatus(200);
    }

    public function test_14_tracking() {
        $u = User::create(['name'=>'C', 'email'=>'c@t.com', 'password'=>'p']);
        $p = Package::create(['description'=>'P', 'created_by'=>$u->id]);
        $o = Order::create(['client_id'=>$u->id, 'package_id'=>$p->id, 'order_number'=>'TRACK1', 'sender_name'=>'S', 'sender_phone'=>'1', 'sender_address'=>'A', 'recipient_name'=>'R', 'recipient_phone'=>'2', 'recipient_address'=>'B']);

        // On force le rafraîchissement
        $o->refresh();
        $num = $o->order_number;
        Delivery::create(['order_id'=>$o->id, 'status'=>'pending']);

        $this->getJson("/api/tracking/$num")->assertStatus(200);
    }

    public function test_15_rate() {
        $u = User::create(['name'=>'C', 'email'=>'c@t.com', 'password'=>'p']);
        $u->assignRole('client');
        $del = $this->createDeliveryFlow($u);
        $del->update(['status'=>'delivered']);
        $this->actingAs($u)->postJson("/api/deliveries/{$del->id}/rate", ['rating'=>5])->assertStatus(200);
    }

    public function test_16_list_del() {
        $u = User::create(['name'=>'C', 'email'=>'c@t.com', 'password'=>'p']);
        $u->assignRole('client');
        $this->actingAs($u)->getJson('/api/deliveries')->assertStatus(200);
    }

    public function test_17_loc_upd() {
        $u = User::create(['name'=>'D', 'email'=>'d@t.com', 'password'=>'p']);
        $u->assignRole('driver');
        $d = Driver::create(['user_id'=>$u->id, 'status'=>'available', 'vehicle_type'=>'m']);
        $this->actingAs($u)->putJson('/api/driver/location', ['latitude'=>-18, 'longitude'=>47])->assertStatus(200);
    }

    public function test_18_dup_accept() {
        $u = User::create(['name'=>'D', 'email'=>'d@t.com', 'password'=>'p']);
        $u->assignRole('driver');
        $otherU = User::create(['name'=>'O', 'email'=>'o@t.com', 'password'=>'p']);
        $otherD = Driver::create(['user_id'=>$otherU->id, 'status'=>'available', 'vehicle_type'=>'m']);
        $del = $this->createDeliveryFlow();
        $del->update(['driver_id'=>$otherD->id, 'status'=>'assigned']);
        $this->actingAs($u)->postJson("/api/deliveries/{$del->id}/accept")->assertStatus(422);
    }

    public function test_19_req_desc() {
        $u = User::create(['name'=>'C', 'email'=>'c@t.com', 'password'=>'p']);
        $u->assignRole('client');
        $this->actingAs($u)->postJson('/api/deliveries', [])->assertStatus(422);
    }

    public function test_20_stat_hist() {
        $u = User::create(['name'=>'D', 'email'=>'d@t.com', 'password'=>'p']);
        $u->assignRole('driver');
        $d = Driver::create(['user_id'=>$u->id, 'status'=>'busy', 'vehicle_type'=>'m']);
        $del = $this->createDeliveryFlow();
        $del->update(['driver_id'=>$d->id, 'status'=>'assigned']);
        $this->actingAs($u)->patchJson("/api/deliveries/{$del->id}/status", ['status'=>'picked_up']);
        $this->assertDatabaseHas('statuses', ['delivery_id'=>$del->id, 'status'=>'picked_up']);
    }

    public function test_21_dash_data() {
        $a = User::create(['name'=>'A', 'email'=>'a@t.com', 'password'=>'p']);
        $a->assignRole('admin');
        $this->actingAs($a)->getJson('/api/admin/dashboard')->assertStatus(200);
    }

    public function test_22_export() {
        $a = User::create(['name'=>'A', 'email'=>'a@t.com', 'password'=>'p']);
        $a->assignRole('admin');
        $this->actingAs($a)->get('/api/admin/performance/export')->assertStatus(200);
    }

    public function test_23_list_drivers() {
        $a = User::create(['name'=>'A', 'email'=>'a@t.com', 'password'=>'p']);
        $a->assignRole('admin');
        $this->actingAs($a)->getJson('/api/admin/drivers')->assertStatus(200);
    }

    public function test_24_manual_assign() {
        $a = User::create(['name'=>'A', 'email'=>'a@t.com', 'password'=>'p']);
        $a->assignRole('admin');
        $dUser = User::create(['name'=>'D', 'email'=>'d@t.com', 'password'=>'p']);
        $d = Driver::create(['user_id'=>$dUser->id, 'status'=>'available', 'vehicle_type'=>'m']);
        $del = $this->createDeliveryFlow();
        $this->actingAs($a)->postJson("/api/admin/deliveries/{$del->id}/assign", ['driver_id'=>$d->id])->assertStatus(200);
    }

    public function test_25_kpi_avg() {
        $a = User::create(['name'=>'A', 'email'=>'a@t.com', 'password'=>'p']);
        $a->assignRole('admin');
        $del = $this->createDeliveryFlow();
        $del->update(['status'=>'delivered', 'assigned_at'=>now()->subMinutes(30), 'updated_at'=>now()]);
        $res = $this->actingAs($a)->getJson('/api/admin/dashboard');
        $this->assertNotNull($res['stats']['deliveries']['avg_delivery_time_minutes']);
    }

    public function test_26_kpi_rev() {
        $a = User::create(['name'=>'A', 'email'=>'a@t.com', 'password'=>'p']);
        $a->assignRole('admin');
        $del = $this->createDeliveryFlow();
        $del->order->update(['delivery_fee'=>50]);
        $res = $this->actingAs($a)->getJson('/api/admin/dashboard');
        $this->assertEquals(50, $res['stats']['revenue']['total']);
    }

    public function test_27_dash_struct() {
        $a = User::create(['name'=>'A', 'email'=>'a@t.com', 'password'=>'p']);
        $a->assignRole('admin');
        $this->actingAs($a)->getJson('/api/admin/dashboard')->assertJsonStructure(['active_drivers']);
    }

    public function test_28_prot_adm() { $this->getJson('/api/admin/dashboard')->assertStatus(401); }

    public function test_29_webhook_stat() {
        $u = User::create(['name'=>'D', 'email'=>'d@t.com', 'password'=>'p']);
        $u->assignRole('driver');
        $d = Driver::create(['user_id'=>$u->id, 'status'=>'busy', 'vehicle_type'=>'m']);
        $del = $this->createDeliveryFlow();
        $del->update(['driver_id'=>$d->id, 'status'=>'assigned']);
        $this->actingAs($u)->patchJson("/api/deliveries/{$del->id}/status", ['status'=>'delivered'])->assertStatus(200);
    }

    public function test_30_exp_filename() {
        $a = User::create(['name'=>'A', 'email'=>'a@t.com', 'password'=>'p']);
        $a->assignRole('admin');
        $this->actingAs($a)->get('/api/admin/performance/export')->assertHeader('Content-Disposition');
    }

    public function test_31_active_drivers() {
        $a = User::create(['name'=>'A', 'email'=>'a@t.com', 'password'=>'p']);
        $a->assignRole('admin');
        $u = User::create(['name'=>'D', 'email'=>'d@t.com', 'password'=>'p']);
        Driver::create(['user_id'=>$u->id, 'status'=>'busy', 'current_lat'=>-18, 'current_lng'=>47, 'vehicle_type'=>'m']);
        $res = $this->actingAs($a)->getJson('/api/admin/dashboard');
        $this->assertNotEmpty($res['active_drivers']);
    }
}
