package pe.gob.midagri.piip.documentos;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pe.gob.midagri.piip.auditoria.service.AuditService;
import pe.gob.midagri.piip.auditoria.service.IdempotencyService;
import pe.gob.midagri.piip.documentos.dto.DocumentoAuthorizedContext;
import pe.gob.midagri.piip.documentos.dto.DocumentoEvidenciaApta;
import pe.gob.midagri.piip.documentos.dto.UploadDocumentCommand;
import pe.gob.midagri.piip.documentos.entity.*;
import pe.gob.midagri.piip.documentos.repository.*;
import pe.gob.midagri.piip.documentos.service.DocumentStorage;
import pe.gob.midagri.piip.documentos.service.impl.DocumentoServiceImpl;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService.AsignacionEfectiva;

class DocumentoServiceImplTest {
    private final TipoDocumentoRepository tipos = mock(TipoDocumentoRepository.class);
    private final ExpedienteInstitucionalRepository expedientes = mock(ExpedienteInstitucionalRepository.class);
    private final DocumentoSerieRepository series = mock(DocumentoSerieRepository.class);
    private final DocumentoVersionRepository versiones = mock(DocumentoVersionRepository.class);
    private final IdempotencyService idempotencia = mock(IdempotencyService.class);
    private final AuditService auditoria = mock(AuditService.class);
    private final DocumentoServiceImpl service = new DocumentoServiceImpl(tipos, expedientes, series, versiones,
            idempotencia, auditoria, new ObjectMapper(), mock(DocumentStorage.class));

    @Test
    void conservaBlobYCalculaSha256EnServidorParaDocumentoValido() {
        prepararCarga();
        byte[] contenido = "%PDF-contenido".getBytes(StandardCharsets.US_ASCII);

        var detail = service.cargarEnExpediente(contexto(), "clave-1", comando(contenido));

        assertEquals("e76598a635c3c9395ba36acd84a34238968703957f3988f3231c05e215b9c56b", detail.hashSha256());
        verify(versiones).save(argThat(version -> java.util.Arrays.equals(contenido, version.getContenido())));
        verify(auditoria).registrarExito(any());
    }

    @Test
    void aceptaDocumentoDe100MbMenosUnByte() {
        byte[] debajoDelLimite = new byte[104857599];
        debajoDelLimite[0] = '%'; debajoDelLimite[1] = 'P'; debajoDelLimite[2] = 'D'; debajoDelLimite[3] = 'F';
        prepararCarga();
        assertDoesNotThrow(() -> service.cargarEnExpediente(contexto(), "debajo-limite", comando(debajoDelLimite)));
    }

    @Test
    void aceptaLimiteInclusivoDe100Mb() {
        byte[] limite = new byte[104857600];
        limite[0] = '%'; limite[1] = 'P'; limite[2] = 'D'; limite[3] = 'F';
        prepararCarga();
        assertDoesNotThrow(() -> service.cargarEnExpediente(contexto(), "limite", comando(limite)));
    }

    @Test
    void rechazaDocumentoDe100MbMasUnByteSinPersistir() {
        byte[] excedido = new byte[104857601];
        excedido[0] = '%'; excedido[1] = 'P'; excedido[2] = 'D'; excedido[3] = 'F';
        prepararCarga();
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.cargarEnExpediente(contexto(), "excedido", comando(excedido)));
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, error.getStatusCode());
    }

    @Test
    void rechazaPropietarioDeSerieDistintoYNoPermiteCambiarlo() {
        prepararCarga();
        DocumentoSerieEntity serie = new DocumentoSerieEntity(); serie.setId(50L); serie.setExpedienteInstitucionalId(99L);
        when(series.findById(50L)).thenReturn(Optional.of(serie));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.crearVersion(contexto(), 50L, "clave", "etag", comando("%PDF".getBytes(StandardCharsets.US_ASCII))));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        assertEquals("DOCUMENT_OWNER_IMMUTABLE", error.getReason());
        verifyNoInteractions(versiones);
    }

    @Test
    void validaEvidenciaFormalSinExponerLaEntidadDocumental() {
        DocumentoVersionEntity documento = new DocumentoVersionEntity();
        documento.setId(40L); documento.setTipoDocumentoId(2); documento.setSerieId(30L);
        documento.setActivo("S"); documento.setInmutable("S"); documento.setHashSha256("a".repeat(64));
        TipoDocumentoEntity tipo = new TipoDocumentoEntity();
        tipo.setId(2); tipo.setNombre("Evidencia de Suspensión"); tipo.setActivo("S");
        DocumentoSerieEntity serie = new DocumentoSerieEntity();
        serie.setId(30L); serie.setActiva("S"); serie.setClasificacionValidada(ClasificacionDocumento.INTERNO);
        when(versiones.findById(40L)).thenReturn(Optional.of(documento));
        when(tipos.findById(2)).thenReturn(Optional.of(tipo));
        when(series.findById(30L)).thenReturn(Optional.of(serie));

        DocumentoEvidenciaApta resultado = service.validarEvidencia(40L, "Evidencia de Suspensión");

        assertTrue(resultado.apto());
        assertEquals(40L, resultado.documentoId());
    }

    private void prepararCarga() {
        TipoDocumentoEntity tipo = new TipoDocumentoEntity();
        tipo.setId(1); tipo.setContexto(ContextoTipoDocumento.INSTITUCIONAL); tipo.setActivo("S");
        when(expedientes.findById(20L)).thenReturn(Optional.of(new ExpedienteInstitucionalEntity()));
        when(tipos.findByIdAndActivo(1, "S")).thenReturn(Optional.of(tipo));
        doAnswer(invocation -> { DocumentoSerieEntity s = invocation.getArgument(0); s.setId(30L); return s; }).when(series).save(any());
        doAnswer(invocation -> { DocumentoVersionEntity v = invocation.getArgument(0); v.setId(40L); return v; }).when(versiones).save(any());
        when(idempotencia.execute(any(), any())).thenAnswer(invocation -> {
            IdempotencyService.IdempotencyResponse response = invocation.<IdempotencyService.IdempotentOperation>getArgument(1).execute();
            return new IdempotencyService.IdempotencyResult(response.recursoTipo(), response.recursoId(), response.respuestaJson(), false);
        });
    }

    private DocumentoAuthorizedContext contexto() {
        return new DocumentoAuthorizedContext("sub-1", 10L,
                new AsignacionEfectiva(2L, 10L, 4L, "Responsable", 7L), 7L, 20L, "corr-1");
    }

    private UploadDocumentCommand comando(byte[] contenido) {
        return new UploadDocumentCommand(1, "Aprobación", "archivo.pdf", "application/pdf", ClasificacionDocumento.INTERNO, contenido);
    }
}
