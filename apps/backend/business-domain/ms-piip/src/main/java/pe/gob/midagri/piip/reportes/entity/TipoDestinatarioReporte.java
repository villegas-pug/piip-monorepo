package pe.gob.midagri.piip.reportes.entity;

/**
 * Tipos de destinatario válidos para un reporte
 * aprobado conforme a BR-125 y al DDL 017 (CHECK
 * {@code CK_RD_TIPO_DESTINATARIO}). La
 * autorización efectiva de la operación sensible
 * sigue siendo potestad de la Oficina de
 * Modernización; este enum solo clasifica
 * formalmente al destinatario.
 */
public enum TipoDestinatarioReporte {
    AUTORIDAD_MIDAGRI,
    OFICINA_MODERNIZACION,
    PCM_SGP
}
