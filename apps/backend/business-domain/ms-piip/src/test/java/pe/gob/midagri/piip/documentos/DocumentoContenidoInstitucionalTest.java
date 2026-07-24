package pe.gob.midagri.piip.documentos;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import pe.gob.midagri.piip.documentos.controller.DocumentoController;
import pe.gob.midagri.piip.documentos.dto.ContenidoDocumentoResponse;
import pe.gob.midagri.piip.documentos.dto.DocumentoAuthorizedContext;
import pe.gob.midagri.piip.documentos.entity.ClasificacionDocumento;
import pe.gob.midagri.piip.documentos.service.DocumentoAuthorizedContextResolver;
import pe.gob.midagri.piip.documentos.service.DocumentoService;
import pe.gob.midagri.piip.documentos.service.DocumentoService.ContenidoInstitucional;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService.AsignacionEfectiva;

/**
 * Pruebas MockMvc del endpoint de contenido institucional. Verifica
 * la revalidación efectiva, la cabecera ETag y la respuesta binaria
 * con la cabecera X-Clasificacion-Validada para auditoría del
 * cliente.
 */
@ExtendWith(MockitoExtension.class)
class DocumentoContenidoInstitucionalTest {

    @Mock
    private DocumentoService documentoService;
    @Mock
    private DocumentoAuthorizedContextResolver contextoResolver;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DocumentoController(documentoService, contextoResolver))
                .build();
    }

    @Test
    void devuelveContenidoConCabeceraClasificacionValidada() throws Exception {
        when(contextoResolver.resolver(any(), any(), any(), any(), any(), any()))
                .thenReturn(contexto());
        ContenidoDocumentoResponse meta = new ContenidoDocumentoResponse(
                40L, 30L, 1, "archivo.pdf", "application/pdf", "pdf",
                7L, "a".repeat(64), ClasificacionDocumento.INTERNO,
                LocalDateTime.of(2026, 7, 22, 10, 0), "\"40-1-INTERNO\"");
        when(documentoService.obtenerContenidoInstitucional(any(), eq(40L)))
                .thenReturn(new ContenidoInstitucional(meta, "%PDF-1.4".getBytes()));

        mockMvc.perform(get("/api/v1/documentos/40/contenido")
                        .header("X-Asignacion-Efectiva-Id", "2")
                        .header("X-Unidad-Recurso-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"40-1-INTERNO\""))
                .andExpect(header().string("X-Clasificacion-Validada", "INTERNO"))
                .andExpect(content().bytes("%PDF-1.4".getBytes()));
    }

    @Test
    void respondeNotModifiedCuandoEtagCoincide() throws Exception {
        when(contextoResolver.resolver(any(), any(), any(), any(), any(), any()))
                .thenReturn(contexto());
        ContenidoDocumentoResponse meta = new ContenidoDocumentoResponse(
                40L, 30L, 1, "archivo.pdf", "application/pdf", "pdf",
                7L, "a".repeat(64), ClasificacionDocumento.INTERNO,
                LocalDateTime.of(2026, 7, 22, 10, 0), "\"40-1-INTERNO\"");
        when(documentoService.obtenerContenidoInstitucional(any(), eq(40L)))
                .thenReturn(new ContenidoInstitucional(meta, "%PDF-1.4".getBytes()));

        mockMvc.perform(get("/api/v1/documentos/40/contenido")
                        .header("X-Asignacion-Efectiva-Id", "2")
                        .header("X-Unidad-Recurso-Id", "7")
                        .header("If-None-Match", "\"40-1-INTERNO\""))
                .andExpect(status().isNotModified())
                .andExpect(header().string("ETag", "\"40-1-INTERNO\""))
                .andExpect(jsonPath("$").doesNotExist());
    }

    private static DocumentoAuthorizedContext contexto() {
        return new DocumentoAuthorizedContext("sub-1", 10L,
                new AsignacionEfectiva(2L, 10L, 4L, "Consulta", 7L),
                7L, 40L, "corr-1");
    }
}
