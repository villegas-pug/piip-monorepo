package pe.gob.midagri.piip.consulta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import pe.gob.midagri.piip.consulta.dto.PublicPortfolioDetail;
import pe.gob.midagri.piip.consulta.dto.PublicPortfolioDocumento;
import pe.gob.midagri.piip.consulta.dto.PublicPortfolioPage;
import pe.gob.midagri.piip.consulta.dto.PublicPortfolioQuery;
import pe.gob.midagri.piip.consulta.dto.PublicPortfolioSummary;
import pe.gob.midagri.piip.consulta.dto.TipoRegistroConsulta;
import pe.gob.midagri.piip.consulta.service.impl.ConsultaPublicaServiceImpl;
import pe.gob.midagri.piip.documentos.entity.ClasificacionDocumento;
import pe.gob.midagri.piip.documentos.entity.DocumentoPublicacionEntity;
import pe.gob.midagri.piip.documentos.entity.DocumentoSerieEntity;
import pe.gob.midagri.piip.documentos.entity.DocumentoVersionEntity;
import pe.gob.midagri.piip.documentos.entity.TipoDocumentoEntity;
import pe.gob.midagri.piip.documentos.repository.DocumentoPublicacionRepository;
import pe.gob.midagri.piip.documentos.repository.DocumentoSerieRepository;
import pe.gob.midagri.piip.documentos.repository.DocumentoVersionRepository;
import pe.gob.midagri.piip.documentos.repository.TipoDocumentoRepository;
import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;
import pe.gob.midagri.piip.portafolio.entity.RegistroPortafolioEntity;
import pe.gob.midagri.piip.portafolio.entity.TipoRegistro;
import pe.gob.midagri.piip.portafolio.repository.RegistroPortafolioRepository;

class ConsultaPublicaServiceImplTest {

    @Test
    void excluirEstadosTerminalesYDevuelveSoloCuatroCampos() {
        RegistroPortafolioRepository registros = mock(RegistroPortafolioRepository.class);
        DocumentoVersionRepository versiones = mock(DocumentoVersionRepository.class);
        DocumentoSerieRepository series = mock(DocumentoSerieRepository.class);
        DocumentoPublicacionRepository publicaciones = mock(DocumentoPublicacionRepository.class);
        TipoDocumentoRepository tipos = mock(TipoDocumentoRepository.class);
        ConsultaPublicaServiceImpl service = new ConsultaPublicaServiceImpl(
                registros, versiones, series, publicaciones, tipos);

        RegistroPortafolioEntity aprobado = portafolio(50L, "PIIP-OM-00050", "Iniciativa pública",
                EstadoIniciativa.INICIATIVA_APROBADA, 1L);
        RegistroPortafolioEntity archivado = portafolio(51L, "PIIP-OM-00051", "Iniciativa archivada",
                EstadoIniciativa.INICIATIVA_ARCHIVADA, 1L);
        when(registros.findAll()).thenReturn(List.of(aprobado, archivado));

        PublicPortfolioPage page = service.buscar(
                new PublicPortfolioQuery(null, null, null, 0, 20));

        assertEquals(1, page.totalElementos());
        PublicPortfolioSummary item = page.items().get(0);
        assertEquals(50L, item.id());
        assertEquals("PIIP-OM-00050", item.codigo());
        assertEquals("Iniciativa pública", item.nombre());
        assertEquals("INICIATIVA_APROBADA", item.estado());
        assertTrue(item.publicaciones().isEmpty());
    }

