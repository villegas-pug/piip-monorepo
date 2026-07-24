// Punto unico de reexportacion para tipos del feature de proyectos del
// portafolio (US3). Reutiliza los catalogos canonicos del recorrido de
// registro (US1) para no duplicar constantes ni enums (principio DRY
// constitucional) y exponerlos como un unico barrel estable.
//
// Referencia contractual: specs/001-gestionar-portafolio-innovacion/contracts/openapi/piip-api.yaml
// y Constitucion 5.0.0 (seccion "Catalogos e invariantes canonicos del portafolio").
// Ningun consumidor debe tomar tipos directamente de registro/api/types
// cuando los necesite para proyectos: debe importarlos desde aqui.

export * from '../../../registro/api/types/common.types';
export * from './proyectos.types';