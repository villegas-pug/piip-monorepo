package pe.gob.midagri.piip.reportes.entity;

/**
 * Estados técnicos del ciclo de vida de un reporte
 * institucional definidos en el DDL 017 (CHECK
 * {@code CK_RE_ESTADO_TECNICO}). No implican
 * transiciones de portafolio; solo trazan el avance
 * del proceso de generación, aprobación y remisión.
 */
public enum EstadoTecnicoReporte {
    INICIADA,
    GENERADA,
    APROBADA,
    FALLIDA
}
