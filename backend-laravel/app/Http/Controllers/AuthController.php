<?php

namespace App\Http\Controllers;

use App\Http\Controllers\Controller;
use App\Models\User;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Hash;
use Illuminate\Validation\ValidationException;

class AuthController extends Controller
{
    public function register(Request $request): JsonResponse
    {
        $data = $request->validate([
            'name'     => 'required|string|max:255',
            'email'    => 'required|email|unique:users',
            'phone'    => 'nullable|string|max:20',
            'password' => 'required|min:8|confirmed',
            'role'     => 'nullable|string|in:client,driver',
            'vehicle_type'  => 'nullable|string',
            'vehicle_model' => 'nullable|string',
            'vehicle_plate' => 'nullable|string',
        ]);

        $role = $request->role ?? 'client';

        $user = \DB::transaction(function () use ($data, $role) {
            $user = User::create([
                'name'     => $data['name'],
                'email'    => $data['email'],
                'phone'    => $data['phone'] ?? null,
                'password' => $data['password'],
            ]);

            $user->assignRole($role);

            // Si c'est un livreur, on crée son profil Driver avec les infos fournies
            if ($role === 'driver') {
                $user->driver()->create([
                    'status'        => 'available',
                    'vehicle_type'  => $data['vehicle_type'] ?? 'motorcycle',
                    'vehicle_model' => $data['vehicle_model'] ?? null,
                    'vehicle_plate' => $data['vehicle_plate'] ?? null,
                ]);
            }

            return $user;
        });

        $token = $user->createToken('mobile-app')->plainTextToken;

        return response()->json([
            'message' => 'Compte créé avec succès',
            'user'    => $this->userResource($user),
            'token'   => $token,
        ], 201);
    }

    public function login(Request $request): JsonResponse
    {
        $request->validate([
            'email'    => 'required|email',
            'password' => 'required',
        ]);

        $user = User::where('email', $request->email)->first();

        if (! $user || ! Hash::check($request->password, $user->password)) {
            throw ValidationException::withMessages([
                'email' => ['Identifiants incorrects.'],
            ]);
        }

        // Révoquer les anciens tokens et créer un nouveau
        $user->tokens()->delete();
        $token = $user->createToken('mobile-app')->plainTextToken;

        return response()->json([
            'message' => 'Connexion réussie',
            'user'    => $this->userResource($user),
            'token'   => $token,
        ]);
    }

    public function logout(Request $request): JsonResponse
    {
        $request->user()->currentAccessToken()->delete();

        return response()->json(['message' => 'Déconnecté avec succès']);
    }

    public function me(Request $request): JsonResponse
    {
        $user = $request->user()->load('driver');

        return response()->json($this->userResource($user));
    }

    public function search(Request $request): JsonResponse
    {
        $query = $request->query('search');
        if (!$query) return response()->json([]);

        $users = User::where('name', 'like', "%$query%")
            ->orWhere('email', 'like', "%$query%")
            ->limit(10)
            ->get();

        return response()->json($users->map(fn($u) => $this->userResource($u)));
    }

    public function updateProfile(Request $request): JsonResponse
    {
        $user = $request->user();
        $data = $request->validate([
            'name'         => 'nullable|string|max:255',
            'phone'        => 'nullable|string|max:20',
            'domicile'     => 'nullable|string|max:255',
            'domicile_lat' => 'nullable|numeric',
            'domicile_lng' => 'nullable|numeric',
            'bureau'       => 'nullable|string|max:255',
            'bureau_lat'   => 'nullable|numeric',
            'bureau_lng'   => 'nullable|numeric',
            'vehicle_type'  => 'nullable|string',
            'vehicle_model' => 'nullable|string',
            'vehicle_plate' => 'nullable|string',
            'avatar'       => 'nullable|image|max:2048', // Image upload support
        ]);

        if ($request->hasFile('avatar')) {
            $path = $request->file('avatar')->store('avatars', 'public');
            $data['avatar'] = $path; // On stocke uniquement le chemin relatif
        }

        $user->update($data);

        // Si l'utilisateur est un livreur, on met aussi à jour son profil Driver
        if ($user->hasRole('driver')) {
            $driverData = array_filter([
                'vehicle_type'  => $request->vehicle_type,
                'vehicle_model' => $request->vehicle_model,
                'vehicle_plate' => $request->vehicle_plate,
            ]);

            if (!empty($driverData) || !$user->driver) {
                $user->driver()->updateOrCreate(
                    ['user_id' => $user->id],
                    array_merge(['status' => 'available'], $driverData)
                );
            }
        }

        return response()->json($this->userResource($user->load('driver')));
    }

    private function userResource(User $user): array
    {
        $avatarUrl = null;
        if ($user->avatar) {
            // Si c'est déjà une URL complète (ex: via un seeder), on la garde
            if (filter_var($user->avatar, FILTER_VALIDATE_URL)) {
                $avatarUrl = $user->avatar;
            } else {
                // Sinon on génère l'URL complète basée sur l'hôte de la requête actuelle
                $avatarUrl = url('storage/' . $user->avatar);
            }
        }

        return [
            'id'           => $user->id,
            'name'         => $user->name,
            'email'        => $user->email,
            'phone'        => $user->phone,
            'avatar'       => $avatarUrl,
            'domicile'     => $user->domicile,
            'domicile_lat' => $user->domicile_lat,
            'domicile_lng' => $user->domicile_lng,
            'bureau'       => $user->bureau,
            'bureau_lat'   => $user->bureau_lat,
            'bureau_lng'   => $user->bureau_lng,
            'roles'        => $user->getRoleNames(),
            'role'         => $user->getRoleNames()->first(),
            'driver'       => $user->driver ? [
                'id'            => $user->driver->id,
                'status'        => $user->driver->status,
                'vehicle_type'  => $user->driver->vehicle_type,
                'vehicle_model' => $user->driver->vehicle_model,
                'vehicle_plate' => $user->driver->vehicle_plate,
                'rating'        => (float) $user->driver->rating,
                'rating_count'  => $user->driver->rating_count,
            ] : null,
        ];
    }
}
