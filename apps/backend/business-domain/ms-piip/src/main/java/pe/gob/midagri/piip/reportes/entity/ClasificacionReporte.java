package pe.gob.midagri.piip.reportes.entity;

/**
 * Clasificación de privacidad del reporte institucional
 * conforme a BR-126 y al DDL 017. Se asigna INTERNO por
 * defecto; RESTRINGIDO cuando el reporte contiene cualquier
 * dato con esa clasificación. La consulta pública nunca
 * expone reportes.
 */
public enum ClasificacionReporte {
    INTERNO,
    RESTRINGIDO
}
