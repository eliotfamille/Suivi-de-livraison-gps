<?php

namespace App\Models;

use Database\Factories\UserFactory;
use Illuminate\Database\Eloquent\Attributes\Fillable;
use Illuminate\Database\Eloquent\Attributes\Hidden;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Relations\HasOne;
use Illuminate\Foundation\Auth\User as Authenticatable;
use Illuminate\Notifications\Notifiable;
use Laravel\Sanctum\HasApiTokens;
use Spatie\Permission\Traits\HasRoles;

#[Fillable(['name', 'email', 'password', 'phone', 'avatar'])]
#[Hidden(['password', 'remember_token'])]
class User extends Authenticatable
{
    /** @use HasFactory<UserFactory> */
    use HasApiTokens, HasFactory, Notifiable, HasRoles;

    /**
     * Obtenir les attributs à caster.
     *
     * @return array<string, string>
     */
    protected function casts(): array
    {
        return [
            'email_verified_at' => 'datetime',
            'password' => 'hashed',
        ];
    }

    /**
     * Relation un-à-un avec le livreur (Driver).
     */
    public function driver(): HasOne
    {
        return $this->hasOne(Driver::class);
    }

    /**
     * Vérifier si l'utilisateur possède le rôle de client.
     */
    public function isClient(): bool
    {
        return $this->hasRole('client');
    }

    /**
     * Vérifier si l'utilisateur possède le rôle d'administrateur.
     */
    public function isAdmin(): bool
    {
        return $this->hasRole('admin');
    }

    /**
     * Vérifier si l'utilisateur possède le rôle de livreur.
     */
    public function isDriver(): bool
    {
        return $this->hasRole('driver');
    }
}