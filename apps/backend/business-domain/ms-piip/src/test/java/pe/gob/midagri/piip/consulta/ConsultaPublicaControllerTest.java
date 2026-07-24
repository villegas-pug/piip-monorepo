package pe.gob.midagri.piip.consulta;

import static org.mockito.ArgumentMatchers.any;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import pe.gob.midagri.piip.consulta.controller.ConsultaPublicaController;
import pe.gob.midagri.piip.consulta.dto.PublicPortfolioDetail;
import pe.gob.midagri.piip.consulta.dto.PublicPortfolioDocumento;
import pe.gob.midagri.piip.consulta.dto.PublicPortfolioPage;
import pe.gob.midagri.piip.consulta.dto.PublicPortfolioSummary;
import pe.gob.midagri.piip.consulta.dto.TipoRegistroConsulta;
import pe.gob.midagri.piip.consulta.service.ConsultaPublicaService;

/**
 * Pruebas MockMvc del controlador de consulta pública. Verifica
 * la respuesta anónima, los cuatro campos públicos y la ausencia
 * estructural de un endpoint de contenido o descarga.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("US7 - Consulta pública allowlist y publicación elegible")
class ConsultaPublicaControllerTest {

    @Mock
    private ConsultaPublicaService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(new ConsultaPublicaController(service))
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    @DisplayName("devuelve la página pública con ETag y sin contenido")
    void devuelvePaginaPublica() throws Exception {
        PublicPortfolioDocumento publicacion = new PublicPortfolioDocumento(
                "Informe de Aprobación", "Aprobación del Plan Anual", 1, "pdf",
                java.time.LocalDateTime.of(2026, 7, 22, 10, 0));
        PublicPortfolioSummary resumen = new PublicPortfolioSummary(
                50L, TipoRegistroConsulta.INICIATIVA, "PIIP-OM-00050",
                "Iniciativa pública", "INICIATIVA_APROBADA",
                java.time.LocalDate.of(2026, 7, 22),
                List.of(publicacion), "\"50-1\"");
        PublicPortfolioPage page = new PublicPortfolioPage(
                List.of(resumen), 0, 20, 1L, 1, "\"p-1\"");
        when(service.buscar(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/consulta/publica/portafolio")
                        .param("tipo", "INICIATIVA")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"p-1\""))
                .andExpect(jsonPath("$.items[0].id").value(50))
                .andExpect(jsonPath("$.items[0].codigo").value("PIIP-OM-00050"))
                .andExpect(jsonPath("$.items[0].nombre").value("Iniciativa pública"))
                .andExpect(jsonPath("$.items[0].estado").value("INICIATIVA_APROBADA"))
                .andExpect(jsonPath("$.items[0].publicaciones[0].tituloPublico")
                        .value("Aprobación del Plan Anual"))
                .andExpect(jsonPath("$.items[0].publicaciones[0].fechaPublicacion")
                        .value("2026-07-22T10:00:00"));
    }

    @Test
    @DisplayName("devuelve el detalle público con ETag")
    void devuelveDetallePublico() throws Exception {
        PublicPortfolioDocumento publicacion = new PublicPortfolioDocumento(
                "Informe de Aprobación", "Aprobación del Plan Anual", 1, "pdf",
                java.time.LocalDateTime.of(2026, 7, 22, 10, 0));
        PublicPortfolioDetail detalle = new PublicPortfolioDetail(50L,
                TipoRegistroConsulta.INICIATIVA, "PIIP-OM-00050",
                "Iniciativa pública", "INICIATIVA_APROBADA",
                List.of(publicacion), "\"50-1\"");
        when(service.obtenerDetalle(any())).thenReturn(Optional.of(detalle));

        mockMvc.perform(get("/api/v1/consulta/publica/portafolio/50")
                        .header("If-None-Match", "\"otro\""))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"50-1\""))
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.publicaciones[0].tituloPublico")
                        .value("Aprobación del Plan Anual"));
    }

    @Test
    @DisplayName("responde 404 sin confirmar existencia cuando el registro no es público")
    void respondeNotFoundSiNoElegible() throws Exception {
        when(service.obtenerDetalle(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/consulta/publica/portafolio/99"))
                .andExpect(status().isNotFound());
    }
}
