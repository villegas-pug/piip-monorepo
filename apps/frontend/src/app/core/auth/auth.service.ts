import { Injectable, InjectionToken, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import type Keycloak from 'keycloak-js';

import { RuntimeConfig, RuntimeConfigError, loadRuntimeConfig } from '../config/runtime-config';

export type KeycloakFactory = () => Promise<typeof Keycloak>;

/** Punto sustituible para pruebas; la carga real permanece diferida para SSR. */
export const KEYCLOAK_FACTORY = new InjectionToken<KeycloakFactory>('KEYCLOAK_FACTORY', {
  factory: () => () => import('keycloak-js').then(({ default: KeycloakAdapter }) => KeycloakAdapter)
});

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly keycloakFactory = inject(KEYCLOAK_FACTORY);
  private keycloak?: Keycloak;
  private initialization?: Promise<boolean>;
  private config?: RuntimeConfig;

  async initialize(): Promise<boolean> {
    if (!isPlatformBrowser(this.platformId)) {
      return false;
    }

    this.initialization ??= this.initializeBrowser();
    return this.initialization;
  }

  async isAuthenticated(): Promise<boolean> {
    await this.initialize();
    return this.keycloak?.authenticated === true;
  }

  async login(redirectUri?: string): Promise<void> {
    await this.initialize();
    if (!this.keycloak || !this.config) {
      throw new RuntimeConfigError('La autenticación no está disponible en este entorno.');
    }
    await this.keycloak.login({ redirectUri: redirectUri ?? this.config.redirectUri, scope: this.config.scopes.join(' ') });
  }

  async logout(): Promise<void> {
    await this.initialize();
    if (this.keycloak && this.config) {
      await this.keycloak.logout({ redirectUri: this.config.postLogoutRedirectUri });
    }
  }

  async getValidAccessToken(): Promise<string | undefined> {
    await this.initialize();
    if (!this.keycloak?.authenticated) {
      return undefined;
    }

    try {
      await this.keycloak.updateToken(30);
      return this.keycloak.token;
    } catch {
      this.keycloak.clearToken();
      return undefined;
    }
  }

  private async initializeBrowser(): Promise<boolean> {
    const config = await loadRuntimeConfig();
    this.assertLocalRedirect(config.redirectUri, 'redirectUri');
    this.assertLocalRedirect(config.postLogoutRedirectUri, 'postLogoutRedirectUri');

    // La carga diferida evita ejecutar keycloak-js durante el renderizado SSR.
    const KeycloakAdapter = await this.keycloakFactory();
    const keycloak = new KeycloakAdapter({
      clientId: config.clientId,
      oidcProvider: `${config.issuer}/.well-known/openid-configuration`
    });

    this.keycloak = keycloak;
    this.config = config;
    keycloak.onTokenExpired = () => void this.getValidAccessToken();

    return keycloak.init({
      onLoad: 'check-sso',
      flow: 'standard',
      pkceMethod: 'S256',
      responseMode: 'query',
      redirectUri: config.redirectUri,
      scope: config.scopes.join(' '),
      checkLoginIframe: false
    });
  }

  private assertLocalRedirect(redirectUri: string, property: string): void {
    if (new URL(redirectUri).origin !== window.location.origin) {
      throw new RuntimeConfigError(`${property} debe pertenecer al origen de la aplicación.`);
    }
  }
}
