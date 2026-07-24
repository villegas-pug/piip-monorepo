package pe.gob.midagri.piip.consulta.dto;

/**
 * Duplicado del enum de clasificación documental para
 * mantener la independencia de la API pública y evitar exponer
 * la entidad JPA de {@code documentos}.
 */
public enum ClasificacionDocumentoConsulta { PUBLICO, INTERNO, RESTRINGIDO }
