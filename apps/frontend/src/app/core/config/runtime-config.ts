/** Configuración OIDC suministrada por el ambiente, nunca por el código compilado. */
export interface RuntimeConfig {
  readonly issuer: string;
  readonly clientId: string;
  readonly redirectUri: string;
  readonly postLogoutRedirectUri: string;
  readonly scopes: readonly string[];
  /** Referencia verificable de la confirmación externa de OGTI sobre el tema Keycloak. */
  readonly ogtiThemeConfirmationReference: string;
}

export class RuntimeConfigError extends Error {
  override readonly name = 'RuntimeConfigError';
}

let loadedConfig: Promise<RuntimeConfig> | undefined;

/** Carga la configuración pública del ambiente antes de inicializar el adaptador OIDC. */
export function loadRuntimeConfig(): Promise<RuntimeConfig> {
  loadedConfig ??= fetch('/config.json', { cache: 'no-store' })
    .then(async (response) => {
      if (!response.ok) {
        throw new RuntimeConfigError('No se pudo cargar la configuración de autenticación.');
      }

      return validateRuntimeConfig(await response.json());
    })
    .catch((error: unknown) => {
      loadedConfig = undefined;
      if (error instanceof RuntimeConfigError) {
        throw error;
      }
      throw new RuntimeConfigError('La configuración de autenticación no es válida.');
    });

  return loadedConfig;
}

export function validateRuntimeConfig(value: unknown): RuntimeConfig {
  if (!isRecord(value)) {
    throw new RuntimeConfigError('La configuración de autenticación debe ser un objeto.');
  }

  const issuer = requiredUrl(value, 'issuer');
  const clientId = requiredText(value, 'clientId');
  const redirectUri = requiredUrl(value, 'redirectUri');
  const postLogoutRedirectUri = requiredUrl(value, 'postLogoutRedirectUri');
  const ogtiThemeConfirmationReference = requiredText(value, 'ogtiThemeConfirmationReference');
  const scopes = requiredScopes(value['scopes']);

  return Object.freeze({
    issuer,
    clientId,
    redirectUri,
    postLogoutRedirectUri,
    scopes,
    ogtiThemeConfirmationReference
  });
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}

function requiredText(value: Record<string, unknown>, property: string): string {
  const candidate = value[property];
  if (typeof candidate !== 'string' || candidate.trim().length === 0) {
    throw new RuntimeConfigError(`Falta la configuración obligatoria: ${property}.`);
  }
  return candidate.trim();
}

function requiredUrl(value: Record<string, unknown>, property: string): string {
  const candidate = requiredText(value, property);
  try {
    const url = new URL(candidate);
    if (url.protocol !== 'https:' && url.protocol !== 'http:') {
      throw new Error('Protocolo no permitido');
    }
    return url.toString().replace(/\/$/, '');
  } catch {
    throw new RuntimeConfigError(`La configuración ${property} debe ser una URL HTTP(S) absoluta.`);
  }
}

function requiredScopes(value: unknown): readonly string[] {
  if (!Array.isArray(value) || value.length === 0 || value.some((scope) => typeof scope !== 'string' || scope.trim().length === 0)) {
    throw new RuntimeConfigError('Falta la configuración obligatoria: scopes.');
  }

  return Object.freeze([...new Set(value.map((scope) => scope.trim()))]);
}
