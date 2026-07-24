package pe.gob.midagri.piip.documentos.dto;

import pe.gob.midagri.piip.documentos.entity.ClasificacionDocumento;

/**
 * Comando interno de validación de clasificación. Se construye a
 * partir de la solicitud HTTP y del contexto autorizado.
 */
public record ValidarClasificacionCommand(ClasificacionDocumento clasificacion) {
}
