package pe.gob.midagri.piip.reportes.entity;

/**
 * Tipos de reporte institucional definidos en el DDL 017
 * (CHECK {@code CK_RE_TIPO} sobre
 * {@code REPORTE_INSTITUCIONAL.TIPO}). El reporte
 * semestral usa los cortes canónicos 30/06 y 31/12 de
 * BR-013; el extraordinario exige solicitud documentada y
 * aprobación de la Oficina de Modernización conforme a
 * BR-120.
 */
public enum TipoReporte {
    SEMESTRAL,
    EXTRAORDINARIO
}
