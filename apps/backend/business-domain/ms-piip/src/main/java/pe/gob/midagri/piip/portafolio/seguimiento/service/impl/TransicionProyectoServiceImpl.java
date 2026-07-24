package pe.gob.midagri.piip.portafolio.seguimiento.service.impl;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.CancelacionRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.SuspensionRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.TransicionResponse;
import pe.gob.midagri.piip.portafolio.seguimiento.service.AptitudDocumentalService;
import pe.gob.midagri.piip.portafolio.seguimiento.service.TransicionProyectoService;
import pe.gob.midagri.piip.portafolio.transicion.TransicionCommand;
import pe.gob.midagri.piip.portafolio.transicion.TransicionDetail;
import pe.gob.midagri.piip.portafolio.transicion.TransicionEstadoService;

/**
 * Adaptador del contrato de seguimiento a la única máquina canónica de
 * estados. La máquina conserva el bloqueo, la autorización Oracle, el
 * historial append-only y la auditoría transaccional.
 */
@Service
public class TransicionProyectoServiceImpl implements TransicionProyectoService {

    private final TransicionEstadoService transicionEstadoService;
    private final AptitudDocumentalService aptitudDocumentalService;

    public TransicionProyectoServiceImpl(TransicionEstadoService transicionEstadoService,
            AptitudDocumentalService aptitudDocumentalService) {
        this.transicionEstadoService = transicionEstadoService;
        this.aptitudDocumentalService = aptitudDocumentalService;
    }

    @Override
    public TransicionResponse suspender(long proyectoId, SuspensionRequest request,
            PortafolioAuthContext ctx, String idempotencyKey, String payloadJson) {
        validarComando(request == null ? null : request.ifMatch(),
                request == null ? null : request.observacion());
        validarEvidencia(request.idDocumento(),
                AptitudDocumentalService.TipoEvidenciaTransicion.SUSPENSION,
                "EVIDENCE_NOT_ELIGIBLE");
        return transicionar(proyectoId, EstadoIniciativa.SUSPENDIDO,
                request.idDocumento(), request.observacion(), request.ifMatch(), ctx,
                idempotencyKey, payloadJson);
    }

    @Override
    public TransicionResponse cancelar(long proyectoId, CancelacionRequest request,
            PortafolioAuthContext ctx, String idempotencyKey, String payloadJson) {
        validarComando(request == null ? null : request.ifMatch(),
                request == null ? null : request.observacion());
        validarEvidencia(request.idDocumento(),
                AptitudDocumentalService.TipoEvidenciaTransicion.CANCELACION,
                "FORMAL_DECISION_REQUIRED");
        return transicionar(proyectoId, EstadoIniciativa.CANCELADO,
                request.idDocumento(), request.observacion(), request.ifMatch(), ctx,
                idempotencyKey, payloadJson);
    }

    private TransicionResponse transicionar(long proyectoId, EstadoIniciativa destino,
            long documentoId, String observacion, String ifMatch, PortafolioAuthContext ctx,
            String idempotencyKey, String payloadJson) {
        String payload = idempotencyKey == null || idempotencyKey.isBlank() ? payloadJson
                : payloadCanonico(proyectoId, destino, ifMatch, payloadJson);
        TransicionDetail detalle = transicionEstadoService.transicionar(proyectoId,
                new TransicionCommand(destino, observacion.trim(), documentoId, ifMatch), ctx,
                idempotencyKey, payload);
        return new TransicionResponse(detalle.transicionId(), detalle.registroId(),
                detalle.estadoAnterior(), detalle.estadoNuevo(),
                ctx == null || ctx.actorUsuarioId() == null ? 0L : ctx.actorUsuarioId(),
                detalle.fechaTransicion(), observacion.trim(), documentoId, detalle.etag());
    }

    private void validarComando(String ifMatch, String observacion) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED,
                    "IF_MATCH_REQUIRED: la transicion exige la cabecera If-Match");
        }
        if (observacion == null || observacion.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "OBSERVATION_REQUIRED: la observacion es obligatoria");
        }
    }

    private void validarEvidencia(Long documentoId,
            AptitudDocumentalService.TipoEvidenciaTransicion tipo, String codigo) {
        if (documentoId == null || !aptitudDocumentalService.esAptoParaTransicion(documentoId, tipo)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    codigo + ": el documento no es apto para " + tipo.tipoDocumental());
        }
    }

    private String payloadCanonico(long proyectoId, EstadoIniciativa destino, String ifMatch,
            String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new IllegalArgumentException(
                    "IDEMPOTENCY_PAYLOAD_REQUIRED: falta el cuerpo serializado.");
        }
        return "{\"proyectoId\":" + proyectoId + ",\"destino\":\"" + destino
                + "\",\"ifMatch\":\"" + ifMatch.replace("\"", "\\\"")
                + "\",\"comando\":" + payloadJson + "}";
    }
}
