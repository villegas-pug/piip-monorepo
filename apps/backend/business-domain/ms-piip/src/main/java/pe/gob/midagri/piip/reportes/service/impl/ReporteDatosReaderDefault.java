package pe.gob.midagri.piip.reportes.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import pe.gob.midagri.piip.reportes.dto.IndicadorReporte;
import pe.gob.midagri.piip.reportes.dto.ReportFiltros;
import pe.gob.midagri.piip.reportes.dto.TotalDimension;
import pe.gob.midagri.piip.reportes.dto.TotalDimensionItem;
import pe.gob.midagri.piip.reportes.service.ReporteDatosReader;

/**
 * Implementación por defecto del lector de datos
 * del portafolio. Mantiene aislado al módulo
 * reportes: cuando el módulo portafolio publique
 * su agregación, se registrará un bean
 * {@code @Primary} con la fuente canónica
 * (US1..US4) y esta implementación dejará de
 * activarse automáticamente. Mientras tanto, la
 * BR-122 sigue siendo válida (los indicadores
 * con denominador cero se reportan como
 * {@code no aplicable}) y los totales por
 * dimensión reflejan el estado vacío del
 * portafolio en la primera generación.
 */
@Component
public class ReporteDatosReaderDefault implements ReporteDatosReader {

    private static final Logger LOG =
            LoggerFactory.getLogger(ReporteDatosReaderDefault.class);

    @Override
    public List<TotalDimension> totalesBr121(LocalDate fechaCorte,
            ReportFiltros filtros) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("ReporteDatosReaderDefault.totalesBr121 corte={} filtros={}",
                    fechaCorte, filtros);
        }
        return List.of(
                total("tipo", List.of(
                        item("INICIATIVA", "Iniciativas", 0L),
                        item("PROYECTO", "Proyectos", 0L))),
                total("estado", List.of(
                        item("PRESENTADO", "Presentado", 0L),
                        item("INICIATIVA_APROBADA", "Iniciativa aprobada", 0L),
                        item("PROYECTO_EJECUCION", "Proyecto en ejecucion", 0L),
                        item("FINALIZADO", "Finalizado", 0L))),
                total("unidad", List.of()),
                total("fuente", List.of()),
                total("tipoSolucion", List.of()),
                total("producto", List.of()),
                total("cierre", List.of()));
    }

    @Override
    public List<IndicadorReporte> indicadoresBr122(LocalDate fechaCorte,
            ReportFiltros filtros) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("ReporteDatosReaderDefault.indicadoresBr122 corte={} filtros={}",
                    fechaCorte, filtros);
        }
        return List.of(
                IndicadorReporte.calcular("admisibilidad", 0L, 0L,
                        "decisiones admisibles / decisiones de admisibilidad"),
                IndicadorReporte.calcular("aprobacion", 0L, 0L,
                        "iniciativas aprobadas / decisiones de Autoridad sobre iniciativas"),
                IndicadorReporte.calcular("cierre", 0L, 0L,
                        "proyectos FINALIZADO / (FINALIZADO + CANCELADO)"),
                IndicadorReporte.calcular("cumplimientoCiclos", 0L, 0L,
                        "ciclos completos / ciclos aplicables"));
    }

    @Override
    public List<Map<String, Object>> detalleProyectos(LocalDate fechaCorte,
            ReportFiltros filtros) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("ReporteDatosReaderDefault.detalleProyectos corte={} filtros={}",
                    fechaCorte, filtros);
        }
        return List.of();
    }

    private static TotalDimension total(String dimension,
            List<TotalDimensionItem> items) {
        return new TotalDimension(dimension, items);
    }

    private static TotalDimensionItem item(String clave, String etiqueta,
            long total) {
        return new TotalDimensionItem(clave, etiqueta, total);
    }
}
