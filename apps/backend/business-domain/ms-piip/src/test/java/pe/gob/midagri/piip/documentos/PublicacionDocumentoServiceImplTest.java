package pe.gob.midagri.piip.documentos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import pe.gob.midagri.piip.auditoria.service.AuditService;
import pe.gob.midagri.piip.auditoria.service.IdempotencyService;
import pe.gob.midagri.piip.documentos.dto.DocumentoAuthorizedContext;
import pe.gob.midagri.piip.documentos.dto.PublicacionDocumentoDetail;
import pe.gob.midagri.piip.documentos.dto.PublicarDocumentoRequest;
import pe.gob.midagri.piip.documentos.entity.ClasificacionDocumento;
import pe.gob.midagri.piip.documentos.entity.DocumentoPublicacionEntity;
import pe.gob.midagri.piip.documentos.entity.DocumentoVersionEntity;
import pe.gob.midagri.piip.documentos.repository.DocumentoPublicacionRepository;
import pe.gob.midagri.piip.documentos.repository.DocumentoVersionRepository;
import pe.gob.midagri.piip.documentos.service.impl.PublicacionDocumentoServiceImpl;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService.AsignacionEfectiva;

class PublicacionDocumentoServiceImplTest {

    private final DocumentoVersionRepository versiones = mock(DocumentoVersionRepository.class);
    private final DocumentoPublicacionRepository publicaciones = mock(DocumentoPublicacionRepository.class);
    private final IdempotencyService idempotencia = mock(IdempotencyService.class);
    private final AuditService auditoria = mock(AuditService.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final PublicacionDocumentoServiceImpl service = new PublicacionDocumentoServiceImpl(
            versiones, publicaciones, idempotencia, auditoria, mapper);

    @Test
    void confirmaPublicacionConClasificacionPublicoYFechaDelServidor() throws Exception {
        DocumentoVersionEntity documento = new DocumentoVersionEntity();
        documento.setId(40L);
        documento.setActivo("S");
        documento.setInmutable("S");
        documento.setHashSha256("a".repeat(64));
        documento.setClasificacionValidada(ClasificacionDocumento.PUBLICO);
        when(versiones.findById(40L)).thenReturn(Optional.of(documento));
        when(publicaciones.findByDocumentoId(40L)).thenReturn(Optional.empty());
        DocumentoPublicacionEntity persistida = new DocumentoPublicacionEntity();
        persistida.setId(1L);
        persistida.setDocumentoId(40L);
        persistida.setTituloPublico("Aprobación del Plan Anual");
        persistida.setEvaluadorConfirmadorId(10L);
        persistida.setAsignacionEfectivaId(2L);
        when(publicaciones.saveAndFlush(any())).thenReturn(persistida);
        when(publicaciones.findById(1L)).thenReturn(Optional.of(persistida));
        when(idempotencia.execute(any(), any())).thenAnswer(inv -> {
            IdempotencyService.IdempotentOperation op = inv.getArgument(1);
            IdempotencyService.IdempotencyResponse response = op.execute();
            return new IdempotencyService.IdempotencyResult(
                    response.recursoTipo(), response.recursoId(), response.respuestaJson(), false);
        });

        PublicacionDocumentoDetail resultado = service.confirmarPublicacion(
                contextoEvaluador(), "clave-1",
                new PublicarDocumentoRequest(40L, "Aprobación del Plan Anual",
                        "Oficina de Modernización", "Resumen ejecutivo del plan anual"));

        assertEquals(40L, resultado.documentoId());
        assertEquals("Aprobación del Plan Anual", resultado.tituloPublico());
        verify(auditoria).registrarExito(any());
    }

    @Test
    void rechazaPublicacionSinClasificacionPublicoValidada() {
        DocumentoVersionEntity documento = new DocumentoVersionEntity();
        documento.setId(40L);
        documento.setActivo("S");
        documento.setInmutable("S");
        documento.setHashSha256("a".repeat(64));
        documento.setClasificacionValidada(ClasificacionDocumento.INTERNO);
        when(versiones.findById(40L)).thenReturn(Optional.of(documento));
        when(idempotencia.execute(any(), any())).thenAnswer(inv -> {
            IdempotencyService.IdempotentOperation op = inv.getArgument(1);
            op.execute();
            return null;
        });

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () ->
                service.confirmarPublicacion(contextoEvaluador(), "clave-2",
                        new PublicarDocumentoRequest(40L, "Titulo publico valido",
                                "Oficina", null)));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
        assertEquals("PUBLICACION_REQUIERE_CLASIFICACION_PUBLICO", error.getReason());
        verify(publicaciones, never()).saveAndFlush(any());
    }

    @Test
    void rechazaTituloPublicoConSecuenciaNumerica() {
        DocumentoVersionEntity documento = new DocumentoVersionEntity();
        documento.setId(40L);
        documento.setActivo("S");
        documento.setInmutable("S");
        documento.setHashSha256("a".repeat(64));
        documento.setClasificacionValidada(ClasificacionDocumento.PUBLICO);
        when(versiones.findById(40L)).thenReturn(Optional.of(documento));
        when(idempotencia.execute(any(), any())).thenAnswer(inv -> {
            IdempotencyService.IdempotentOperation op = inv.getArgument(1);
            op.execute();
            return null;
        });

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () ->
                service.confirmarPublicacion(contextoEvaluador(), "clave-3",
                        new PublicarDocumentoRequest(40L, "DNI 123456789 presente",
                                "Oficina", null)));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
        assertEquals("TITULO_PUBLICO_SECUENCIA_NUMERICA", error.getReason());
    }

    @Test
    void requiereIdempotencyKey() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () ->
                service.confirmarPublicacion(contextoEvaluador(), " ",
                        new PublicarDocumentoRequest(40L, "Aprobación del Plan Anual",
                                "Oficina", null)));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals("IDEMPOTENCY_KEY_REQUIRED", error.getReason());
    }

    private static DocumentoAuthorizedContext contextoEvaluador() {
        return new DocumentoAuthorizedContext("sub-1", 10L,
                new AsignacionEfectiva(2L, 10L, 4L, "Evaluador", 7L),
                7L, 40L, "corr-1");
    }
}
