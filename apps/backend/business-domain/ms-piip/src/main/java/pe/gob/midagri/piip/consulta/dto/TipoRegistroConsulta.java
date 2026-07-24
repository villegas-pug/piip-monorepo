package pe.gob.midagri.piip.consulta.dto;

/**
 * Tipo de registro del portafolio expuesto por la consulta
 * institucional. Duplicado intencional del enum propietario de
 * {@code portafolio} para evitar exponer la entidad JPA
 * correspondiente y mantener la independencia de la API pública.
 */
public enum TipoRegistroConsulta { INICIATIVA, PROYECTO }
