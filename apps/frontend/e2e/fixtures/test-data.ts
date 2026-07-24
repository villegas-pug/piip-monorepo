/**
 * Datos de validación 100 % sintéticos para las pruebas E2E de PIIP.
 *
 * Alineados con la sección "Datos de validación" de
 * specs/001-gestionar-portafolio-innovacion/quickstart.md:
 * - unidades A y B sin herencia de permisos;
 * - un usuario por cada perfil canónico;
 * - asignaciones vigente, vencida, revocada y de suplencia;
 * - referencias PEI/POI aprobadas para pruebas;
 * - tamaños de archivo alrededor del límite inclusivo de 100 MB.
 *
 * Ningún valor corresponde a una persona real: los nombres son genéricos, los
 * correos usan el dominio reservado `example.invalid` (RFC 2606) y los `sub`
 * son identificadores ficticios de prueba. No se configura ningún test double
 * ni estado antimalware: ese control es responsabilidad de OGTI fuera de PIIP.
 */

/** Perfiles canónicos de PIIP según la Constitución. */
export const PERFILES_CANONICOS = [
  'GlobalAdmin',
  'UnidadAdmin',
  'Responsable',
  'Evaluador',
  'Autoridad',
  'Consulta',
] as const;

export type PerfilCanonico = (typeof PERFILES_CANONICOS)[number];

/** Unidad organizacional sintética. */
export interface UnidadPrueba {
  readonly codigo: string;
  readonly nombre: string;
}

/**
 * Unidades A y B sin herencia de permisos: una asignación sobre la unidad A
 * nunca autoriza operaciones en la unidad B ni en unidades descendientes.
 */
export const UNIDADES = {
  A: { codigo: 'UNIDAD-A', nombre: 'Unidad Sintética A' },
  B: { codigo: 'UNIDAD-B', nombre: 'Unidad Sintética B' },
} as const satisfies Record<string, UnidadPrueba>;

/** Usuario sintético asociado a un perfil canónico y una unidad. */
export interface UsuarioPrueba {
  readonly username: string;
  /** Identificador Keycloak ficticio; nunca un `sub` real. */
  readonly sub: string;
  readonly nombreCompleto: string;
  readonly email: string;
  readonly perfil: PerfilCanonico;
  readonly unidad: string;
}

/** Un usuario por cada perfil canónico, distribuido entre las unidades A y B. */
export const USUARIOS = {
  globalAdmin: {
    username: 'prueba.globaladmin',
    sub: 'sub-sintetico-globaladmin-0001',
    nombreCompleto: 'Usuario Prueba GlobalAdmin',
    email: 'prueba.globaladmin@example.invalid',
    perfil: 'GlobalAdmin',
    unidad: UNIDADES.A.codigo,
  },
  unidadAdmin: {
    username: 'prueba.unidadadmin',
    sub: 'sub-sintetico-unidadadmin-0002',
    nombreCompleto: 'Usuario Prueba UnidadAdmin',
    email: 'prueba.unidadadmin@example.invalid',
    perfil: 'UnidadAdmin',
    unidad: UNIDADES.A.codigo,
  },
  responsable: {
    username: 'prueba.responsable',
    sub: 'sub-sintetico-responsable-0003',
    nombreCompleto: 'Usuario Prueba Responsable',
    email: 'prueba.responsable@example.invalid',
    perfil: 'Responsable',
    unidad: UNIDADES.A.codigo,
  },
  evaluador: {
    username: 'prueba.evaluador',
    sub: 'sub-sintetico-evaluador-0004',
    nombreCompleto: 'Usuario Prueba Evaluador',
    email: 'prueba.evaluador@example.invalid',
    perfil: 'Evaluador',
    unidad: UNIDADES.B.codigo,
  },
  autoridad: {
    username: 'prueba.autoridad',
    sub: 'sub-sintetico-autoridad-0005',
    nombreCompleto: 'Usuario Prueba Autoridad',
    email: 'prueba.autoridad@example.invalid',
    perfil: 'Autoridad',
    unidad: UNIDADES.B.codigo,
  },
  consulta: {
    username: 'prueba.consulta',
    sub: 'sub-sintetico-consulta-0006',
    nombreCompleto: 'Usuario Prueba Consulta',
    email: 'prueba.consulta@example.invalid',
    perfil: 'Consulta',
    unidad: UNIDADES.B.codigo,
  },
} as const satisfies Record<string, UsuarioPrueba>;

