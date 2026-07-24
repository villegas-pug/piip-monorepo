package pe.gob.midagri.piip.portafolio.evaluacion;

import pe.gob.midagri.piip.portafolio.dto.OpenCorrectionRequest;
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.dto.SubsanacionDetail;
import pe.gob.midagri.piip.portafolio.dto.SubsanacionEditCommand;

/**
 * Contrato del modulo de subsanacion de iniciativa (US2).
 *
 * <p>La subsanacion es UNICA por iniciativa: la UK
 * {@code UK_SI_INICIATIVA} lo garantiza a nivel de fila. La persistencia
 * es append-only: las correcciones del Responsable crean nuevas filas;
 * la fila original nunca se borra.
 */
public interface SubsanacionIniciativaService {

    /**
     * Abre la subsanacion unica para la iniciativa. La iniciativa debe
     * seguir en estado PRESENTADO. El plazo debe ser estrictamente
     * posterior a la fecha de apertura (CK_SI_PLAZO).
     */
    SubsanacionDetail abrir(Long iniciativaId, OpenCorrectionRequest request,
            PortafolioAuthContext contexto, String idempotencyKey, String payloadJson);

    /**
     * Edita la subsanacion abierta aplicando solo los campos oficiales
     * 5-12, 22 y 23. La version del ETag (If-Match) debe coincidir con
     * la version actual; cualquier cambio fuera del perimetro se
     * rechaza con FIELD_NOT_EDITABLE.
     */
    SubsanacionDetail editar(Long iniciativaId, SubsanacionEditCommand comando,
            PortafolioAuthContext contexto, Long expectedVersion, String ifMatch,
            String idempotencyKey, String payloadJson);

    /**
     * Cierra la subsanacion registrando la fecha de atencion. La fila
     * permanece para auditoria (no se elimina). Si el plazo ya vencio
     * y la iniciativa fue marcada NO_ADMISIBLE, el cierre es la
     * transicion terminal de la subsanacion.
     */
    SubsanacionDetail cerrar(Long iniciativaId, String motivo,
            PortafolioAuthContext contexto, Long expectedVersion, String ifMatch,
            String idempotencyKey, String payloadJson);
}
