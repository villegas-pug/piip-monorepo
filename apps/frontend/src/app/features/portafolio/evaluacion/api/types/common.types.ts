// Punto único de reexportación para tipos compartidos por los features de
// evaluación y decisión de portafolio. Reutiliza los catálogos canónicos del
// recorrido de registro (US1) para no duplicar constantes ni enums
// (principio DRY constitucional) y exponerlos como un único barrel estable.
//
// Referencia contractual: specs/001-gestionar-portafolio-innovacion/contracts/openapi/piip-api.yaml
// y Constitución 5.0.0 (sección "Catálogos e invariantes canónicos del portafolio").
// Ningún consumidor debe tomar tipos directamente de registro/api/types cuando
// los necesite para evaluación o decisión: debe importarlos desde aquí.

export * from '../../../registro/api/types/common.types';
