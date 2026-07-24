package pe.gob.midagri.piip.reportes.service;

import pe.gob.midagri.piip.reportes.dto.DestinatarioReporteDetail;
import pe.gob.midagri.piip.reportes.dto.ReporteAprobacionDetail;
import pe.gob.midagri.piip.reportes.dto.ReporteAprobacionRequest;
import pe.gob.midagri.piip.reportes.dto.ReporteRemisionDetail;
import pe.gob.midagri.piip.reportes.dto.ReporteRemisionPage;
import pe.gob.midagri.piip.reportes.dto.ReporteRemisionRequest;
import pe.gob.midagri.piip.reportes.dto.ReporteAuthContext;

import java.util.List;

/**
 * Contrato del servicio de aprobación y remisión de
 * reportes (US8, BR-125, BR-127, BR-128, BR-148).
 * <ul>
 *   <li>La aprobación es potestad de la Oficina de
 *       Modernización y fija la versión exacta más
 *       los destinatarios permitidos.</li>
 *   <li>La remisión es manual y recuperable; se
 *       registra contra destinatarios previamente
 *       aprobados, con resultado y motivo. No
 *       existen remisión automática, correo,
 *       PIDE ni sincronización externa.</li>
 * </ul>
 */
public interface AprobacionRemisionReporteService {

    /**
     * Registra la aprobación formal de la versión
     * indicada, con sus destinatarios. La Oficina
     * de Modernización fija la oficina aprobadora
     * y el documento formal. Una aprobación
     * duplicada de la misma versión produce 409
     * {@code REPORT_VERSION_ALREADY_APPROVED}.
     */
    ReporteAprobacionDetail aprobar(Long idReporte,
            ReporteAprobacionRequest request,
            ReporteAuthContext contexto, String idempotencyKey,
            String payloadJson);

    /**
     * Lista los destinatarios aprobados para un
     * reporte. La Oficina de Modernización los
     * usa para preparar la remisión manual.
     */
    List<DestinatarioReporteDetail> listarDestinatarios(Long idReporte,
            ReporteAuthContext contexto);

    /**
     * Registra la remisión manual contra los
     * destinatarios aprobados de la versión. El
     * resultado debe ser EXITOSA, FALLIDA o
     * PENDIENTE; FALLIDA exige motivo. La
     * remisión de una versión no aprobada
     * produce 409
     * {@code REPORT_VERSION_NOT_APPROVED}.
     */
    ReporteRemisionPage remitir(Long idReporte,
            ReporteRemisionRequest request,
            ReporteAuthContext contexto, String idempotencyKey,
            String payloadJson);

    /**
     * Devuelve el historial de remisiones
     * registradas de un reporte. Permite a la
     * Oficina de Modernización reconstruir la
     * trazabilidad manual sin perder evidencia.
     */
    ReporteRemisionPage consultarRemisiones(Long idReporte,
            Integer idVersion, ReporteAuthContext contexto);
}
