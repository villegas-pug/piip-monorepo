package pe.gob.midagri.piip.consulta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import pe.gob.midagri.piip.auditoria.service.AuditService;
import pe.gob.midagri.piip.auditoria.service.AuditService.AuditAccessCommand;
import pe.gob.midagri.piip.consulta.dto.ConsultaInstitucionalAuthContext;
import pe.gob.midagri.piip.consulta.dto.InstitutionalPortfolioDetail;
import pe.gob.midagri.piip.consulta.dto.InstitutionalPortfolioPage;
import pe.gob.midagri.piip.consulta.dto.InstitutionalPortfolioQuery;
import pe.gob.midagri.piip.consulta.dto.TipoRegistroConsulta;
import pe.gob.midagri.piip.consulta.exception.ConsultaInstitucionalValidationException;
import pe.gob.midagri.piip.consulta.service.ConsultaInstitucionalService.ResultadoConsulta;
import pe.gob.midagri.piip.consulta.service.ConsultaInstitucionalService.DetalleConsulta;
import pe.gob.midagri.piip.consulta.service.impl.ConsultaInstitucionalServiceImpl;
import pe.gob.midagri.piip.documentos.dto.DocumentoInstitucionalMetadata;
import pe.gob.midagri.piip.documentos.entity.ClasificacionDocumento;
import pe.gob.midagri.piip.documentos.service.DocumentoInstitucionalReader;
import pe.gob.midagri.piip.portafolio.dto.InstitutionalPortfolioProjection;
import pe.gob.midagri.piip.portafolio.entity.TipoRegistro;
import pe.gob.midagri.piip.portafolio.service.InstitutionalPortfolioReader;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService.AsignacionEfectiva;

@ExtendWith(MockitoExtension.class)
@DisplayName("US7 - Consulta institucional: privacidad y auditoría")
class ConsultaInstitucionalServiceImplTest {

    @Mock
    private InstitutionalPortfolioReader portafolioReader;

    @Mock
    private DocumentoInstitucionalReader documentoReader;

    @Mock
    private AutorizacionEfectivaService autorizacion;

    @Mock
    private AuditService auditoria;

