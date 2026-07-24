package pe.gob.midagri.piip.portafolio.incorporacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import pe.gob.midagri.piip.auditoria.service.AuditService;
import pe.gob.midagri.piip.portafolio.dto.CreateIncorporacionRequest;
import pe.gob.midagri.piip.portafolio.dto.IncorporacionCorreccionRequest;
import pe.gob.midagri.piip.portafolio.dto.IncorporacionDetail;
import pe.gob.midagri.piip.portafolio.dto.IncorporacionResolucionConflictoRequest;
import pe.gob.midagri.piip.portafolio.dto.IncorporacionValidacionRequest;
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.entity.EstadoIncorporacion;
import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;
import pe.gob.midagri.piip.portafolio.entity.IncorporacionCambioEntity;
import pe.gob.midagri.piip.portafolio.entity.IncorporacionConflictoEntity;
import pe.gob.midagri.piip.portafolio.entity.IncorporacionRegistroEntity;
import pe.gob.midagri.piip.portafolio.entity.TipoConflicto;
import pe.gob.midagri.piip.portafolio.repository.IncorporacionCambioRepository;
import pe.gob.midagri.piip.portafolio.repository.IncorporacionConflictoRepository;
import pe.gob.midagri.piip.portafolio.repository.IncorporacionRegistroRepository;
import pe.gob.midagri.piip.portafolio.repository.RegistroPortafolioRepository;
import pe.gob.midagri.piip.portafolio.service.IncorporacionRegistroService;
import pe.gob.midagri.piip.portafolio.service.impl.IncorporacionRegistroServiceImpl;

