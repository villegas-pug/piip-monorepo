package pe.gob.midagri.piip.documentos.dto;

import pe.gob.midagri.piip.documentos.entity.ClasificacionDocumento;

/** Comando interno: módulo y unidad se resuelven en servidor, nunca desde HTTP. */
public record CreateInstitutionalFileCommand(
        String asunto, String referenciaCasoUso, ClasificacionDocumento clasificacion) { }
