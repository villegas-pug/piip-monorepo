package pe.gob.midagri.piip.portafolio.transicion;

import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;

/**
 * DTO HTTP de solicitud para confirmar una transicion de estado de iniciativa
 * conforme a la Constitucion 5.0.0 (tabla "Transiciones controladas
 * iniciales") y al script DDL {@code 014_evaluacion_transiciones.sql}.
 *
 * <p>El cliente aporta el destino canonico, la observacion (obligatoria para
 * varios destinos), el identificador del documento formal que sirve como
 * evidencia y la cabecera {@code If-Match} derivada del ETag devuelto en
 * la consulta previa. El servicio evalua localmente la transicion, revalida
 * la autorizacion efectiva Oracle, registra el evento en
 * {@code TRANSICION_ESTADO} (append-only) y emite la auditoria atomica.
 */
public record TransicionCommand(
        EstadoIniciativa destino,
        String observaciones,
        Long documentoRefId,
        String ifMatch,
        String tipoProductoFinal) {

    public TransicionCommand(EstadoIniciativa destino, String observaciones,
            Long documentoRefId, String ifMatch) {
        this(destino, observaciones, documentoRefId, ifMatch, null);
    }
}
