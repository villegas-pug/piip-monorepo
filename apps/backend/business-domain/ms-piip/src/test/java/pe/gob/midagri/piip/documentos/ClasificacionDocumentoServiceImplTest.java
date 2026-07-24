package pe.gob.midagri.piip.documentos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import pe.gob.midagri.piip.auditoria.service.AuditService;
import pe.gob.midagri.piip.documentos.dto.ClasificacionHistDetalle;
import pe.gob.midagri.piip.documentos.dto.DocumentoAuthorizedContext;
import pe.gob.midagri.piip.documentos.dto.ReclasificarDocumentoCommand;
import pe.gob.midagri.piip.documentos.dto.ReclasificacionDocumentoResult;
import pe.gob.midagri.piip.documentos.dto.ValidacionClasificacionResult;
import pe.gob.midagri.piip.documentos.dto.ValidarClasificacionCommand;
import pe.gob.midagri.piip.documentos.entity.ClasificacionDocumento;
import pe.gob.midagri.piip.documentos.entity.DocumentoClasificacionHistEntity;
import pe.gob.midagri.piip.documentos.entity.DocumentoVersionEntity;
import pe.gob.midagri.piip.documentos.entity.ResultadoClasificacion;
import pe.gob.midagri.piip.documentos.repository.DocumentoClasificacionHistRepository;
import pe.gob.midagri.piip.documentos.repository.DocumentoVersionRepository;
import pe.gob.midagri.piip.documentos.service.impl.ClasificacionDocumentoServiceImpl;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService.AsignacionEfectiva;

class ClasificacionDocumentoServiceImplTest {

    private final DocumentoVersionRepository versiones = mock(DocumentoVersionRepository.class);
    private final DocumentoClasificacionHistRepository hist = mock(DocumentoClasificacionHistRepository.class);
    private final AuditService auditoria = mock(AuditService.class);
    private final ClasificacionDocumentoServiceImpl service =
            new ClasificacionDocumentoServiceImpl(versiones, hist, auditoria);

    @Test
    void validaClasificacionInicialPersistiendoEvaluador() {
        DocumentoVersionEntity documento = new DocumentoVersionEntity();
        documento.setId(40L);
        documento.setNumeroVersion(1);
        documento.setActivo("S");
        documento.setInmutable("S");
        when(versiones.findById(40L)).thenReturn(Optional.of(documento));
        when(versiones.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ValidacionClasificacionResult resultado = service.validarClasificacion(
                contextoEvaluador(), 40L, "\"40-1-P\"",
                new ValidarClasificacionCommand(ClasificacionDocumento.INTERNO));

        assertEquals(ClasificacionDocumento.INTERNO, resultado.clasificacionValidada());
        assertEquals(10L, resultado.evaluadorId());
        verify(auditoria).registrarExito(any());
    }

    @Test
    void rechazaValidacionQueSoloRelajaLaClasificacionValidada() {
        DocumentoVersionEntity documento = new DocumentoVersionEntity();
        documento.setId(40L);
        documento.setNumeroVersion(1);
        documento.setActivo("S");
        documento.setInmutable("S");
        documento.setClasificacionValidada(ClasificacionDocumento.RESTRINGIDO);
        when(versiones.findById(40L)).thenReturn(Optional.of(documento));

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () ->
                service.validarClasificacion(contextoEvaluador(), 40L, "\"40-1-RESTRINGIDO\"",
                        new ValidarClasificacionCommand(ClasificacionDocumento.PUBLICO)));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
        assertEquals("CLASIFICACION_RECLASIFICACION_REQUERIDA", error.getReason());
        verify(versiones, never()).save(any());
    }

