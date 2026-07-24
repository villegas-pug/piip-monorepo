package pe.gob.midagri.piip.documentos;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ProblemDetail;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;

import pe.gob.midagri.piip.config.ApiHeaders;
import pe.gob.midagri.piip.config.CorrelationIdFilter;
import pe.gob.midagri.piip.config.ProblemDetailsConfig;
import pe.gob.midagri.piip.documentos.controller.DocumentoController;
import pe.gob.midagri.piip.documentos.dto.AptitudDocumental;
import pe.gob.midagri.piip.documentos.dto.ContenidoDocumentoResponse;
import pe.gob.midagri.piip.documentos.dto.DocumentVersionDetail;
import pe.gob.midagri.piip.documentos.entity.ClasificacionDocumento;
import pe.gob.midagri.piip.documentos.dto.PublicarDocumentoRequest;
import pe.gob.midagri.piip.documentos.exception.DocumentosExceptionHandler;
import pe.gob.midagri.piip.documentos.service.DocumentoService;
import pe.gob.midagri.piip.documentos.service.PublicacionDocumentoService;
import pe.gob.midagri.piip.documentos.dto.DocumentoAuthorizedContext;
import pe.gob.midagri.piip.documentos.service.DocumentoAuthorizedContextResolver;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService.AsignacionEfectiva;

