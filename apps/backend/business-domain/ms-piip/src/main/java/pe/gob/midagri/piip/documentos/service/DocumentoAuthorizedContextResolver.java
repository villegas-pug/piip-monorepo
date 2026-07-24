package pe.gob.midagri.piip.documentos.service;

import org.springframework.stereotype.Component;
import pe.gob.midagri.piip.documentos.dto.DocumentoAuthorizedContext;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService;

/** Fábrica de contexto exclusivamente para orquestadores de casos de uso del servidor. */
@Component
public class DocumentoAuthorizedContextResolver {
    private final AutorizacionEfectivaService autorizacionEfectivaService;

    public DocumentoAuthorizedContextResolver(AutorizacionEfectivaService autorizacionEfectivaService) {
        this.autorizacionEfectivaService = autorizacionEfectivaService;
    }

    public DocumentoAuthorizedContext resolver(
            String sub, Long asignacionId, String perfilRequerido, Long unidadRecursoId,
            Long recursoId, String correlacionId) {
        var asignacion = autorizacionEfectivaService.revalidarParaOperacionSensible(
                sub, asignacionId, perfilRequerido, unidadRecursoId);
        return new DocumentoAuthorizedContext(sub, asignacion.usuarioId(), asignacion,
                unidadRecursoId, recursoId, correlacionId);
    }
}
