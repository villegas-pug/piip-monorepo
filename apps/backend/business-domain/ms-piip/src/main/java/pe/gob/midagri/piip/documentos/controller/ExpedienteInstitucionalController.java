package pe.gob.midagri.piip.documentos.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import pe.gob.midagri.piip.config.ApiHeaders;
import pe.gob.midagri.piip.documentos.dto.CreateInstitutionalFileCommand;
import pe.gob.midagri.piip.documentos.dto.CreateInstitutionalFileRequest;
import pe.gob.midagri.piip.documentos.dto.DocumentoAuthorizedContext;
import pe.gob.midagri.piip.documentos.dto.ExpedienteInstitucionalDetail;
import pe.gob.midagri.piip.documentos.service.DocumentoAuthorizedContextResolver;
import pe.gob.midagri.piip.documentos.service.ExpedienteInstitucionalService;

/**
 * Controlador REST para expedientes institucionales.
 *
 * <p>El modulo de origen, la unidad y el registrador se resuelven en el
 * servidor a partir del contexto autorizado; el cliente nunca los declara.
 * Crear un expediente no concede por si mismo permiso documental sobre
 * las series que se le asocien: cada carga exige su propia autorizacion.
 * Los errores se devuelven como {@code application/problem+json} a traves
 * del {@code DocumentosExceptionHandler}, unico advice del modulo.
 */
@RestController
@RequestMapping("/api/v1/expedientes-institucionales")
public class ExpedienteInstitucionalController {

    private static final String PERFIL_RESPONSABLE = "Responsable";
    private static final String HEADER_ACTOR_SUB = "X-Actor-Sub";
    private static final String HEADER_UNIDAD_RECURSO = "X-Unidad-Recurso-Id";

    private final ExpedienteInstitucionalService expedienteService;
    private final DocumentoAuthorizedContextResolver contextoResolver;

    public ExpedienteInstitucionalController(ExpedienteInstitucionalService expedienteService,
            DocumentoAuthorizedContextResolver contextoResolver) {
        this.expedienteService = expedienteService;
        this.contextoResolver = contextoResolver;
    }

    /**
     * POST /api/v1/expedientes-institucionales
     * Crea un expediente para documentos formales sin registro de portafolio.
     * El cliente no puede declarar modulo de origen, unidad ni registrador.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ExpedienteInstitucionalDetail> crear(
            @RequestBody @Valid CreateInstitutionalFileRequest comando,
            @RequestHeader(ApiHeaders.EFFECTIVE_ASSIGNMENT_ID) Long asignacionEfectivaId,
            @RequestHeader(value = HEADER_ACTOR_SUB, required = false) String actorSub,
            @RequestHeader(value = ApiHeaders.CORRELATION_ID, required = false) String correlationId,
            @RequestHeader(value = HEADER_UNIDAD_RECURSO) Long unidadRecursoId) {

        if (unidadRecursoId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "INSTITUTIONAL_FILE_UNIT_REQUIRED");
        }
        DocumentoAuthorizedContext contexto = contextoResolver.resolver(
                actorSub, asignacionEfectivaId, PERFIL_RESPONSABLE,
                unidadRecursoId, null, correlationId);
        CreateInstitutionalFileCommand comandoInterno = new CreateInstitutionalFileCommand(
                comando.asunto(),
                comando.referenciaCasoUso(),
                comando.clasificacion());
        ExpedienteInstitucionalDetail detalle = expedienteService.crear(contexto, comandoInterno);
        return ResponseEntity.status(HttpStatus.CREATED).body(detalle);
    }
}