/**
 * Pruebas de contrato MockMvc para el módulo {@code documentos}.
 * Verifica headers canónicos, ETag, idempotencia y Problem Details
 * según los contratos definidos en T118.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("T118 - Documentos: contrato HTTP con headers, ETag, idempotencia y Problem Details")
class DocumentosContratoMvcTest {

    @Mock
    private DocumentoService documentoService;
    @Mock
    private PublicacionDocumentoService publicacionService;
    @Mock
    private DocumentoAuthorizedContextResolver contextoResolver;

    private MockMvc mockMvcDoc;
    private MockMvc mockMvcPub;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class);

        DocumentoController documentoController = new DocumentoController(
                documentoService, contextoResolver);
        DocumentosExceptionHandler advice = new DocumentosExceptionHandler(
                new ProblemDetailsConfig.ProblemDetailsFactory());
        mockMvcDoc = MockMvcBuilders.standaloneSetup(documentoController)
                .setControllerAdvice(advice)
                .addFilters(new CorrelationIdFilter())
                .setMessageConverters(
                        new ByteArrayHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        pe.gob.midagri.piip.documentos.controller.PublicacionDocumentoController pubController =
                new pe.gob.midagri.piip.documentos.controller.PublicacionDocumentoController(
                        publicacionService, contextoResolver);
        mockMvcPub = MockMvcBuilders.standaloneSetup(pubController)
                .setControllerAdvice(advice)
                .addFilters(new CorrelationIdFilter())
                .setMessageConverters(
                        new ByteArrayHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    private static DocumentoAuthorizedContext contextoMock() {
        return new DocumentoAuthorizedContext("sub-1", 10L,
                new AsignacionEfectiva(2L, 10L, 4L, "Evaluador", 7L),
                7L, 40L, "corr-1");
    }

    @Nested
    @DisplayName("Headers canónicos - DocumentoController")
    class HeadersDocumento {

        @Test
        @DisplayName("GET /tipos/{id}/aptitud no exige headers canónicos")
        void getAptitud_sinHeadersRequeridos() throws Exception {
            AptitudDocumental aptitud = new AptitudDocumental(
                    1, "Informe", pe.gob.midagri.piip.documentos.entity.ContextoTipoDocumento.PORTAFOLIO,
                    ClasificacionDocumento.PUBLICO, true, true);
            when(documentoService.obtenerAptitud(1)).thenReturn(aptitud);

            mockMvcDoc.perform(get("/api/v1/documentos/tipos/1/aptitud"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tipoDocumentoId").value(1))
                    .andExpect(jsonPath("$.nombre").value("Informe"))
                    .andExpect(jsonPath("$.clasificacionDefecto").value("PUBLICO"))
                    .andExpect(jsonPath("$.activo").value(true));
        }

        @Test
        @DisplayName("GET /{id}/contenido exige X-Asignacion-Efectiva-Id y X-Unidad-Recurso-Id")
        void getContenido_exigeHeadersAutorizacion() throws Exception {
            when(contextoResolver.resolver(any(), any(), any(), any(), any(), any()))
                    .thenReturn(contextoMock());
            when(documentoService.obtenerContenidoInstitucional(any(), eq(1L)))
                    .thenReturn(new DocumentoService.ContenidoInstitucional(
                            new ContenidoDocumentoResponse(
                                    1L, null, null, "Informe.pdf", "application/pdf",
                                    null, 1024L, "hash1", ClasificacionDocumento.INTERNO,
                                    java.time.LocalDateTime.now(), "\"etag-1\""),
                            new byte[1024]));

            mockMvcDoc.perform(get("/api/v1/documentos/1/contenido")
                            .header(ApiHeaders.EFFECTIVE_ASSIGNMENT_ID, 10L)
                            .header("X-Unidad-Recurso-Id", 7L))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("ETag"))
                    .andExpect(header().exists("X-Clasificacion-Validada"));
        }

        @Test
        @DisplayName("GET /{id}/contenido rechaza sin X-Asignacion-Efectiva-Id con 400")
        void getContenido_sinAsignacion_rechaza400() throws Exception {
            mockMvcDoc.perform(get("/api/v1/documentos/1/contenido")
                            .header("X-Unidad-Recurso-Id", 7L))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        }

        @Test
        @DisplayName("GET /{id}/contenido acepta X-Correlation-Id")
        void getContenido_aceptaCorrelationId() throws Exception {
            when(contextoResolver.resolver(any(), any(), any(), any(), any(), any()))
                    .thenReturn(contextoMock());
            when(documentoService.obtenerContenidoInstitucional(any(), eq(1L)))
                    .thenReturn(new DocumentoService.ContenidoInstitucional(
                            new ContenidoDocumentoResponse(
                                    1L, null, null, "Informe.pdf", "application/pdf",
                                    null, 1024L, "hash2", ClasificacionDocumento.INTERNO,
                                    java.time.LocalDateTime.now(), "\"etag-2\""),
                            new byte[1024]));

            mockMvcDoc.perform(get("/api/v1/documentos/1/contenido")
                            .header(ApiHeaders.EFFECTIVE_ASSIGNMENT_ID, 10L)
                            .header("X-Unidad-Recurso-Id", 7L)
                            .header(ApiHeaders.CORRELATION_ID, "corr-doc-1"))
                    .andExpect(status().isOk())
                    .andExpect(header().exists(ApiHeaders.CORRELATION_ID));
        }
    }

    @Nested
    @DisplayName("ETag - Documentos")
    class ETagDocumentos {

        @Test
        @DisplayName("GET /{id}/contenido devuelve ETag en la respuesta")
        void getContenido_devuelveETag() throws Exception {
            String etag = "\"etag-doc-1\"";
            when(contextoResolver.resolver(any(), any(), any(), any(), any(), any()))
                    .thenReturn(contextoMock());
            when(documentoService.obtenerContenidoInstitucional(any(), eq(1L)))
                    .thenReturn(new DocumentoService.ContenidoInstitucional(
                            new ContenidoDocumentoResponse(
                                    1L, null, null, "Informe.pdf", "application/pdf",
                                    null, 1024L, "hash3", ClasificacionDocumento.INTERNO,
                                    java.time.LocalDateTime.now(), etag),
                            new byte[1024]));

            mockMvcDoc.perform(get("/api/v1/documentos/1/contenido")
                            .header(ApiHeaders.EFFECTIVE_ASSIGNMENT_ID, 10L)
                            .header("X-Unidad-Recurso-Id", 7L))
                    .andExpect(status().isOk())
                    .andExpect(header().string("ETag", etag));
        }

        @Test
        @DisplayName("GET /{id}/contenido con If-None-Match igual devuelve 304")
        void getContenido_conIfNoneMatchigual_devuelve304() throws Exception {
            String etag = "\"etag-doc-304\"";
            when(contextoResolver.resolver(any(), any(), any(), any(), any(), any()))
                    .thenReturn(contextoMock());
            when(documentoService.obtenerContenidoInstitucional(any(), eq(1L)))
                    .thenReturn(new DocumentoService.ContenidoInstitucional(
                            new ContenidoDocumentoResponse(
                                    1L, null, null, "Informe.pdf", "application/pdf",
                                    null, 1024L, "hash4", ClasificacionDocumento.INTERNO,
                                    java.time.LocalDateTime.now(), etag),
                            new byte[1024]));

            mockMvcDoc.perform(get("/api/v1/documentos/1/contenido")
                            .header(ApiHeaders.EFFECTIVE_ASSIGNMENT_ID, 10L)
                            .header("X-Unidad-Recurso-Id", 7L)
                            .header("If-None-Match", etag))
                    .andExpect(status().isNotModified());
        }
    }

    @Nested
    @DisplayName("Headers canónicos - PublicacionDocumentoController")
    class HeadersPublicacion {

        @Test
        @DisplayName("POST /publicaciones exige Idempotency-Key y X-Asignacion-Efectiva-Id")
        void postPublicacion_exigeHeaders() throws Exception {
            when(contextoResolver.resolver(any(), any(), any(), any(), any(), any()))
                    .thenReturn(contextoMock());
            LocalDateTime fechaServidor = LocalDateTime.of(2026, 7, 22, 11, 0);
            when(publicacionService.confirmarPublicacion(any(), any(), any()))
                    .thenReturn(new pe.gob.midagri.piip.documentos.dto.PublicacionDocumentoDetail(
                            7L, 40L, "Aprobación del Plan Anual",
                            ClasificacionDocumento.PUBLICO, 10L, 2L, fechaServidor));

            mockMvcPub.perform(post("/api/v1/documentos/publicaciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(ApiHeaders.EFFECTIVE_ASSIGNMENT_ID, 2L)
                            .header("X-Unidad-Recurso-Id", 7L)
                            .header(ApiHeaders.IDEMPOTENCY_KEY, "pub-key-1")
                            .content(objectMapper.writeValueAsString(new PublicarDocumentoRequest(
                                    40L, "Aprobación", "Oficina", "Resumen ejecutivo"))))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("X-Publicacion-Id"))
                    .andExpect(header().exists("ETag"));
        }

        @Test
        @DisplayName("POST /publicaciones rechaza sin Idempotency-Key con 400")
        void postPublicacion_sinIdempotencyKey_rechaza400() throws Exception {
            mockMvcPub.perform(post("/api/v1/documentos/publicaciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(ApiHeaders.EFFECTIVE_ASSIGNMENT_ID, 2L)
                            .header("X-Unidad-Recurso-Id", 7L)
                            .content(objectMapper.writeValueAsString(new PublicarDocumentoRequest(
                                    40L, "Aprobación", "Oficina", "Resumen ejecutivo"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        }

        @Test
        @DisplayName("POST /publicaciones rechaza sin X-Asignacion-Efectiva-Id con 400")
        void postPublicacion_sinAsignacion_rechaza400() throws Exception {
            mockMvcPub.perform(post("/api/v1/documentos/publicaciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Unidad-Recurso-Id", 7L)
                            .header(ApiHeaders.IDEMPOTENCY_KEY, "pub-key-2")
                            .content(objectMapper.writeValueAsString(new PublicarDocumentoRequest(
                                    40L, "Aprobación", "Oficina", "Resumen ejecutivo"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        }

        @Test
        @DisplayName("POST /publicaciones rechaza sin X-Unidad-Recurso-Id con 400")
        void postPublicacion_sinUnidadRecurso_rechaza400() throws Exception {
            mockMvcPub.perform(post("/api/v1/documentos/publicaciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(ApiHeaders.EFFECTIVE_ASSIGNMENT_ID, 2L)
                            .header(ApiHeaders.IDEMPOTENCY_KEY, "pub-key-3")
                            .content(objectMapper.writeValueAsString(new PublicarDocumentoRequest(
                                    40L, "Aprobación", "Oficina", "Resumen ejecutivo"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        }
    }

    @Nested
    @DisplayName("Idempotencia - Documentos")
    class IdempotenciaDocumentos {

        @Test
        @DisplayName("POST /publicaciones con Idempotency-Key diferente se acepta")
        void postPublicacion_conKeyDiferente_seAcepta() throws Exception {
            when(contextoResolver.resolver(any(), any(), any(), any(), any(), any()))
                    .thenReturn(contextoMock());
            LocalDateTime fechaServidor = LocalDateTime.of(2026, 7, 22, 12, 0);
            when(publicacionService.confirmarPublicacion(any(), eq("pub-key-nuevo"), any()))
                    .thenReturn(new pe.gob.midagri.piip.documentos.dto.PublicacionDocumentoDetail(
                            8L, 41L, "Nuevo Informe",
                            ClasificacionDocumento.PUBLICO, 10L, 2L, fechaServidor));

            mockMvcPub.perform(post("/api/v1/documentos/publicaciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(ApiHeaders.EFFECTIVE_ASSIGNMENT_ID, 2L)
                            .header("X-Unidad-Recurso-Id", 7L)
                            .header(ApiHeaders.IDEMPOTENCY_KEY, "pub-key-nuevo")
                            .content(objectMapper.writeValueAsString(new PublicarDocumentoRequest(
                                    41L, "Nuevo Informe", "Oficina", "Resumen ejecutivo"))))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("POST /publicaciones sin Idempotency-Key devuelve Problem Details")
        void postPublicacion_sinKey_problemDetails() throws Exception {
            mockMvcPub.perform(post("/api/v1/documentos/publicaciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(ApiHeaders.EFFECTIVE_ASSIGNMENT_ID, 2L)
                            .header("X-Unidad-Recurso-Id", 7L)
                            .content(objectMapper.writeValueAsString(new PublicarDocumentoRequest(
                                    40L, "Aprobación", "Oficina", "Resumen ejecutivo"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.code").exists())
                    .andExpect(jsonPath("$.title").exists());
        }
    }

    @Nested
    @DisplayName("Problem Details (RFC 9457) - Documentos")
    class ProblemDetailsDocumentos {

        @Test
        @DisplayName("DocumentosValidationException produce 422 con Problem Details")
        void validacionException_produce422ProblemDetails() throws Exception {
            when(contextoResolver.resolver(any(), any(), any(), any(), any(), any()))
                    .thenReturn(contextoMock());
            when(publicacionService.confirmarPublicacion(any(), any(), any()))
                    .thenThrow(new org.springframework.web.server.ResponseStatusException(
                            org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
                            "DOCUMENT_CLASSIFICATION_INVALID: La clasificación validada no es PUBLICO"));

            mockMvcPub.perform(post("/api/v1/documentos/publicaciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(ApiHeaders.EFFECTIVE_ASSIGNMENT_ID, 2L)
                            .header("X-Unidad-Recurso-Id", 7L)
                            .header(ApiHeaders.IDEMPOTENCY_KEY, "pub-pd-1")
                            .content("{\"documentoId\":40,\"tituloPublico\":\"Aprobación\",\"autoridadPublica\":\"Oficina\",\"resumenPublico\":\"Resumen ejecutivo\"}"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.type").exists())
                    .andExpect(jsonPath("$.title").exists())
                    .andExpect(jsonPath("$.status").value(422))
                    .andExpect(jsonPath("$.code").value("DOCUMENT_CLASSIFICATION_INVALID"));
        }

        @Test
        @DisplayName("El Problem Details incluye traceId para correlación")
        void problemDetails_incluyeTraceId() throws Exception {
            when(contextoResolver.resolver(any(), any(), any(), any(), any(), any()))
                    .thenReturn(contextoMock());
            when(publicacionService.confirmarPublicacion(any(), any(), any()))
                    .thenThrow(new org.springframework.web.server.ResponseStatusException(
                            org.springframework.http.HttpStatus.NOT_FOUND,
                            "DOCUMENT_NOT_FOUND: Documento no encontrado en el ámbito"));

            mockMvcPub.perform(post("/api/v1/documentos/publicaciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(ApiHeaders.EFFECTIVE_ASSIGNMENT_ID, 2L)
                            .header("X-Unidad-Recurso-Id", 7L)
                            .header(ApiHeaders.IDEMPOTENCY_KEY, "pub-pd-2")
                            .header(ApiHeaders.CORRELATION_ID, "corr-pd-doc-1")
                            .content(objectMapper.writeValueAsString(new PublicarDocumentoRequest(
                                    99L, "No Existe", "Oficina", "Resumen ejecutivo"))))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.traceId").exists());
        }
    }

    @Nested
    @DisplayName("Contratos de estado HTTP - Documentos")
    class EstadoHttpDocumentos {

        @Test
        @DisplayName("Publicación confirmada devuelve 201 Created")
        void publicacionConfirmada_devuelve201() throws Exception {
            when(contextoResolver.resolver(any(), any(), any(), any(), any(), any()))
                    .thenReturn(contextoMock());
            LocalDateTime fechaServidor = LocalDateTime.of(2026, 7, 22, 13, 0);
            when(publicacionService.confirmarPublicacion(any(), any(), any()))
                    .thenReturn(new pe.gob.midagri.piip.documentos.dto.PublicacionDocumentoDetail(
                            9L, 42L, "Informe Final",
                            ClasificacionDocumento.PUBLICO, 10L, 2L, fechaServidor));

            mockMvcPub.perform(post("/api/v1/documentos/publicaciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(ApiHeaders.EFFECTIVE_ASSIGNMENT_ID, 2L)
                            .header("X-Unidad-Recurso-Id", 7L)
                            .header(ApiHeaders.IDEMPOTENCY_KEY, "pub-st-1")
                            .content(objectMapper.writeValueAsString(new PublicarDocumentoRequest(
                                    42L, "Informe Final", "Oficina", "Resumen ejecutivo"))))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Alcance denegado devuelve 403 Forbidden")
        void alcanceDenegado_devuelve403() throws Exception {
            when(contextoResolver.resolver(any(), any(), any(), any(), any(), any()))
                    .thenThrow(new org.springframework.web.server.ResponseStatusException(
                            org.springframework.http.HttpStatus.FORBIDDEN,
                            "INSTITUTIONAL_FILE_SCOPE_DENIED"));

            mockMvcPub.perform(post("/api/v1/documentos/publicaciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(ApiHeaders.EFFECTIVE_ASSIGNMENT_ID, 999L)
                            .header("X-Unidad-Recurso-Id", 7L)
                            .header(ApiHeaders.IDEMPOTENCY_KEY, "pub-st-2")
                            .content(objectMapper.writeValueAsString(new PublicarDocumentoRequest(
                                    40L, "Aprobación", "Oficina", "Resumen ejecutivo"))))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        }

        @Test
        @DisplayName("Documento no encontrado devuelve 404")
        void documentoNoEncontrado_devuelve404() throws Exception {
            when(contextoResolver.resolver(any(), any(), any(), any(), any(), any()))
                    .thenReturn(contextoMock());
            when(documentoService.obtenerContenidoInstitucional(any(), eq(999L)))
                    .thenThrow(new org.springframework.web.server.ResponseStatusException(
                            org.springframework.http.HttpStatus.NOT_FOUND,
                            "DOCUMENTO_NOT_FOUND"));

            mockMvcDoc.perform(get("/api/v1/documentos/999/contenido")
                            .header(ApiHeaders.EFFECTIVE_ASSIGNMENT_ID, 10L)
                            .header("X-Unidad-Recurso-Id", 7L))
                    .andExpect(status().isNotFound());
        }
    }
}
