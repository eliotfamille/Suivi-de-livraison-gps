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
        ]);

        $user = User::create($data);
        $user->assignRole('client'); // rôle par défaut

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

        return response()->json([
            'user' => $this->userResource($user),
        ]);
    }

    private function userResource(User $user): array
    {
        return [
            'id'     => $user->id,
            'name'   => $user->name,
            'email'  => $user->email,
            'phone'  => $user->phone,
            'avatar' => $user->avatar,
            'roles'  => $user->getRoleNames(),
            'driver' => $user->driver ? [
                'id'            => $user->driver->id,
                'status'        => $user->driver->status,
                'vehicle_type'  => $user->driver->vehicle_type,
                'vehicle_plate' => $user->driver->vehicle_plate,
                'rating'        => $user->driver->rating,
            ] : null,
        ];
    }
}
