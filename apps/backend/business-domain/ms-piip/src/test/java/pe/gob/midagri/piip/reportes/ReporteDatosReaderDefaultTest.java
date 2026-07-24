package pe.gob.midagri.piip.reportes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import pe.gob.midagri.piip.reportes.dto.IndicadorReporte;
import pe.gob.midagri.piip.reportes.dto.ReportFiltros;
import pe.gob.midagri.piip.reportes.dto.TotalDimension;
import pe.gob.midagri.piip.reportes.service.impl.ReporteDatosReaderDefault;

/**
 * Pruebas del lector de datos por defecto del
 * módulo reportes. Verifica que, sin agregación
 * real del portafolio, los indicadores BR-122 se
 * reportan como no aplicable cuando el denominador
 * es cero y los totales por dimensión devuelven
 * cero. La conexión con el módulo portafolio
 * llegará cuando se publique el agregador canónico
 * (US1..US4); mientras tanto, esta implementación
 * mantiene aislado al módulo reportes.
 */
class ReporteDatosReaderDefaultTest {

    private final ReporteDatosReaderDefault reader = new ReporteDatosReaderDefault();

    @Test
    void totalesBr121_incluyenTodasLasDimensionesCanonica() {
        List<TotalDimension> totales = reader.totalesBr121(
                LocalDate.of(2026, 6, 30), filtrosVacios());
        assertEquals(7, totales.size());
        for (String dimension : List.of("tipo", "estado", "unidad",
                "fuente", "tipoSolucion", "producto", "cierre")) {
            assertNotNull(totales.stream()
                    .filter(t -> t.dimension().equals(dimension))
                    .findFirst().orElse(null));
        }
    }

    @Test
    void indicadoresBr122_sonCuatroSegunBR122() {
        List<IndicadorReporte> indicadores = reader.indicadoresBr122(
                LocalDate.of(2026, 6, 30), filtrosVacios());
        assertEquals(4, indicadores.size());
        for (IndicadorReporte indicador : indicadores) {
            assertFalse(indicador.aplicable(),
                    "Sin agregador, el indicador '" + indicador.nombre()
                            + "' debe ser no aplicable por denominador cero");
            assertNotNull(indicador.detalle());
        }
    }

    @Test
    void detalleProyectos_devuelveVacio_mientrasNoExistaAgregador() {
        assertTrue(reader.detalleProyectos(
                LocalDate.of(2026, 6, 30), filtrosVacios()).isEmpty());
    }

    @Test
    void totalesPorTipo_incluyenIniciativaYProyecto() {
        List<TotalDimension> totales = reader.totalesBr121(
                LocalDate.of(2026, 6, 30), filtrosVacios());
        TotalDimension tipo = totales.stream()
                .filter(t -> t.dimension().equals("tipo"))
                .findFirst().orElseThrow();
        assertEquals(2, tipo.items().size());
        assertTrue(tipo.items().stream()
                .anyMatch(i -> "INICIATIVA".equals(i.clave())));
        assertTrue(tipo.items().stream()
                .anyMatch(i -> "PROYECTO".equals(i.clave())));
    }

    private static ReportFiltros filtrosVacios() {
        return new ReportFiltros(null, null, null, null,
                null, null, null, List.of());
    }
}