    @Test
    void rechazaReclasificacionMenosRestrictiva() {
        DocumentoVersionEntity documento = new DocumentoVersionEntity();
        documento.setId(40L);
        documento.setNumeroVersion(1);
        documento.setActivo("S");
        documento.setInmutable("S");
        documento.setClasificacionValidada(ClasificacionDocumento.RESTRINGIDO);
        when(versiones.findById(40L)).thenReturn(Optional.of(documento));

        ReclasificarDocumentoCommand comando = new ReclasificarDocumentoCommand(
                ClasificacionDocumento.PUBLICO, 50L, 99L, "Motivo");
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () ->
                service.reclasificar(contextoEvaluador(), 40L, "\"40-1-RESTRINGIDO\"", comando));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
        assertEquals("RECLASIFICACION_MENOS_RESTRICTIVA", error.getReason());
        verify(hist, never()).saveAndFlush(any());
    }

    @Test
    void rechazaReclasificacionConMismaAutoridadQueRegistrador() {
        DocumentoVersionEntity documento = new DocumentoVersionEntity();
        documento.setId(40L);
        documento.setNumeroVersion(1);
        documento.setActivo("S");
        documento.setInmutable("S");
        documento.setClasificacionValidada(ClasificacionDocumento.INTERNO);
        when(versiones.findById(40L)).thenReturn(Optional.of(documento));

        ReclasificarDocumentoCommand comando = new ReclasificarDocumentoCommand(
                ClasificacionDocumento.RESTRINGIDO, 50L, contextoEvaluador().actorUsuarioId(),
                "Motivo");
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () ->
                service.reclasificar(contextoEvaluador(), 40L, "\"40-1-INTERNO\"", comando));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
        assertEquals("AUTORIDAD_DISTINTA_REGISTRADOR", error.getReason());
    }

    @Test
    void aplicaReclasificacionRestrictivaRegistrandoHistorial() {
        DocumentoVersionEntity documento = new DocumentoVersionEntity();
        documento.setId(40L);
        documento.setNumeroVersion(1);
        documento.setActivo("S");
        documento.setInmutable("S");
        documento.setClasificacionValidada(ClasificacionDocumento.INTERNO);
        DocumentoVersionEntity refrescado = new DocumentoVersionEntity();
        refrescado.setId(40L);
        refrescado.setNumeroVersion(1);
        refrescado.setActivo("S");
        refrescado.setInmutable("S");
        refrescado.setClasificacionValidada(ClasificacionDocumento.RESTRINGIDO);
        refrescado.setUsuarioValidaId(10L);
        when(versiones.findById(40L)).thenReturn(Optional.of(documento), Optional.of(refrescado));
        when(versiones.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(hist.saveAndFlush(any())).thenAnswer(inv -> {
            DocumentoClasificacionHistEntity entidad = inv.getArgument(0);
            entidad.setId(123L);
            return entidad;
        });

        ReclasificacionDocumentoResult resultado = service.reclasificar(
                contextoEvaluador(), 40L, "\"40-1-INTERNO\"",
                new ReclasificarDocumentoCommand(
                        ClasificacionDocumento.RESTRINGIDO, 50L, 99L, "Refuerzo"));

        assertEquals(ClasificacionDocumento.RESTRINGIDO, resultado.clasificacionNueva());
        assertEquals(ClasificacionDocumento.INTERNO, resultado.clasificacionAnterior());
        assertEquals(ResultadoClasificacion.APLICADA, resultado.historial().resultado());
        verify(auditoria).registrarExito(any());
    }

    @Test
    void devuelveHistorialOrdenadoSinEntidadJPA() {
        DocumentoVersionEntity documento = new DocumentoVersionEntity();
        documento.setId(40L);
        documento.setActivo("S");
        documento.setInmutable("S");
        when(versiones.findById(40L)).thenReturn(Optional.of(documento));
        DocumentoClasificacionHistEntity entrada = new DocumentoClasificacionHistEntity();
        entrada.setId(1L);
        entrada.setDocumentoId(40L);
        entrada.setClasificacionAnterior(ClasificacionDocumento.INTERNO);
        entrada.setClasificacionNueva(ClasificacionDocumento.RESTRINGIDO);
        entrada.setAutoridadDecisoraId(99L);
        entrada.setEvaluadorRegistradorId(10L);
        entrada.setDocumentoDecisionId(50L);
        entrada.setMotivo("Refuerzo");
        entrada.setResultado(ResultadoClasificacion.APLICADA);
        when(hist.findByDocumentoIdOrderByFechaCambioAsc(40L)).thenReturn(List.of(entrada));

        List<ClasificacionHistDetalle> resultado = service.listarHistorial(contextoEvaluador(), 40L);

        assertEquals(1, resultado.size());
        assertNotNull(resultado.get(0).historialId());
        assertNull(resultado.get(0).fechaCambio()); // El default la aplica el servidor al persistir.
    }

    private static DocumentoAuthorizedContext contextoEvaluador() {
        return new DocumentoAuthorizedContext("sub-1", 10L,
                new AsignacionEfectiva(2L, 10L, 4L, "Evaluador", 7L),
                7L, 40L, "corr-1");
    }
}
