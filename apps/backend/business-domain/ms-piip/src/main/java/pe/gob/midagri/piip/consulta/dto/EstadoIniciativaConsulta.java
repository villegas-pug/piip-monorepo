package pe.gob.midagri.piip.consulta.dto;

/**
 * Duplicado del enum de estado del portafolio para mantener la
 * independencia de la API pública y evitar exponer la entidad
 * JPA de {@code portafolio}.
 */
public enum EstadoIniciativaConsulta {
    PRESENTADO,
    NO_ADMISIBLE,
    NO_APLICABLE,
    INICIATIVA_APROBADA,
    INICIATIVA_ARCHIVADA,
    PROYECTO_EJECUCION,
    SUSPENDIDO,
    CANCELADO,
    PRODUCTO_APROBADO,
    PRODUCTO_NO_APROBADO,
    FINALIZADO
}