    @Test
    void incluyeSoloPublicacionesConClasificacionPublicoValidada() {
        RegistroPortafolioRepository registros = mock(RegistroPortafolioRepository.class);
        DocumentoVersionRepository versiones = mock(DocumentoVersionRepository.class);
        DocumentoSerieRepository series = mock(DocumentoSerieRepository.class);
        DocumentoPublicacionRepository publicaciones = mock(DocumentoPublicacionRepository.class);
        TipoDocumentoRepository tipos = mock(TipoDocumentoRepository.class);
        ConsultaPublicaServiceImpl service = new ConsultaPublicaServiceImpl(
                registros, versiones, series, publicaciones, tipos);

        RegistroPortafolioEntity aprobado = portafolio(50L, "PIIP-OM-00050", "Iniciativa pública",
                EstadoIniciativa.INICIATIVA_APROBADA, 1L);
        when(registros.findAll()).thenReturn(List.of(aprobado));

        DocumentoVersionEntity version = new DocumentoVersionEntity();
        version.setId(40L);
        version.setRegistroPortafolioId(50L);
        version.setSerieId(30L);
        version.setActivo("S");
        version.setInmutable("S");
        version.setClasificacionValidada(ClasificacionDocumento.PUBLICO);
        version.setNumeroVersion(1);
        version.setFormato("pdf");
        when(versiones.findByRegistroPortafolioIdAndActivoAndInmutable(50L, "S", "S"))
                .thenReturn(List.of(version));

        DocumentoSerieEntity serie = new DocumentoSerieEntity();
        serie.setId(30L);
        serie.setActiva("S");
        serie.setTipoDocumentoId(2);
        when(series.findAllById(List.of(30L))).thenReturn(List.of(serie));

        TipoDocumentoEntity tipo = new TipoDocumentoEntity();
        tipo.setId(2);
        tipo.setActivo("S");
        tipo.setNombre("Informe de Aprobación");
        when(tipos.findAllById(List.of(2))).thenReturn(List.of(tipo));

        DocumentoPublicacionEntity publicacion = new DocumentoPublicacionEntity();
        publicacion.setId(7L);
        publicacion.setDocumentoId(40L);
        publicacion.setTituloPublico("Aprobación del Plan Anual");
        publicacion.setFechaPublicacion(java.time.LocalDateTime.of(2026, 7, 22, 10, 0));
        when(publicaciones.findByDocumentoIdIn(List.of(40L))).thenReturn(List.of(publicacion));

        PublicPortfolioPage page = service.buscar(
                new PublicPortfolioQuery(null, null, null, 0, 20));

        assertEquals(1, page.items().size());
        List<PublicPortfolioDocumento> publicacionesDto = page.items().get(0).publicaciones();
        assertEquals(1, publicacionesDto.size());
        assertEquals("Informe de Aprobación", publicacionesDto.get(0).tipoDocumental());
        assertEquals("Aprobación del Plan Anual", publicacionesDto.get(0).tituloPublico());
    }

    @Test
    void devolverDetalleVacioSiRegistroNoElegible() {
        RegistroPortafolioRepository registros = mock(RegistroPortafolioRepository.class);
        DocumentoVersionRepository versiones = mock(DocumentoVersionRepository.class);
        DocumentoSerieRepository series = mock(DocumentoSerieRepository.class);
        DocumentoPublicacionRepository publicaciones = mock(DocumentoPublicacionRepository.class);
        TipoDocumentoRepository tipos = mock(TipoDocumentoRepository.class);
        ConsultaPublicaServiceImpl service = new ConsultaPublicaServiceImpl(
                registros, versiones, series, publicaciones, tipos);

        when(registros.findById(51L)).thenReturn(Optional.of(
                portafolio(51L, "PIIP-OM-00051", "Iniciativa archivada",
                        EstadoIniciativa.INICIATIVA_ARCHIVADA, 1L)));

        Optional<PublicPortfolioDetail> resultado = service.obtenerDetalle(51L);
        assertTrue(resultado.isEmpty());
    }

    private static RegistroPortafolioEntity portafolio(Long id, String codigo, String nombre,
            EstadoIniciativa estado, Long unidad) {
        RegistroPortafolioEntity r = new RegistroPortafolioEntity();
        r.setId(id);
        r.setCodigo(codigo);
        r.setNombre(nombre);
        r.setEstado(estado);
        r.setTipoRegistro(TipoRegistro.INICIATIVA);
        r.setUnidadEjecutoraId(unidad);
        r.setFechaInicio(java.time.LocalDate.of(2026, 7, 22));
        r.setSubsanacionActiva("N");
        r.setComponenteDigital("N");
        r.setVersion(1L);
        r.setDescripcion("Problema público");
        r.setObjetivoPei("PEI objetivo");
        r.setActividadPoi("POI actividad");
        r.setResponsableId(99L);
        return r;
    }
}
