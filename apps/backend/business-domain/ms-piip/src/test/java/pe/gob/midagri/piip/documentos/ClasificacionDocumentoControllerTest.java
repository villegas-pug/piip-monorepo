package pe.gob.midagri.piip.documentos;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import pe.gob.midagri.piip.documentos.controller.ClasificacionDocumentoController;
import pe.gob.midagri.piip.documentos.dto.ClasificacionHistDetalle;
import pe.gob.midagri.piip.documentos.dto.ReclasificarDocumentoRequest;
import pe.gob.midagri.piip.documentos.dto.ReclasificacionDocumentoResult;
import pe.gob.midagri.piip.documentos.dto.ValidacionClasificacionResult;
import pe.gob.midagri.piip.documentos.dto.ValidarClasificacionRequest;
import pe.gob.midagri.piip.documentos.entity.ClasificacionDocumento;
import pe.gob.midagri.piip.documentos.entity.ResultadoClasificacion;
import pe.gob.midagri.piip.documentos.service.ClasificacionDocumentoService;
import pe.gob.midagri.piip.documentos.service.DocumentoAuthorizedContextResolver;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService.AsignacionEfectiva;
import pe.gob.midagri.piip.documentos.dto.DocumentoAuthorizedContext;

/**
 * Pruebas MockMvc del controlador de clasificación documental.
 * Verifica que se exigen cabeceras canónicas, ETag e If-Match, y
 * que las respuestas devuelven el DTO sin exponer entidades JPA.
 */
@ExtendWith(MockitoExtension.class)
class ClasificacionDocumentoControllerTest {

    @Mock
    private ClasificacionDocumentoService service;
    @Mock
    private DocumentoAuthorizedContextResolver contextoResolver;

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ClasificacionDocumentoController(service, contextoResolver))
                .build();
    }

    @Test
    void validaClasificacionDevuelve200ConEtag() throws Exception {
        when(contextoResolver.resolver(any(), any(), any(), any(), any(), any()))
                .thenReturn(contexto());
        when(service.validarClasificacion(any(), eq(40L), any(), any()))
                .thenReturn(new ValidacionClasificacionResult(40L, null, ClasificacionDocumento.INTERNO,
                        java.time.LocalDateTime.of(2026, 7, 22, 10, 0), 10L, "\"40-1-INTERNO\""));

        mockMvc.perform(post("/api/v1/documentos/40/clasificacion/validacion")
                        .header("X-Asignacion-Efectiva-Id", "2")
                        .header("X-Unidad-Recurso-Id", "7")
                        .header("If-Match", "\"40-1-P\"")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(
                                new ValidarClasificacionRequest(ClasificacionDocumento.INTERNO))))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"40-1-INTERNO\""))
                .andExpect(jsonPath("$.clasificacionValidada").value("INTERNO"));
    }

    @Test
    void reclasificarDevuelve200ConEtag() throws Exception {
        when(contextoResolver.resolver(any(), any(), any(), any(), any(), any()))
                .thenReturn(contexto());
        ClasificacionHistDetalle hist = new ClasificacionHistDetalle(1L, 40L,
                ClasificacionDocumento.INTERNO, ClasificacionDocumento.RESTRINGIDO,
                99L, 10L, 50L, "Refuerzo",
                java.time.LocalDateTime.of(2026, 7, 22, 10, 0), ResultadoClasificacion.APLICADA);
        when(service.reclasificar(any(), eq(40L), any(), any()))
                .thenReturn(new ReclasificacionDocumentoResult(40L,
                        ClasificacionDocumento.INTERNO, ClasificacionDocumento.RESTRINGIDO,
                        "\"40-1-RESTRINGIDO\"", hist));

        mockMvc.perform(post("/api/v1/documentos/40/clasificacion/reclasificacion")
                        .header("X-Asignacion-Efectiva-Id", "2")
                        .header("X-Unidad-Recurso-Id", "7")
                        .header("If-Match", "\"40-1-INTERNO\"")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(
                                new ReclasificarDocumentoRequest(ClasificacionDocumento.RESTRINGIDO,
                                        50L, 99L, "Refuerzo"))))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"40-1-RESTRINGIDO\""))
                .andExpect(jsonPath("$.clasificacionAnterior").value("INTERNO"))
                .andExpect(jsonPath("$.clasificacionNueva").value("RESTRINGIDO"));
    }

    private static DocumentoAuthorizedContext contexto() {
        return new DocumentoAuthorizedContext("sub-1", 10L,
                new AsignacionEfectiva(2L, 10L, 4L, "Evaluador", 7L),
                7L, 40L, "corr-1");
    }
}
