package pe.gob.midagri.piip.documentos.dto;

import pe.gob.midagri.piip.documentos.entity.ClasificacionDocumento;
import pe.gob.midagri.piip.documentos.entity.ContextoTipoDocumento;

/** DTO del catálogo documental, sin entidad ni detalles de almacenamiento. */
public record AptitudDocumental(
        Integer tipoDocumentoId, String nombre, ContextoTipoDocumento contexto,
        ClasificacionDocumento clasificacionDefecto, boolean obligatorio, boolean activo) { }