/** Situación de vigencia de una asignación usuario-perfil-unidad. */
export type EstadoAsignacionPrueba = 'VIGENTE' | 'VENCIDA' | 'REVOCADA' | 'SUPLENCIA';

/** Asignación sintética para probar la autorización efectiva por unidad. */
export interface AsignacionPrueba {
  readonly id: string;
  readonly usuario: string;
  readonly perfil: PerfilCanonico;
  readonly unidad: string;
  readonly estado: EstadoAsignacionPrueba;
  /** Fechas ISO 8601 (AAAA-MM-DD); `fin: null` indica vigencia abierta. */
  readonly inicio: string;
  readonly fin: string | null;
}

/** Una asignación por cada situación exigida: vigente, vencida, revocada y suplencia. */
export const ASIGNACIONES: readonly AsignacionPrueba[] = [
  {
    id: 'asig-vigente-001',
    usuario: USUARIOS.responsable.username,
    perfil: 'Responsable',
    unidad: UNIDADES.A.codigo,
    estado: 'VIGENTE',
    inicio: '2026-01-01',
    fin: null,
  },
  {
    id: 'asig-vencida-001',
    usuario: USUARIOS.evaluador.username,
    perfil: 'Evaluador',
    unidad: UNIDADES.B.codigo,
    estado: 'VENCIDA',
    inicio: '2025-01-01',
    fin: '2025-12-31',
  },
  {
    id: 'asig-revocada-001',
    usuario: USUARIOS.unidadAdmin.username,
    perfil: 'UnidadAdmin',
    unidad: UNIDADES.A.codigo,
    estado: 'REVOCADA',
    inicio: '2025-06-01',
    fin: '2025-09-30',
  },
  {
    id: 'asig-suplencia-001',
    usuario: USUARIOS.autoridad.username,
    perfil: 'Autoridad',
    unidad: UNIDADES.B.codigo,
    estado: 'SUPLENCIA',
    inicio: '2026-07-01',
    fin: '2026-07-31',
  },
];

/** Referencia sintética de planeamiento (objetivo PEI o actividad POI). */
export interface ReferenciaPlaneamientoPrueba {
  readonly tipo: 'PEI' | 'POI';
  readonly codigo: string;
  readonly nombre: string;
  readonly version: number;
  readonly estado: 'APROBADO';
}

/** Referencias PEI/POI aprobadas para pruebas; no son valores institucionales reales. */
export const PLANEAMIENTO = {
  pei: {
    tipo: 'PEI',
    codigo: 'PEI-OEI-001',
    nombre: 'Objetivo estratégico institucional sintético 1',
    version: 1,
    estado: 'APROBADO',
  },
  poi: {
    tipo: 'POI',
    codigo: 'POI-AO-001',
    nombre: 'Actividad operativa sintética 1',
    version: 1,
    estado: 'APROBADO',
  },
} as const satisfies Record<string, ReferenciaPlaneamientoPrueba>;

/**
 * Tamaños de archivo (en bytes) alrededor del límite inclusivo de 100 MB,
 * según el escenario 6 del quickstart.
 */
export const TAMANIOS_ARCHIVO = {
  /** 100 MB - 1 byte: aceptado si el MIME es válido; conserva BLOB y SHA-256. */
  aceptadoBajoLimite: 104857599,
  /** Exactamente 100 MB: aceptado; el límite es inclusivo. */
  maximoInclusivo: 104857600,
  /** 100 MB + 1 byte: rechazado con `413 DOCUMENT_TOO_LARGE`, sin metadato formalizado. */
  excedido: 104857601,
} as const;

/** Formatos de documento aceptados inicialmente por PIIP. */
export const FORMATOS_ARCHIVO = ['PDF', 'OOXML', 'JPEG', 'PNG'] as const;

export type FormatoArchivo = (typeof FORMATOS_ARCHIVO)[number];

/** Tipos MIME representativos de cada formato aceptado. */
export const MIME_TYPES_PERMITIDOS: Readonly<Record<FormatoArchivo, string>> = {
  PDF: 'application/pdf',
  OOXML: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  JPEG: 'image/jpeg',
  PNG: 'image/png',
};
