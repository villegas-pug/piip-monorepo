package pe.gob.midagri.piip.portafolio.cierre.service.impl;

import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import pe.gob.midagri.piip.portafolio.cierre.dto.DecisionProductoFinal;
import pe.gob.midagri.piip.portafolio.cierre.dto.DecisionProductoFinalRequest;
import pe.gob.midagri.piip.portafolio.cierre.dto.DecisionProductoFinalResponse;
import pe.gob.midagri.piip.portafolio.cierre.service.DecisionProductoFinalService;
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;
import pe.gob.midagri.piip.portafolio.seguimiento.service.AptitudDocumentalService;
import pe.gob.midagri.piip.portafolio.transicion.TransicionCommand;
import pe.gob.midagri.piip.portafolio.transicion.TransicionDetail;
import pe.gob.midagri.piip.portafolio.transicion.TransicionEstadoService;

/**
 * Adapta la decisión de producto a la única máquina de estados canónica.
 * La máquina conserva bajo bloqueo la revalidación Oracle, el historial
 * append-only, la idempotencia y la auditoría atómica; este servicio solo
 * valida los requisitos propios de la decisión formal.
 */
@Service
public class DecisionProductoFinalServiceImpl implements DecisionProductoFinalService {

    private static final Set<String> TIPOS_PRODUCTO_FINAL = Set.of(
            "PROTOTIPO_CONCEPTUALIZADO", "SOLUCION_FUNCIONAL");

    private final TransicionEstadoService transicionEstadoService;
    private final AptitudDocumentalService aptitudDocumentalService;

    public DecisionProductoFinalServiceImpl(TransicionEstadoService transicionEstadoService,
            AptitudDocumentalService aptitudDocumentalService) {
        this.transicionEstadoService = transicionEstadoService;
        this.aptitudDocumentalService = aptitudDocumentalService;
    }

    @Override
    public DecisionProductoFinalResponse decidir(long proyectoId,
            DecisionProductoFinalRequest request, String ifMatch,
            PortafolioAuthContext contexto, String idempotencyKey, String payloadJson) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw error(HttpStatus.PRECONDITION_REQUIRED, "IF_MATCH_REQUIRED");
        }
        if (request == null || request.decision() == null) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "PRODUCT_DECISION_REQUIRED");
        }
        if (!TIPOS_PRODUCTO_FINAL.contains(request.tipoProductoFinal())) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "PRODUCT_FINAL_TYPE_REQUIRED");
        }

        EstadoIniciativa destino = request.decision() == DecisionProductoFinal.APROBAR
                ? EstadoIniciativa.PRODUCTO_APROBADO
                : EstadoIniciativa.PRODUCTO_NO_APROBADO;
        Long documentoId = documentoRequerido(request, destino);
        String observacion = normalizarObservacion(request.observacion(), destino);
        validarDocumentoApto(documentoId, destino);

        TransicionDetail detalle = transicionEstadoService.transicionar(proyectoId,
                new TransicionCommand(destino, observacion, documentoId, ifMatch,
                        request.tipoProductoFinal()),
                contexto, idempotencyKey, payloadCanonico(proyectoId, ifMatch, payloadJson));
        return new DecisionProductoFinalResponse(detalle.registroId(), detalle.estadoNuevo(),
                request.tipoProductoFinal(), detalle.transicionId(), detalle.fechaTransicion(),
                detalle.etag());
    }

    private static Long documentoRequerido(DecisionProductoFinalRequest request,
            EstadoIniciativa destino) {
        Long documentoId = destino == EstadoIniciativa.PRODUCTO_APROBADO
                ? request.documentoId() : request.evidenciaId();
        if (documentoId == null) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY,
                    destino == EstadoIniciativa.PRODUCTO_APROBADO
                            ? "FORMAL_DECISION_REQUIRED" : "EVIDENCE_NOT_ELIGIBLE");
        }
        return documentoId;
    }

    private void validarDocumentoApto(Long documentoId, EstadoIniciativa destino) {
        AptitudDocumentalService.TipoEvidenciaTransicion tipo =
                destino == EstadoIniciativa.PRODUCTO_APROBADO
                        ? AptitudDocumentalService.TipoEvidenciaTransicion.APROBACION_PRODUCTO_FINAL
                        : AptitudDocumentalService.TipoEvidenciaTransicion.NO_APROBACION_PRODUCTO_FINAL;
        if (!aptitudDocumentalService.esAptoParaTransicion(documentoId, tipo)) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY,
                    destino == EstadoIniciativa.PRODUCTO_APROBADO
                            ? "FORMAL_DECISION_REQUIRED" : "EVIDENCE_NOT_ELIGIBLE");
        }
    }

    private static String normalizarObservacion(String observacion, EstadoIniciativa destino) {
        if (observacion == null || observacion.isBlank()) {
            if (destino == EstadoIniciativa.PRODUCTO_NO_APROBADO) {
                throw error(HttpStatus.UNPROCESSABLE_ENTITY, "OBSERVATION_REQUIRED");
            }
            return null;
        }
        return observacion.trim();
    }

    private static String payloadCanonico(long proyectoId, String ifMatch, String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "IDEMPOTENCY_PAYLOAD_REQUIRED");
        }
        return "{\"proyectoId\":" + proyectoId + ",\"ifMatch\":\""
                + ifMatch.replace("\"", "\\\"") + "\",\"comando\":" + payloadJson + "}";
    }

    private static ResponseStatusException error(HttpStatus status, String code) {
        return new ResponseStatusException(status, code);
    }
}
