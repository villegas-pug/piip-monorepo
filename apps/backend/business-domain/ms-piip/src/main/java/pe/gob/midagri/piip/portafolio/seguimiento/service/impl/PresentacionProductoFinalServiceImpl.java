package pe.gob.midagri.piip.portafolio.seguimiento.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import pe.gob.midagri.piip.auditoria.service.AuditService;
import pe.gob.midagri.piip.auditoria.service.IdempotencyService;
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;
import pe.gob.midagri.piip.portafolio.entity.RegistroPortafolioEntity;
import pe.gob.midagri.piip.portafolio.entity.TipoRegistro;
import pe.gob.midagri.piip.portafolio.repository.RegistroPortafolioRepository;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.PresentacionProductoFinalRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.PresentacionProductoFinalResponse;
import pe.gob.midagri.piip.portafolio.seguimiento.entity.PresentacionProductoFinalEntity;
import pe.gob.midagri.piip.portafolio.seguimiento.entity.PresentacionProductoFinalEvidenciaEntity;
import pe.gob.midagri.piip.portafolio.seguimiento.repository.PresentacionProductoFinalEvidenciaRepository;
import pe.gob.midagri.piip.portafolio.seguimiento.repository.PresentacionProductoFinalRepository;
import pe.gob.midagri.piip.portafolio.seguimiento.service.AptitudDocumentalService;
import pe.gob.midagri.piip.portafolio.seguimiento.service.PresentacionProductoFinalService;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService;

/** Registra presentaciones finales append-only sin decidir ni transicionar el proyecto. */
@Service
public class PresentacionProductoFinalServiceImpl implements PresentacionProductoFinalService {
    private static final Set<String> TIPOS_FINALES = Set.of("PROTOTIPO_CONCEPTUALIZADO", "SOLUCION_FUNCIONAL");
    private final RegistroPortafolioRepository registroRepository;
    private final PresentacionProductoFinalRepository presentacionRepository;
    private final PresentacionProductoFinalEvidenciaRepository evidenciaRepository;
    private final AptitudDocumentalService aptitudDocumentalService;
    private final AutorizacionEfectivaService autorizacionService;
    private final AuditService auditService;
    private final IdempotencyService idempotencyService;

    public PresentacionProductoFinalServiceImpl(RegistroPortafolioRepository registroRepository,
            PresentacionProductoFinalRepository presentacionRepository,
            PresentacionProductoFinalEvidenciaRepository evidenciaRepository,
            AptitudDocumentalService aptitudDocumentalService, AutorizacionEfectivaService autorizacionService,
            AuditService auditService, IdempotencyService idempotencyService) {
        this.registroRepository = registroRepository; this.presentacionRepository = presentacionRepository;
        this.evidenciaRepository = evidenciaRepository; this.aptitudDocumentalService = aptitudDocumentalService;
        this.autorizacionService = autorizacionService; this.auditService = auditService;
        this.idempotencyService = idempotencyService;
    }

