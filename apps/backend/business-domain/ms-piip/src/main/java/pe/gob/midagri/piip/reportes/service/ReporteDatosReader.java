package pe.gob.midagri.piip.reportes.service;

import java.util.List;
import java.util.Map;

import pe.gob.midagri.piip.reportes.dto.IndicadorReporte;
import pe.gob.midagri.piip.reportes.dto.ReportFiltros;
import pe.gob.midagri.piip.reportes.dto.TotalDimension;

/**
 * Puerto que aísla al módulo reportes del acceso
 * directo a los agregados del portafolio. La
 * arquitectura modular exige que la comunicación
 * entre módulos se haga por servicios, no por
 * repositorios. El módulo portafolio provee la
 * implementación canónica (T107/T108); cuando no
 * se ha conectado, el módulo reportes opera con
 * una implementación por defecto que devuelve
 * agregados en cero, manteniendo la coherencia
 * con la constitución y permitiendo pruebas
 * aisladas sin acoplamiento a Oracle.
 */
public interface ReporteDatosReader {

    /** Totales por dimensión (BR-121) del portafolio para el corte y filtros dados. */
    List<TotalDimension> totalesBr121(java.time.LocalDate fechaCorte,
            ReportFiltros filtros);

    /** Indicadores BR-122 del portafolio para el corte y filtros dados. */
    List<IndicadorReporte> indicadoresBr122(java.time.LocalDate fechaCorte,
            ReportFiltros filtros);

    /** Detalle plano por proyecto que alimenta el XLSX y el PDF. */
    List<Map<String, Object>> detalleProyectos(java.time.LocalDate fechaCorte,
            ReportFiltros filtros);
}
