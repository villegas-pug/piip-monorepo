package pe.gob.midagri.piip.portafolio.cierre.service.impl;

import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.gob.midagri.piip.portafolio.cierre.dto.CierreProyectoRequest;
import pe.gob.midagri.piip.portafolio.cierre.dto.CierreProyectoResponse;
import pe.gob.midagri.piip.portafolio.cierre.entity.CierreProyectoEntity;
import pe.gob.midagri.piip.portafolio.cierre.entity.ValidacionResultadoEntity;
import pe.gob.midagri.piip.portafolio.cierre.repository.CierreProyectoRepository;
import pe.gob.midagri.piip.portafolio.cierre.repository.ValidacionResultadoRepository;
import pe.gob.midagri.piip.portafolio.cierre.service.CierreProyectoService;
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;
import pe.gob.midagri.piip.portafolio.entity.RegistroPortafolioEntity;
import pe.gob.midagri.piip.portafolio.repository.RegistroPortafolioRepository;
import pe.gob.midagri.piip.portafolio.seguimiento.service.AptitudDocumentalService;
import pe.gob.midagri.piip.portafolio.transicion.TransicionCommand;
import pe.gob.midagri.piip.portafolio.transicion.TransicionDetail;
import pe.gob.midagri.piip.portafolio.transicion.TransicionEstadoService;

/**
 * Cierra un proyecto en una única transacción: valida los resultados que
 * registró el Responsable, registra el cierre y delega la transición,
 * revalidación Oracle, historial y auditoría en la máquina canónica.
 */
@Service
public class CierreProyectoServiceImpl implements CierreProyectoService {
    private final RegistroPortafolioRepository registroRepository;
    private final CierreProyectoRepository cierreRepository;
    private final ValidacionResultadoRepository validacionRepository;
    private final AptitudDocumentalService aptitudDocumentalService;
    private final TransicionEstadoService transicionEstadoService;

    public CierreProyectoServiceImpl(RegistroPortafolioRepository registroRepository,
            CierreProyectoRepository cierreRepository,
            ValidacionResultadoRepository validacionRepository,
            AptitudDocumentalService aptitudDocumentalService,
            TransicionEstadoService transicionEstadoService) {
        this.registroRepository = registroRepository;
        this.cierreRepository = cierreRepository;
        this.validacionRepository = validacionRepository;
        this.aptitudDocumentalService = aptitudDocumentalService;
        this.transicionEstadoService = transicionEstadoService;
    }

    @Override
    @Transactional
    public CierreProyectoResponse cerrar(long proyectoId, CierreProyectoRequest request, String ifMatch,
            PortafolioAuthContext contexto, String idempotencyKey, String payloadJson) {
        validarSolicitud(request, ifMatch, contexto, idempotencyKey, payloadJson);
        RegistroPortafolioEntity proyecto = registroRepository.findByIdForUpdate(proyectoId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND"));
        CierreProyectoEntity cierreExistente = cierreRepository.findByIdProyecto(proyectoId).orElse(null);
        if (cierreExistente != null) {
            TransicionDetail transicion = transicionEstadoService.transicionar(proyectoId,
                    new TransicionCommand(EstadoIniciativa.FINALIZADO, request.observacion().trim(),
                            request.informeFinalDocumentoId(), ifMatch),
                    contexto, idempotencyKey, payloadCanonico(proyectoId, ifMatch, payloadJson));
            return new CierreProyectoResponse(cierreExistente.getId(), proyectoId,
                    transicion.estadoNuevo(), fecha(cierreExistente), transicion.etag());
        }
        if (proyecto.getEstado() != EstadoIniciativa.PRODUCTO_APROBADO
                && proyecto.getEstado() != EstadoIniciativa.PRODUCTO_NO_APROBADO) {
            throw error(HttpStatus.CONFLICT, "CLOSURE_NOT_ALLOWED_IN_STATE");
        }
        if (vacio(proyecto.getResultadosClave())) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "CLOSURE_INCOMPLETE");
        }
        if (!aptitudDocumentalService.esAptoParaTransicion(request.informeFinalDocumentoId(),
                AptitudDocumentalService.TipoEvidenciaTransicion.INFORME_FINAL_CIERRE)) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "EVIDENCE_NOT_ELIGIBLE");
        }

        ValidacionResultadoEntity validacion = new ValidacionResultadoEntity();
        validacion.setIdProyecto(proyectoId);
        validacion.setIdResponsable(proyecto.getResponsableId());
        validacion.setIdEvaluador(contexto.actorUsuarioId());
        validacion.setResultadosClave(proyecto.getResultadosClave().trim());
        validacion.setCreadoPor(contexto.actorSub());
        validacionRepository.save(validacion);

        CierreProyectoEntity cierre = new CierreProyectoEntity();
        cierre.setIdProyecto(proyectoId);
        cierre.setInformeFinal(request.informeFinal().trim());
        cierre.setResultados(proyecto.getResultadosClave().trim());
        cierre.setAprendizajes(request.aprendizajes().trim());
        cierre.setConclusion(request.conclusion().trim());
        cierre.setObservacion(request.observacion().trim());
        cierre.setIdEvaluador(contexto.actorUsuarioId());
        cierre = cierreRepository.save(cierre);

        TransicionDetail transicion = transicionEstadoService.transicionar(proyectoId,
                new TransicionCommand(EstadoIniciativa.FINALIZADO, cierre.getObservacion(),
                        request.informeFinalDocumentoId(), ifMatch),
                contexto, idempotencyKey, payloadCanonico(proyectoId, ifMatch, payloadJson));
        return new CierreProyectoResponse(cierre.getId(), proyectoId, transicion.estadoNuevo(),
                proyecto.getFechaCierre(), transicion.etag());
    }

    private static void validarSolicitud(CierreProyectoRequest request, String ifMatch,
            PortafolioAuthContext contexto, String idempotencyKey, String payloadJson) {
        if (ifMatch == null || ifMatch.isBlank()) throw error(HttpStatus.PRECONDITION_REQUIRED, "IF_MATCH_REQUIRED");
        if (contexto == null || !"Evaluador".equals(contexto.perfilEfectivo())
                || contexto.actorSub() == null || contexto.actorUsuarioId() == null
                || contexto.asignacionEfectivaId() == null) throw error(HttpStatus.FORBIDDEN, "ASSIGNMENT_SCOPE_DENIED");
        if (request == null || vacio(request.informeFinal()) || request.informeFinalDocumentoId() == null
                || vacio(request.aprendizajes()) || vacio(request.conclusion()) || vacio(request.observacion()))
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "CLOSURE_INCOMPLETE");
        if (idempotencyKey == null || idempotencyKey.isBlank() || payloadJson == null || payloadJson.isBlank())
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "IDEMPOTENCY_KEY_REQUIRED");
    }

    private static String payloadCanonico(long proyectoId, String ifMatch, String payloadJson) {
        return "{\"proyectoId\":" + proyectoId + ",\"ifMatch\":\""
                + ifMatch.replace("\"", "\\\"") + "\",\"comando\":" + payloadJson + "}";
    }

    private static boolean vacio(String valor) { return valor == null || valor.isBlank(); }
    private static LocalDate fecha(CierreProyectoEntity cierre) {
        return cierre.getFechaCierre() == null ? null : cierre.getFechaCierre().toLocalDate();
    }
    private static ResponseStatusException error(HttpStatus status, String codigo) {
        return new ResponseStatusException(status, codigo);
    }
}
