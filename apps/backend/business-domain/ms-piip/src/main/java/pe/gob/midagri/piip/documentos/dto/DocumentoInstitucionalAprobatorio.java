package pe.gob.midagri.piip.documentos.dto;

/** Evidencia mínima para que otro módulo compruebe una aprobación institucional. */
public record DocumentoInstitucionalAprobatorio(Long documentoId, Long expedienteId, boolean valido) {}