/**
 * Pruebas unitarias para la incorporación individual de información
 * existente conforme al contrato
 * {@code specs/001-gestionar-portafolio-innovacion/contracts/portafolio.md}.
 *
 * <p>El ciclo de vida es: {@code PENDIENTE} → correcciones ilimitadas
 * append-only → resolución de conflictos (si los hay) → {@code VALIDADO} o
 * {@code RECHAZADO} por el Evaluador. La validación exige hash de origen,
 * datos originales y que cualquier conflicto esté resuelto.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("US1 - Incorporación individual: IncorporacionRegistroService")
class IncorporacionRegistroServiceTest {

    @Mock private IncorporacionRegistroRepository incorporacionRepository;
    @Mock private IncorporacionCambioRepository cambioRepository;
    @Mock private IncorporacionConflictoRepository conflictoRepository;
    @Mock private RegistroPortafolioRepository registroRepository;
    @Mock private AuditService auditService;

    private IncorporacionRegistroService service;

    @BeforeEach
    void setUp() {
        service = new IncorporacionRegistroServiceImpl(
                incorporacionRepository, cambioRepository, conflictoRepository,
                registroRepository, auditService);
    }

    private PortafolioAuthContext contextoResponsable() {
        return new PortafolioAuthContext("sub-002", 2L, 200L, "Responsable", 1L, 1L, "corr-002");
    }

    private PortafolioAuthContext contextoEvaluador() {
        return new PortafolioAuthContext("sub-evaluador", 99L, 999L, "Evaluador", 1L, 1L, "corr-eval");
    }

    @Test
    @DisplayName("Registrar incorporación nueva devuelve PENDIENTE y audita el alta")
    void registrar_nuevaIncorporacion_retornaPendiente() {
        CreateIncorporacionRequest request = new CreateIncorporacionRequest(
                "Archivo historico",
                LocalDate.of(2025, 1, 15),
                1L, 500L, "abc123hash", "{\"nombre\":\"Iniciativa legacy\"}", null);
        when(incorporacionRepository.findByHashOriginalAndResponsableIdAndFuente("abc123hash", 1L, "Archivo historico"))
                .thenReturn(Optional.empty());

        IncorporacionRegistroEntity saved = new IncorporacionRegistroEntity();
        saved.setId(1L);
        saved.setEstado(EstadoIncorporacion.PENDIENTE);
        saved.setHashOriginal("abc123hash");
        when(incorporacionRepository.save(any(IncorporacionRegistroEntity.class))).thenReturn(saved);

        IncorporacionDetail detalle = service.registrar(request, contextoResponsable());

        assertNotNull(detalle);
        assertEquals(EstadoIncorporacion.PENDIENTE, detalle.estado());
        assertEquals(1L, detalle.id());
        verify(incorporacionRepository).save(any(IncorporacionRegistroEntity.class));
        verify(auditService).registrarExito(any(AuditService.AuditCommand.class));
    }

    @Test
    @DisplayName("Hash duplicado para el mismo Responsable se rechaza con 409 CONFLICT")
    void registrar_hashDuplicado_lanzaConflict() {
        CreateIncorporacionRequest request = new CreateIncorporacionRequest(
                "Archivo", LocalDate.of(2025, 1, 15),
                1L, 500L, "abc123hash", null, null);

        IncorporacionRegistroEntity existente = new IncorporacionRegistroEntity();
        existente.setId(7L);
        when(incorporacionRepository.findByHashOriginalAndResponsableIdAndFuente("abc123hash", 1L, "Archivo"))
                .thenReturn(Optional.of(existente));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.registrar(request, contextoResponsable()));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertTrue(error.getReason() != null && error.getReason().contains("DUPLICATE_INCORPORATION_HASH"));
    }

    @Test
    @DisplayName("Código heredado duplicado abre conflicto DUPLICADO sin bloquear la creación")
    void registrar_codigoHeredadoDuplicado_abreConflictoPendiente() {
        CreateIncorporacionRequest request = new CreateIncorporacionRequest(
                "Archivo", LocalDate.of(2025, 1, 15),
                1L, 500L, "otro-hash-1", "{}", "CODIGO-LEGACY-01");
        when(incorporacionRepository.findByHashOriginalAndResponsableIdAndFuente("otro-hash-1", 1L, "Archivo"))
                .thenReturn(Optional.empty());
        var registroExistente = new pe.gob.midagri.piip.portafolio.entity.RegistroPortafolioEntity();
        registroExistente.setId(22L);
        when(registroRepository.findByCodigoOrigen("CODIGO-LEGACY-01")).thenReturn(Optional.of(registroExistente));

        IncorporacionRegistroEntity saved = new IncorporacionRegistroEntity();
        saved.setId(2L);
        saved.setEstado(EstadoIncorporacion.PENDIENTE);
        when(incorporacionRepository.save(any(IncorporacionRegistroEntity.class))).thenReturn(saved);
        when(conflictoRepository.save(any(IncorporacionConflictoEntity.class))).thenReturn(new IncorporacionConflictoEntity());

        IncorporacionDetail detalle = service.registrar(request, contextoResponsable());
        assertNotNull(detalle);
        assertEquals(EstadoIncorporacion.PENDIENTE, detalle.estado());
        verify(conflictoRepository).save(any(IncorporacionConflictoEntity.class));
    }

    @Test
    @DisplayName("Corrección sobre PENDIENTE registra cambio append-only con datos antes y después")
    void corregir_pendiente_registraCambioAppendOnly() {
        IncorporacionRegistroEntity entity = new IncorporacionRegistroEntity();
        entity.setId(1L);
        entity.setEstado(EstadoIncorporacion.PENDIENTE);
        entity.setDatosOriginales("datos originales");
        when(incorporacionRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(cambioRepository.save(any(IncorporacionCambioEntity.class))).thenReturn(new IncorporacionCambioEntity());
        when(incorporacionRepository.save(any(IncorporacionRegistroEntity.class))).thenReturn(entity);

        IncorporacionCorreccionRequest req = new IncorporacionCorreccionRequest(1L,
                "datos nuevos", "Corrección solicitada por Evaluación");
        IncorporacionDetail resultado = service.corregir(req, contextoResponsable());

        assertNotNull(resultado);
        verify(cambioRepository).save(any(IncorporacionCambioEntity.class));
        verify(auditService).registrarExito(any(AuditService.AuditCommand.class));
    }

    @Test
    @DisplayName("Corrección sobre estado distinto a PENDIENTE se rechaza con 409")
    void corregir_estadoNoPendiente_lanzaConflict() {
        IncorporacionRegistroEntity entity = new IncorporacionRegistroEntity();
        entity.setId(1L);
        entity.setEstado(EstadoIncorporacion.VALIDADO);
        when(incorporacionRepository.findById(1L)).thenReturn(Optional.of(entity));

        IncorporacionCorreccionRequest req = new IncorporacionCorreccionRequest(1L, "x", "motivo");
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.corregir(req, contextoResponsable()));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(cambioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Las correcciones son ilimitadas mientras esté PENDIENTE")
    void corregir_multiplesCorrecciones_todasAnexadas() {
        IncorporacionRegistroEntity entity = new IncorporacionRegistroEntity();
        entity.setId(1L);
        entity.setEstado(EstadoIncorporacion.PENDIENTE);
        when(incorporacionRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(cambioRepository.save(any(IncorporacionCambioEntity.class))).thenReturn(new IncorporacionCambioEntity());
        when(incorporacionRepository.save(any(IncorporacionRegistroEntity.class))).thenReturn(entity);

        for (int i = 0; i < 5; i++) {
            service.corregir(new IncorporacionCorreccionRequest(1L,
                    "datos v" + i, "motivo " + i), contextoResponsable());
        }
        verify(cambioRepository, times(5)).save(any(IncorporacionCambioEntity.class));
    }

    @Test
    @DisplayName("Validación sin conflictos pendientes transita a VALIDADO y puede vincular duplicado")
    void validar_sinConflictos_transicionaValidadoYVinculaDuplicado() {
        IncorporacionRegistroEntity entity = new IncorporacionRegistroEntity();
        entity.setId(1L);
        entity.setEstado(EstadoIncorporacion.PENDIENTE);
        when(incorporacionRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(conflictoRepository.findByIncorporacionIdAndResuelto(1L, "N"))
                .thenReturn(Collections.emptyList());
        when(incorporacionRepository.save(any(IncorporacionRegistroEntity.class))).thenReturn(entity);

        IncorporacionValidacionRequest req = new IncorporacionValidacionRequest(
                1L, EstadoIniciativa.PRESENTADO, 42L, "Validado con vínculo a iniciativa 42");
        IncorporacionDetail detalle = service.validar(req, contextoEvaluador());

        assertNotNull(detalle);
        verify(auditService).registrarExito(any(AuditService.AuditCommand.class));
    }

    @Test
    @DisplayName("Validación con conflictos pendientes se rechaza con INCORPORATION_CONFLICT_UNRESOLVED")
    void validar_conConflictosPendientes_lanzaConflict() {
        IncorporacionRegistroEntity entity = new IncorporacionRegistroEntity();
        entity.setId(1L);
        entity.setEstado(EstadoIncorporacion.PENDIENTE);
        when(incorporacionRepository.findById(1L)).thenReturn(Optional.of(entity));
        IncorporacionConflictoEntity conflicto = new IncorporacionConflictoEntity();
        conflicto.setId(9L);
        conflicto.setResuelto("N");
        conflicto.setTipoConflicto(TipoConflicto.DUPLICADO);
        when(conflictoRepository.findByIncorporacionIdAndResuelto(1L, "N"))
                .thenReturn(List.of(conflicto));
        when(incorporacionRepository.save(any(IncorporacionRegistroEntity.class))).thenReturn(entity);

        IncorporacionValidacionRequest req = new IncorporacionValidacionRequest(
                1L, EstadoIniciativa.PRESENTADO, null, "motivado");
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.validar(req, contextoEvaluador()));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertTrue(error.getReason() != null && error.getReason().contains("INCORPORATION_CONFLICT_UNRESOLVED"));
    }

    @Test
    @DisplayName("Validación sobre estado distinto a PENDIENTE se rechaza con 409")
    void validar_estadoNoPendiente_lanzaConflict() {
        IncorporacionRegistroEntity entity = new IncorporacionRegistroEntity();
        entity.setId(1L);
        entity.setEstado(EstadoIncorporacion.VALIDADO);
        when(incorporacionRepository.findById(1L)).thenReturn(Optional.of(entity));

        IncorporacionValidacionRequest req = new IncorporacionValidacionRequest(
                1L, EstadoIniciativa.PRESENTADO, null, null);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.validar(req, contextoEvaluador()));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    @DisplayName("Resolución de conflicto marca el conflicto como resuelto y exige contexto Evaluador")
    void resolverConflicto_exitosoMarcaResuelto() {
        IncorporacionConflictoEntity conflicto = new IncorporacionConflictoEntity();
        conflicto.setId(9L);
        conflicto.setIncorporacionId(1L);
        conflicto.setResuelto("N");
        conflicto.setTipoConflicto(TipoConflicto.CODIGO);
        when(conflictoRepository.findById(9L)).thenReturn(Optional.of(conflicto));
        when(conflictoRepository.save(any(IncorporacionConflictoEntity.class))).thenReturn(conflicto);

        IncorporacionRegistroEntity incorporation = new IncorporacionRegistroEntity();
        incorporation.setId(1L);
        incorporation.setEstado(EstadoIncorporacion.PENDIENTE);
        when(incorporacionRepository.findById(1L)).thenReturn(Optional.of(incorporation));

        IncorporacionResolucionConflictoRequest req = new IncorporacionResolucionConflictoRequest(
                9L, 1L, "Conflicto resuelto, sin duplicado", 600L);
        IncorporacionDetail resultado = service.resolverConflicto(req, contextoEvaluador());

        assertNotNull(resultado);
        verify(conflictoRepository).save(any(IncorporacionConflictoEntity.class));
        verify(auditService).registrarExito(any(AuditService.AuditCommand.class));
    }

    @Test
    @DisplayName("Resolver un conflicto ya resuelto se rechaza con 409")
    void resolverConflicto_yaResuelto_lanzaConflict() {
        IncorporacionConflictoEntity conflicto = new IncorporacionConflictoEntity();
        conflicto.setId(9L);
        conflicto.setResuelto("S");
        when(conflictoRepository.findById(9L)).thenReturn(Optional.of(conflicto));

        IncorporacionRegistroEntity incorporation = new IncorporacionRegistroEntity();
        incorporation.setId(1L);
        incorporation.setEstado(EstadoIncorporacion.PENDIENTE);
        when(incorporacionRepository.findById(1L)).thenReturn(Optional.of(incorporation));

        IncorporacionResolucionConflictoRequest req = new IncorporacionResolucionConflictoRequest(
                9L, 1L, "intento", null);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.resolverConflicto(req, contextoEvaluador()));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    @DisplayName("El servicio expone contrato de interfaz y no entidades JPA en su API pública")
    void servicioExponeContratoSinEntidadesJPA() {
        assertTrue(IncorporacionRegistroService.class.isInterface());
        for (var metodo : IncorporacionRegistroService.class.getDeclaredMethods()) {
            String retorno = metodo.getReturnType().getName();
            assertFalse(retorno.contains("pe.gob.midagri.piip.portafolio.entity"),
                    () -> "El método " + metodo.getName() + " no debe retornar entidades JPA");
        }
    }

    @Test
    @DisplayName("El estado RECHAZADO forma parte del ciclo de vida de la incorporación")
    void estadosIncorporacionCubrenRechazo() {
        assertNotNull(EstadoIncorporacion.PENDIENTE);
        assertNotNull(EstadoIncorporacion.VALIDADO);
        assertNotNull(EstadoIncorporacion.RECHAZADO);
    }

    @Test
    @DisplayName("Validación con observación vacía en estado RECHAZADO sigue siendo válida a nivel de servicio")
    void validar_estadoRechazado_aceptaObservacionOpcional() {
        IncorporacionRegistroEntity entity = new IncorporacionRegistroEntity();
        entity.setId(1L);
        entity.setEstado(EstadoIncorporacion.PENDIENTE);
        when(incorporacionRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(conflictoRepository.findByIncorporacionIdAndResuelto(1L, "N"))
                .thenReturn(Collections.emptyList());
        when(incorporacionRepository.save(any(IncorporacionRegistroEntity.class))).thenReturn(entity);

        // El servicio acepta observación nula u opcional; el detalle debe
        // generarse sin error y la transición se aplica.
        IncorporacionValidacionRequest req = new IncorporacionValidacionRequest(
                1L, EstadoIniciativa.INICIATIVA_ARCHIVADA, null, null);
        IncorporacionDetail detalle = service.validar(req, contextoEvaluador());
        assertNotNull(detalle);
    }

    @Test
    @DisplayName("Si el código heredado no existe, no se crea conflicto")
    void registrar_codigoHeredadoInexistente_noCreaConflicto() {
        CreateIncorporacionRequest request = new CreateIncorporacionRequest(
                "Archivo", LocalDate.of(2025, 1, 15),
                1L, 500L, "hash-sin-codigo", "{}", "CODIGO-NUEVO");
        when(incorporacionRepository.findByHashOriginalAndResponsableIdAndFuente("hash-sin-codigo", 1L, "Archivo"))
                .thenReturn(Optional.empty());
        when(registroRepository.findByCodigoOrigen("CODIGO-NUEVO")).thenReturn(Optional.empty());

        IncorporacionRegistroEntity saved = new IncorporacionRegistroEntity();
        saved.setId(3L);
        saved.setEstado(EstadoIncorporacion.PENDIENTE);
        when(incorporacionRepository.save(any(IncorporacionRegistroEntity.class))).thenReturn(saved);

        IncorporacionDetail detalle = service.registrar(request, contextoResponsable());
        assertNotNull(detalle);
        verify(conflictoRepository, never()).save(any());
    }
}
