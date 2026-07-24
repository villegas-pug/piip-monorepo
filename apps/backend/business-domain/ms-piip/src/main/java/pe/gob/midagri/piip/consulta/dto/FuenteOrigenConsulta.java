package pe.gob.midagri.piip.consulta.dto;

import pe.gob.midagri.piip.consulta.dto.TipoSolucionConsulta;

/**
 * Duplicado del enum canónico para evitar exponer la entidad
 * JPA del módulo {@code portafolio} y mantener la independencia
 * de la API pública de la consulta institucional.
 */
public enum FuenteOrigenConsulta {
    FICHA_INICIATIVA,
    CONCURSO_INTERNO,
    INNOVACION_ABIERTA,
    PROPUESTA_JEFATURA,
    OTROS
}