    @Override
    public PresentacionProductoFinalResponse presentar(long proyectoId, PresentacionProductoFinalRequest request,
            PortafolioAuthContext ctx, String idempotencyKey, String payloadJson) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) throw error(HttpStatus.UNPROCESSABLE_ENTITY, "IDEMPOTENCY_KEY_REQUIRED");
        final PresentacionProductoFinalResponse[] resultado = new PresentacionProductoFinalResponse[1];
        idempotencyService.execute(new IdempotencyService.IdempotencyRequest("PORTAFOLIO", "PRESENTAR_PRODUCTO_FINAL",
                idempotencyKey, payloadJson, ctx == null ? "unknown" : ctx.actorSub()), () -> {
            resultado[0] = presentarInterno(proyectoId, request, ctx);
            return new IdempotencyService.IdempotencyResponse("PRESENTACION_PRODUCTO_FINAL", resultado[0].idPresentacion(), "{}");
        });
        return resultado[0];
    }

    @Transactional
    PresentacionProductoFinalResponse presentarInterno(long proyectoId, PresentacionProductoFinalRequest request,
            PortafolioAuthContext ctx) {
        RegistroPortafolioEntity proyecto = registroRepository.findById(proyectoId)
                .filter(r -> r.getTipoRegistro() == TipoRegistro.PROYECTO)
                .filter(r -> r.getEstado() == EstadoIniciativa.PROYECTO_EJECUCION)
                .orElseThrow(() -> error(HttpStatus.CONFLICT, "PRESENTATION_NOT_ALLOWED_IN_STATE"));
        autorizar(ctx, proyecto);
        validar(request);
        if (!aptitudDocumentalService.esApto(request.idDocumentoSustenta(), "DocumentacionProductoFinal"))
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "EVIDENCE_NOT_ELIGIBLE");
        for (Long evidenciaId : request.evidenciaIds()) {
            if (!aptitudDocumentalService.esApto(evidenciaId, "EvidenciaProductoFinal"))
                throw error(HttpStatus.UNPROCESSABLE_ENTITY, "EVIDENCE_NOT_ELIGIBLE");
        }
        PresentacionProductoFinalEntity anterior = presentacionRepository
                .findFirstByIdProyectoOrderByVersionDesc(proyectoId).orElse(null);
        PresentacionProductoFinalEntity entidad = new PresentacionProductoFinalEntity();
        entidad.setIdProyecto(proyectoId); entidad.setVersion(anterior == null ? 1 : anterior.getVersion() + 1);
        entidad.setIdVersionAnterior(anterior == null ? null : anterior.getId()); entidad.setIdResponsable(ctx.actorUsuarioId());
        entidad.setIdDocumentoSustenta(request.idDocumentoSustenta()); entidad.setTipoProductoFinal(request.tipoProductoFinal());
        entidad.setDocumentacionGestion(normalizar(request.documentacionGestion())); entidad.setResultadosClave(normalizar(request.resultadosClave()));
        entidad.setNota(normalizar(request.nota())); entidad.setDescripcion(serializar(entidad));
        entidad = presentacionRepository.save(entidad);
        for (Long documentoId : request.evidenciaIds().stream().distinct().toList()) {
            PresentacionProductoFinalEvidenciaEntity evidencia = new PresentacionProductoFinalEvidenciaEntity();
            evidencia.setIdPresentacion(entidad.getId()); evidencia.setIdDocumento(documentoId); evidencia.setCreadoPor(ctx.actorSub());
            evidenciaRepository.save(evidencia);
        }
        auditService.registrarExito(new AuditService.AuditCommand(ctx.correlacionId(), ctx.actorUsuarioId(), null,
                ctx.asignacionEfectivaId(), ctx.perfilEfectivo(), ctx.unidadEfectivaId(), "PRESENTAR_PRODUCTO_FINAL",
                "PORTAFOLIO", "PRESENTACION_PRODUCTO_FINAL", entidad.getId(), "SUCCESS",
                Map.of("proyectoId", String.valueOf(proyectoId), "evidencias", String.valueOf(request.evidenciaIds().size())), "INTERNO"));
        // No se persiste ni invoca transición: el estado sigue PROYECTO_EJECUCION.
        return respuesta(entidad, request.evidenciaIds().stream().distinct().toList());
    }

    private void autorizar(PortafolioAuthContext ctx, RegistroPortafolioEntity proyecto) {
        if (ctx == null || !"Responsable".equals(ctx.perfilEfectivo()) || ctx.actorSub() == null || ctx.asignacionEfectivaId() == null)
            throw error(HttpStatus.FORBIDDEN, "ASSIGNMENT_SCOPE_DENIED");
        AutorizacionEfectivaService.AsignacionEfectiva asignacion = autorizacionService.revalidarParaOperacionSensible(ctx.actorSub(), ctx.asignacionEfectivaId(), "Responsable", proyecto.getUnidadEjecutoraId());
        if (ctx.actorUsuarioId() == null || !ctx.actorUsuarioId().equals(asignacion.usuarioId()))
            throw error(HttpStatus.FORBIDDEN, "ASSIGNMENT_SCOPE_DENIED");
    }
    private static void validar(PresentacionProductoFinalRequest r) {
        if (r == null || !TIPOS_FINALES.contains(r.tipoProductoFinal()) || r.idDocumentoSustenta() == null || r.evidenciaIds() == null)
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "PRODUCT_FINAL_TYPE_REQUIRED");
        if (r.resultadosClave() == null || r.resultadosClave().isBlank()) throw error(HttpStatus.UNPROCESSABLE_ENTITY, "OFFICIAL_FIELD_REQUIRED");
        if (r.evidenciaIds().stream().anyMatch(id -> id == null)) throw error(HttpStatus.UNPROCESSABLE_ENTITY, "EVIDENCE_NOT_ELIGIBLE");
    }
    private static PresentacionProductoFinalResponse respuesta(PresentacionProductoFinalEntity e,List<Long> evidencias) { return new PresentacionProductoFinalResponse(e.getId(),e.getIdProyecto(),e.getVersion(),e.getIdVersionAnterior()==null?0:e.getIdVersionAnterior(),e.getTipoProductoFinal(),e.getDocumentacionGestion(),e.getResultadosClave(),e.getNota(),e.getIdDocumentoSustenta(),evidencias,"\""+e.getId()+"-"+e.getVersion()+"\""); }
    private static String serializar(PresentacionProductoFinalEntity e) { return String.join("|", e.getTipoProductoFinal(), nulo(e.getDocumentacionGestion()), nulo(e.getResultadosClave()), nulo(e.getNota())); }
    private static String normalizar(String s){return s == null ? null : s.trim();} private static String nulo(String s){return s==null?"":s;} private static ResponseStatusException error(HttpStatus h,String c){return new ResponseStatusException(h,c);}
}
