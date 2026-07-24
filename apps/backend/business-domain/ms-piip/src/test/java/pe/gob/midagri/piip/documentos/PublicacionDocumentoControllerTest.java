package pe.gob.midagri.piip.documentos;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import pe.gob.midagri.piip.documentos.controller.PublicacionDocumentoController;
import pe.gob.midagri.piip.documentos.dto.DocumentoAuthorizedContext;
import pe.gob.midagri.piip.documentos.dto.PublicacionDocumentoDetail;
import pe.gob.midagri.piip.documentos.dto.PublicarDocumentoRequest;
import pe.gob.midagri.piip.documentos.entity.ClasificacionDocumento;
import pe.gob.midagri.piip.documentos.service.DocumentoAuthorizedContextResolver;
import pe.gob.midagri.piip.documentos.service.PublicacionDocumentoService;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService.AsignacionEfectiva;

/**
 * Pruebas MockMvc del controlador de publicación. Verifica que
 * la fecha del servidor se expone en la respuesta y que la
 * cabecera Idempotency-Key es obligatoria.
 */
@ExtendWith(MockitoExtension.class)
class PublicacionDocumentoControllerTest {

    @Mock
    private PublicacionDocumentoService service;
    @Mock
    private DocumentoAuthorizedContextResolver contextoResolver;

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PublicacionDocumentoController(service, contextoResolver))
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(mapper))
                .build();
    }

    @Test
    void confirmaPublicacionDevuelve201ConFechaDelServidor() throws Exception {
        when(contextoResolver.resolver(any(), any(), any(), any(), any(), any()))
                .thenReturn(contexto());
        LocalDateTime fechaServidor = LocalDateTime.of(2026, 7, 22, 11, 0);
        when(service.confirmarPublicacion(any(), eq("clave-1"), any()))
                .thenReturn(new PublicacionDocumentoDetail(7L, 40L, "Aprobación del Plan Anual",
                        ClasificacionDocumento.PUBLICO, 10L, 2L, fechaServidor));

        mockMvc.perform(post("/api/v1/documentos/publicaciones")
                        .header("X-Asignacion-Efectiva-Id", "2")
                        .header("X-Unidad-Recurso-Id", "7")
                        .header("Idempotency-Key", "clave-1")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(
                                new PublicarDocumentoRequest(40L, "Aprobación del Plan Anual",
                                        "Oficina de Modernización", "Resumen ejecutivo"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Publicacion-Id", "7"))
                .andExpect(jsonPath("$.documentoId").value(40))
                .andExpect(jsonPath("$.tituloPublico").value("Aprobación del Plan Anual"))
                .andExpect(jsonPath("$.clasificacion").value("PUBLICO"))
                .andExpect(jsonPath("$.fechaPublicacion").value("2026-07-22T11:00:00"));
    }

    private static DocumentoAuthorizedContext contexto() {
        return new DocumentoAuthorizedContext("sub-1", 10L,
                new AsignacionEfectiva(2L, 10L, 4L, "Evaluador", 7L),
                7L, 40L, "corr-1");
    }
}