    private ConsultaInstitucionalServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ConsultaInstitucionalServiceImpl(portafolioReader, documentoReader,
                autorizacion, auditoria);
    }

    @Test
    @DisplayName("rechaza la búsqueda cuando falta la asignación efectiva")
    void rechazaBusquedaSinAsignacionEfectiva() {
        ConsultaInstitucionalAuthContext contexto = new ConsultaInstitucionalAuthContext(
                "sub-actor", null, null, "Consulta", "corr-1", null);

        ConsultaInstitucionalValidationException ex = assertThrows(
                ConsultaInstitucionalValidationException.class,
                () -> service.buscar(new InstitutionalPortfolioQuery(null, null, null, null,
                        null, null, null, null, null, 0, 20), contexto));
        assertTrue(ex.getReason().contains("CONSULTA_ASIGNACION_REQUERIDA"));
        verify(portafolioReader, never()).buscar(any());
    }

    @Test
    @DisplayName("deniega y audita cuando la autorización efectiva falla")
    void deniegaCuandoAutorizacionFalla() {
        ConsultaInstitucionalAuthContext contexto = new ConsultaInstitucionalAuthContext(
                "sub-actor", 9L, 9L, "Consulta", "corr-1", List.of(1L));
        when(autorizacion.revalidarAsignacionInstitucional("sub-actor", 9L, 1L))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "ASSIGNMENT_SCOPE_DENIED"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.buscar(new InstitutionalPortfolioQuery(null, null, null, null,
                        null, null, null, null, null, 0, 20), contexto));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(portafolioReader, never()).buscar(any());
        verify(auditoria).registrarAccesoDenegado(any(AuditAccessCommand.class));
    }

    @Test
    @DisplayName("devuelve la página y oculta el responsable para el perfil Consulta")
    void devuelvePaginaOcultandoResponsableParaConsulta() {
        ConsultaInstitucionalAuthContext contexto = new ConsultaInstitucionalAuthContext(
                "sub-actor", 7L, 9L, "Consulta", "corr-1", List.of(1L, 2L));
        when(autorizacion.revalidarAsignacionInstitucional("sub-actor", 9L, 1L))
                .thenReturn(new AsignacionEfectiva(9L, 7L, 1L, "Consulta", 1L));
        pe.gob.midagri.piip.portafolio.dto.InstitutionalPortfolioPage pagina =
                new pe.gob.midagri.piip.portafolio.dto.InstitutionalPortfolioPage(
                List.of(new pe.gob.midagri.piip.portafolio.dto.InstitutionalPortfolioPage.InstitutionalPortfolioSummary(
                        10L, TipoRegistro.INICIATIVA, "PIIP-OM-00001", null,
                        "Iniciativa", "PRESENTADO",
                        java.time.LocalDate.of(2026, 7, 22), 1L, "Unidad", "OM", 101L,
                        1L, "\"10-1\"")),
                0, 20, 1L, 1);
        when(portafolioReader.buscar(any())).thenReturn(pagina);

        ResultadoConsulta resultado = service.buscar(new InstitutionalPortfolioQuery(
                TipoRegistroConsulta.INICIATIVA, null, null, "PRESENTADO",
                null, null, null, null, "codigo", 0, 20), contexto);
        InstitutionalPortfolioPage page = resultado.page();
        assertNotNull(page);
        assertEquals(1, page.items().size());
        assertEquals(10L, page.items().get(0).id());
        assertNull(page.items().get(0).responsableId());
        assertFalse(page.items().get(0).puedeVerResponsable());
        verify(auditoria).registrarAccesoExitoso(any(AuditAccessCommand.class));
    }

    @Test
    @DisplayName("muestra el responsable cuando el actor es administrador")
    void muestraResponsableParaAdministrador() {
        ConsultaInstitucionalAuthContext contexto = new ConsultaInstitucionalAuthContext(
                "sub-actor", 7L, 9L, "GlobalAdmin", "corr-1", List.of(1L));
        when(autorizacion.revalidarAsignacionInstitucional("sub-actor", 9L, 1L))
                .thenReturn(new AsignacionEfectiva(9L, 7L, 1L, "GlobalAdmin", 1L));
        pe.gob.midagri.piip.portafolio.dto.InstitutionalPortfolioPage pagina =
                new pe.gob.midagri.piip.portafolio.dto.InstitutionalPortfolioPage(
                List.of(new pe.gob.midagri.piip.portafolio.dto.InstitutionalPortfolioPage.InstitutionalPortfolioSummary(
                        20L, TipoRegistro.PROYECTO, "PIIP-OGTI-00001", null,
                        "Proyecto", "PROYECTO_EJECUCION",
                        java.time.LocalDate.of(2026, 7, 22), 1L, "Unidad", "OM", 200L,
                        1L, "\"20-1\"")),
                0, 20, 1L, 1);
        when(portafolioReader.buscar(any())).thenReturn(pagina);

        ResultadoConsulta resultado = service.buscar(new InstitutionalPortfolioQuery(
                null, null, null, null, null, null, null, null, null, 0, 20), contexto);
        assertEquals(200L, resultado.page().items().get(0).responsableId());
        assertTrue(resultado.page().items().get(0).puedeVerResponsable());
    }

    @Test
    @DisplayName("devuelve el detalle con documentos visibles y oculta los no visibles")
    void devuelveDetalleConDocumentosVisibles() {
        ConsultaInstitucionalAuthContext contexto = new ConsultaInstitucionalAuthContext(
                "sub-actor", 7L, 9L, "Consulta", "corr-1", List.of(1L));
        when(autorizacion.revalidarAsignacionInstitucional("sub-actor", 9L, 1L))
                .thenReturn(new AsignacionEfectiva(9L, 7L, 1L, "Consulta", 1L));
        InstitutionalPortfolioProjection proyeccion = new InstitutionalPortfolioProjection(
                50L, "INICIATIVA", "PIIP-OM-00010", null,
                java.time.LocalDate.of(2026, 7, 22), null,
                "Iniciativa", "POTENCIAL_ADAPTABLE", "FICHA_INICIATIVA",
                null, 101L, null, null, null, null,
                1L, "PRESENTADO", "N", null, null, null,
                1L, java.time.LocalDateTime.of(2026, 7, 22, 0, 0),
                null, null,
                List.of(), List.of(), List.of(), List.of());
        when(portafolioReader.obtener(eq(50L), anyList())).thenReturn(Optional.of(proyeccion));
        List<DocumentoInstitucionalMetadata> documentos = List.of(
                new DocumentoInstitucionalMetadata(
                        1001L, 501L, 1, "Ficha", "ficha.pdf", "pdf",
                        "application/pdf", 2048L, "abc",
                        ClasificacionDocumento.PUBLICO, ClasificacionDocumento.PUBLICO,
                        "FichaIniciativaInnovacionPublica", "PORTAFOLIO", true,
                        java.time.LocalDateTime.of(2026, 7, 22, 0, 0), 7L, "\"1001-1\""));
        when(documentoReader.listarPorRegistro(50L)).thenReturn(documentos);

        Optional<DetalleConsulta> resultado = service.obtenerDetalle(50L, contexto);
        assertTrue(resultado.isPresent());
        assertTrue(resultado.get().visible());
        InstitutionalPortfolioDetail detalle = resultado.get().detalle();
        assertNotNull(detalle);
        assertEquals(50L, detalle.id());
        assertEquals(1, detalle.documentos().size());
        assertTrue(detalle.documentos().get(0).puedeConsultarContenido());
        verify(auditoria).registrarAccesoExitoso(any(AuditAccessCommand.class));
    }

    @Test
    @DisplayName("responde como no visible sin confirmar existencia fuera de ámbito")
    void respondeNoVisibleFueraDeAmbito() {
        ConsultaInstitucionalAuthContext contexto = new ConsultaInstitucionalAuthContext(
                "sub-actor", 7L, 9L, "Consulta", "corr-1", List.of(1L));
        when(autorizacion.revalidarAsignacionInstitucional("sub-actor", 9L, 1L))
                .thenReturn(new AsignacionEfectiva(9L, 7L, 1L, "Consulta", 1L));
        when(portafolioReader.obtener(eq(99L), anyList())).thenReturn(Optional.empty());

        Optional<DetalleConsulta> resultado = service.obtenerDetalle(99L, contexto);
        assertTrue(resultado.isPresent());
        assertFalse(resultado.get().visible());
        assertNull(resultado.get().detalle());
        verify(documentoReader, never()).listarPorRegistro(any());
    }
}
