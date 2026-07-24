package pe.gob.midagri.piip.reportes.service;

import pe.gob.midagri.piip.reportes.dto.ExtraordinarioReportRequest;
import pe.gob.midagri.piip.reportes.dto.ReporteArchivoDescarga;
import pe.gob.midagri.piip.reportes.dto.ReporteAuthContext;
import pe.gob.midagri.piip.reportes.dto.ReporteDetail;
import pe.gob.midagri.piip.reportes.dto.ReportOperation;
import pe.gob.midagri.piip.reportes.dto.SemestralReportRequest;

/**
 * Contrato del servicio de generación de reportes
 * institucionales (US8, BR-013, BR-120, BR-121,
 * BR-122, BR-123, BR-126, BR-128, BR-148). Su
 * implementación consolida el snapshot JSON
 * canónico, persiste el reporte, delega la
 * generación de PDF y XLSX a los renderers y
 * mantiene la idempotencia, la auditoría atómica
 * y la autorización efectiva contra Oracle.
 */
public interface GeneracionReporteService {

    /**
     * Genera el reporte semestral oficial (US8,
     * BR-013). El servidor deriva el periodo y la
     * fecha de corte a partir de {@code anio} y
     * {@code semestre}; rechaza cualquier
     * desviación.
     */
    ReportOperation generarSemestral(SemestralReportRequest request,
            ReporteAuthContext contexto, String idempotencyKey,
            String payloadJson);

    /**
     * Genera un reporte extraordinario
     * configurable (US8, BR-120, BR-123). La
     * generación exige la solicitud y la
     * aprobación documentada; el periodo y los
     * filtros viajan en el cuerpo.
     */
    ReportOperation generarExtraordinario(ExtraordinarioReportRequest request,
            ReporteAuthContext contexto, String idempotencyKey,
            String payloadJson);

    /**
     * Devuelve el detalle del reporte, su
     * snapshot, sus indicadores, sus totales y
     * sus archivos. La respuesta se usa en
     * {@code GET /generaciones/{id}}.
     */
    ReporteDetail consultar(Long idReporte, ReporteAuthContext contexto);

    /**
     * Resuelve el archivo PDF/XLSX y su contenido binario para descarga
     * institucional.
     */
    ReporteArchivoDescarga descargarArchivo(Long idReporte, String formato,
            ReporteAuthContext contexto);
}
