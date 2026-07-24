package pe.gob.midagri.piip.seguridad.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import pe.gob.midagri.piip.auditoria.service.IdempotencyService;
import pe.gob.midagri.piip.seguridad.dto.MatrixAuthContext;
import pe.gob.midagri.piip.seguridad.dto.MatrixCombination;
import pe.gob.midagri.piip.seguridad.dto.MatrixDeactivationRequest;
import pe.gob.midagri.piip.seguridad.dto.MatrixFunction;
import pe.gob.midagri.piip.seguridad.dto.MatrixVersionDetail;
import pe.gob.midagri.piip.seguridad.dto.MatrixVersionRequest;
import pe.gob.midagri.piip.seguridad.service.MatrizAsignacionService;

/** API institucional de la matriz aprobada externamente y registrada en PIIP. */
@Validated
@RestController
@RequestMapping("/api/v1/seguridad")
@Tag(name = "Seguridad - Matriz funcional")
public class MatrizFuncionalController {
    private final MatrizAsignacionService matriz;
    private final IdempotencyService idempotencia;
    private final ObjectMapper json;

    public MatrizFuncionalController(MatrizAsignacionService matriz, IdempotencyService idempotencia,
            ObjectMapper json) {
        this.matriz = matriz; this.idempotencia = idempotencia; this.json = json;
    }

    @Operation(summary = "Crear una versión inmutable de matriz funcional")
    @PostMapping(value = "/matrices/versiones", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MatrixVersionDetail> crearVersion(@Valid @RequestBody MatrixVersionRequest request,
            @RequestHeader("Idempotency-Key") String clave,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacion,
            @RequestHeader("X-Unidad-Efectiva-Id") Long unidad,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlacion, Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(idempotente("CREAR_VERSION_MATRIZ", clave, request,
                () -> matriz.crearVersion(request, contexto(principal, asignacion, unidad, correlacion)), principal));
    }

    @Operation(summary = "Consultar el historial paginado de matrices")
    @GetMapping("/matrices/versiones")
    public Page<MatrixVersionDetail> listarVersiones(@RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanio,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacion,
            @RequestHeader("X-Unidad-Efectiva-Id") Long unidad,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlacion, Principal principal) {
        return matriz.listarVersiones(pagina, tamanio, contexto(principal, asignacion, unidad, correlacion));
    }

    @Operation(summary = "Consultar funciones vigentes e históricas autorizadas")
    @GetMapping("/funciones")
    public List<MatrixFunction> listarFunciones(@RequestHeader("X-Asignacion-Efectiva-Id") Long asignacion,
            @RequestHeader("X-Unidad-Efectiva-Id") Long unidad,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlacion, Principal principal) {
        return matriz.listarFunciones(contexto(principal, asignacion, unidad, correlacion));
    }

    @Operation(summary = "Consultar las combinaciones de una versión de matriz")
    @GetMapping("/matrices/versiones/{id}/combinaciones")
    public List<MatrixCombination> listarCombinaciones(@PathVariable Long id,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacion,
            @RequestHeader("X-Unidad-Efectiva-Id") Long unidad,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlacion, Principal principal) {
        return matriz.listarCombinaciones(id, contexto(principal, asignacion, unidad, correlacion));
    }

    @Operation(summary = "Inactivar una combinación mediante una nueva versión")
    @PostMapping(value = "/matrices/combinaciones/{id}/inactivaciones", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MatrixVersionDetail> inactivar(@PathVariable Long id,
            @Valid @RequestBody MatrixDeactivationRequest request, @RequestHeader("Idempotency-Key") String clave,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacion,
            @RequestHeader("X-Unidad-Efectiva-Id") Long unidad,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlacion, Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(idempotente("INACTIVAR_COMBINACION_MATRIZ", clave,
                request, () -> matriz.inactivarCombinacion(id, request,
                        contexto(principal, asignacion, unidad, correlacion)), principal));
    }

    private MatrixAuthContext contexto(Principal principal, Long asignacion, Long unidad, String correlacion) {
        return new MatrixAuthContext(principal == null ? null : principal.getName(), asignacion, unidad, correlacion);
    }

    private MatrixVersionDetail idempotente(String operacion, String clave, Object request,
            java.util.function.Supplier<MatrixVersionDetail> accion, Principal principal) {
        try {
            String payload = json.writeValueAsString(request);
            var resultado = idempotencia.execute(new IdempotencyService.IdempotencyRequest("SEGURIDAD", operacion,
                    clave, payload, principal == null ? "" : principal.getName()), () -> {
                        MatrixVersionDetail detalle = accion.get();
                        try {
                            return new IdempotencyService.IdempotencyResponse("MATRIZ_FUNCIONAL_VERSION", detalle.id(),
                                    json.writeValueAsString(detalle));
                        } catch (JsonProcessingException ex) {
                            throw new IllegalStateException("No fue posible serializar la respuesta de matriz.", ex);
                        }
                    });
            return json.readValue(resultado.respuestaJson(), MatrixVersionDetail.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No fue posible procesar la idempotencia de matriz.", ex);
        }
    }
}
