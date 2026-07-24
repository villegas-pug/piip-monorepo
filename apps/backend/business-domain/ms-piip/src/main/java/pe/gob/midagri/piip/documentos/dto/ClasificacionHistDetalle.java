package pe.gob.midagri.piip.documentos.dto;

import java.time.LocalDateTime;

import pe.gob.midagri.piip.documentos.entity.ClasificacionDocumento;
import pe.gob.midagri.piip.documentos.entity.ResultadoClasificacion;

/**
 * Detalle inmutable de un evento del historial de
 * reclasificación. Se conserva en respuestas HTTP y en
 * proyecciones institucionales; nunca incluye la contraseña,
 * credenciales ni contenido documental.
 */
public record ClasificacionHistDetalle(
        Long historialId,
        Long documentoId,
        ClasificacionDocumento clasificacionAnterior,
        ClasificacionDocumento clasificacionNueva,
        Long autoridadDecisoraId,
        Long evaluadorRegistradorId,
        Long documentoDecisionId,
        String motivo,
        LocalDateTime fechaCambio,
        ResultadoClasificacion resultado) {
}
