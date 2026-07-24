package pe.gob.midagri.piip.documentos.service;

import java.util.List;

import pe.gob.midagri.piip.documentos.dto.ClasificacionHistDetalle;
import pe.gob.midagri.piip.documentos.dto.DocumentoAuthorizedContext;
import pe.gob.midagri.piip.documentos.dto.ReclasificarDocumentoCommand;
import pe.gob.midagri.piip.documentos.dto.ReclasificacionDocumentoResult;
import pe.gob.midagri.piip.documentos.dto.ValidacionClasificacionResult;
import pe.gob.midagri.piip.documentos.dto.ValidarClasificacionCommand;

/**
 * Autoridad única para validación y reclasificación de
 * documentos del módulo {@code documentos}. Implementa la regla
 * restrictiva constitucional: la reclasificación nunca puede ser
 * menos restrictiva que la clasificación actual validada. Toda
 * transición se registra con fecha del servidor y se audita con
 * la asignación efectiva.
 */
public interface ClasificacionDocumentoService {

    /**
     * Valida la clasificación inicial propuesta por el Responsable
     * y la fija en el documento. La fecha del servidor se aplica
     * desde la base de datos. Devuelve el ETag optimista calculado
     * sobre la versión reclasificada.
     */
    ValidacionClasificacionResult validarClasificacion(
            DocumentoAuthorizedContext contexto, Long documentoId,
            String ifMatch, ValidarClasificacionCommand comando);

    /**
     * Aplica una reclasificación con la decisión formal de la
     * Autoridad. La transición debe cumplir la regla restrictiva
     * (no puede pasar a una clasificación menos restrictiva) y se
     * registra en {@code DOCUMENTO_CLASIFICACION_HIST} con su
     * resultado. Si la reclasificación es más restrictiva, las
     * proyecciones públicas dejarán de incluir la versión en
     * futuras consultas.
     */
    ReclasificacionDocumentoResult reclasificar(
            DocumentoAuthorizedContext contexto, Long documentoId,
            String ifMatch, ReclasificarDocumentoCommand comando);

    /**
     * Devuelve el historial append-only de reclasificaciones del
     * documento. La respuesta está ordenada cronológicamente y
     * nunca incluye datos personales innecesarios.
     */
    List<ClasificacionHistDetalle> listarHistorial(DocumentoAuthorizedContext contexto, Long documentoId);
}
