package pe.gob.midagri.piip.consulta;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import pe.gob.midagri.piip.consulta.controller.ConsultaInstitucionalController;
import pe.gob.midagri.piip.consulta.dto.EstadoIniciativaConsulta;
import pe.gob.midagri.piip.consulta.dto.InstitutionalPortfolioDetail;
import pe.gob.midagri.piip.consulta.dto.InstitutionalPortfolioPage;
import pe.gob.midagri.piip.consulta.dto.InstitutionalPortfolioQuery;
import pe.gob.midagri.piip.consulta.dto.InstitutionalPortfolioSummary;
import pe.gob.midagri.piip.consulta.dto.TipoRegistroConsulta;
import pe.gob.midagri.piip.consulta.service.ConsultaInstitucionalService;

/**
 * Pruebas MockMvc del controlador de consulta institucional. El
 * objetivo es verificar que las cabeceras canónicas, la
 * paginación y el ETag se conservan sin exponer entidades JPA
 * ni contenido/descarga documental.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("US7 - Consulta institucional por ámbito y clasificación")
class ConsultaInstitucionalControllerTest {

    @Mock
    private ConsultaInstitucionalService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ConsultaInstitucionalController(service))
                .build();
    }

    @Test
    @DisplayName("devuelve la página de portafolio con ETag y cabeceras canónicas")
    void devuelvePaginaConCabecerasCanonica() throws Exception {
        InstitutionalPortfolioSummary resumen = new InstitutionalPortfolioSummary(
                10L, TipoRegistroConsulta.INICIATIVA, "PIIP-OM-00001", null,
                "Iniciativa de prueba", EstadoIniciativaConsulta.PRESENTADO.name(),
                java.time.LocalDate.of(2026, 7, 22), 1L, "Unidad de Modernización", "OM",
                101L, true, 1L, "\"10-1\"");
        InstitutionalPortfolioPage page = new InstitutionalPortfolioPage(
                List.of(resumen), 0, 20, 1L, 1, "\"p-1\"");
        when(service.buscar(any(InstitutionalPortfolioQuery.class), any()))
                .thenReturn(new ConsultaInstitucionalService.ResultadoConsulta(page, page.items()));

        mockMvc.perform(get("/api/v1/consulta/institucional/portafolio")
                        .header("X-Asignacion-Efectiva-Id", "9")
                        .header("X-Correlation-Id", "corr-1")
                        .param("tipo", "INICIATIVA")
                        .param("estado", "PRESENTADO")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"p-1\""))
                .andExpect(header().string("X-Correlation-Id", "corr-1"))
                .andExpect(jsonPath("$.items[0].id").value(10))
                .andExpect(jsonPath("$.items[0].codigo").value("PIIP-OM-00001"))
                .andExpect(jsonPath("$.items[0].tipoRegistro").value("INICIATIVA"))
                .andExpect(jsonPath("$.items[0].estado").value("PRESENTADO"))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanio").value(20))
                .andExpect(jsonPath("$.etag").value("\"p-1\""));

        verify(service).buscar(any(InstitutionalPortfolioQuery.class), any());
    }

    @Test
    @DisplayName("oculta el responsable cuando el actor no tiene visibilidad ampliada")
    void ocultaResponsableNoVisible() throws Exception {
        InstitutionalPortfolioSummary resumen = new InstitutionalPortfolioSummary(
                11L, TipoRegistroConsulta.PROYECTO, "PIIP-OGTI-00001", null, "Proyecto X",
                EstadoIniciativaConsulta.PROYECTO_EJECUCION.name(),
                java.time.LocalDate.of(2026, 7, 1), 2L, "Unidad OGTI", "OGTI",
                null, false, 1L, "\"11-1\"");
        InstitutionalPortfolioPage page = new InstitutionalPortfolioPage(
                List.of(resumen), 0, 20, 1L, 1, "\"p-2\"");
        when(service.buscar(any(InstitutionalPortfolioQuery.class), any()))
                .thenReturn(new ConsultaInstitucionalService.ResultadoConsulta(page, page.items()));

        mockMvc.perform(get("/api/v1/consulta/institucional/portafolio")
                        .header("X-Asignacion-Efectiva-Id", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].responsableId").doesNotExist())
                .andExpect(jsonPath("$.items[0].puedeVerResponsable").value(false));
    }

    @Test
    @DisplayName("devuelve el detalle institucional con ETag")
    void devuelveDetalleConEtag() throws Exception {
        InstitutionalPortfolioDetail detalle = new InstitutionalPortfolioDetail(
                50L, TipoRegistroConsulta.INICIATIVA, "PIIP-OM-00010", null,
                java.time.LocalDate.of(2026, 7, 22), null,
                "Iniciativa detalle", null, null, null, 101L, null, null, null, null,
                1L, "Unidad de Modernización", "OM",
                EstadoIniciativaConsulta.PRESENTADO, false, null, null, null,
                List.of(), List.of(), List.of(), List.of(), null,
                false, false, false,
                java.time.LocalDateTime.of(2026, 7, 22, 0, 0), 1L, "\"50-1\"");
        when(service.obtenerDetalle(eq(50L), any()))
                .thenReturn(Optional.of(new ConsultaInstitucionalService.DetalleConsulta(detalle, true)));

        mockMvc.perform(get("/api/v1/consulta/institucional/portafolio/50")
                        .header("X-Asignacion-Efectiva-Id", "9")
                        .header("If-Match", "\"50-1\""))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"50-1\""))
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.codigo").value("PIIP-OM-00010"))
                .andExpect(jsonPath("$.estado").value("PRESENTADO"));
    }

    @Test
    @DisplayName("responde 304 cuando la ETag coincide")
    void respondeNotModifiedSiEtagCoincide() throws Exception {
        InstitutionalPortfolioDetail detalle = new InstitutionalPortfolioDetail(
                51L, TipoRegistroConsulta.INICIATIVA, "PIIP-OM-00011", null,
                java.time.LocalDate.of(2026, 7, 22), null,
                "Iniciativa detalle", null, null, null, 101L, null, null, null, null,
                1L, "Unidad de Modernización", "OM",
                EstadoIniciativaConsulta.PRESENTADO, false, null, null, null,
                List.of(), List.of(), List.of(), List.of(), null,
                false, false, false,
                java.time.LocalDateTime.of(2026, 7, 22, 0, 0), 1L, "\"51-1\"");
        when(service.obtenerDetalle(eq(51L), any()))
                .thenReturn(Optional.of(new ConsultaInstitucionalService.DetalleConsulta(detalle, true)));

        mockMvc.perform(get("/api/v1/consulta/institucional/portafolio/51")
                        .header("X-Asignacion-Efectiva-Id", "9")
                        .header("If-None-Match", "\"51-1\""))
                .andExpect(status().isNotModified())
                .andExpect(header().string("ETag", "\"51-1\""));
    }

    @Test
    @DisplayName("responde 404 sin confirmar existencia cuando el registro está fuera de ámbito")
    void respondeNotFoundSiFueraDeAmbito() throws Exception {
        when(service.obtenerDetalle(eq(99L), any()))
                .thenReturn(Optional.of(new ConsultaInstitucionalService.DetalleConsulta(null, false)));

        mockMvc.perform(get("/api/v1/consulta/institucional/portafolio/99")
                        .header("X-Asignacion-Efectiva-Id", "9"))
                .andExpect(status().isNotFound());
    }
}
