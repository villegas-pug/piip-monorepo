// Punto único de reexportación para tipos de evaluación y subsanación.
// Mantén este barrel estable: cualquier importador debe poder tomar tipos desde
// aquí. NO se importan tipos desde registro/api/types directamente cuando se
// consumen desde evaluacion; se hace a través de ./common.types para
// evitar acoplamientos directos a la estructura interna de registro.

export * from './common.types';
export * from './initiative.types';
export * from './evaluacion.types';
