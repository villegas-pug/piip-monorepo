package pe.gob.midagri.piip.documentos.dto;

import pe.gob.midagri.piip.documentos.entity.ClasificacionDocumento;

public record ExpedienteInstitucionalDetail(
        Long id, String codigo, String asunto, String moduloOrigen, String referenciaCasoUso,
        Long unidadId, ClasificacionDocumento clasificacion, long version) { }
