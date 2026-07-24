import { PLATFORM_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { AuthService, KEYCLOAK_FACTORY } from './auth.service';

describe('AuthService', () => {
  beforeEach(() => {
    TestBed.resetTestingModule();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    TestBed.resetTestingModule();
  });

  it('redirige al ambiente Keycloak configurado sin administrar credenciales ni tema', async () => {
    const login = vi.fn(async () => undefined);
    const adapter = {
      authenticated: false,
      init: vi.fn(async () => false),
      login,
      logout: vi.fn(async () => undefined),
      updateToken: vi.fn(async () => true),
      clearToken: vi.fn(),
      token: undefined,
      onTokenExpired: undefined
    };
    // Clase simulada que actúa como el constructor `Keycloak` de keycloak-js.
    // Se usa una clase real porque el servicio invoca `new KeycloakAdapter(...)`
    // y `vi.fn()` en vitest 4 no soporta `new`.
    const KeycloakAdapter = vi.fn(function () {
      return adapter;
    });
    const origin = window.location.origin;
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => new Response(JSON.stringify({
        issuer: 'https://sso.sintetico.example/realms/piip',
        clientId: 'piip-web-sintetico',
        redirectUri: `${origin}/institucional`,
        postLogoutRedirectUri: `${origin}/consulta-publica`,
        scopes: ['openid', 'profile'],
        ogtiThemeConfirmationReference: 'OGTI-PRUEBA-001'
      }), { status: 200 }))
    );

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        { provide: PLATFORM_ID, useValue: 'browser' },
        { provide: KEYCLOAK_FACTORY, useValue: async () => KeycloakAdapter }
      ]
    });

    await TestBed.inject(AuthService).login();

    expect(KeycloakAdapter).toHaveBeenCalledWith({
      clientId: 'piip-web-sintetico',
      oidcProvider: 'https://sso.sintetico.example/realms/piip/.well-known/openid-configuration'
    });
    expect(login).toHaveBeenCalledWith({
      redirectUri: `${window.location.origin}/institucional`,
      scope: 'openid profile'
    });
  });
});
