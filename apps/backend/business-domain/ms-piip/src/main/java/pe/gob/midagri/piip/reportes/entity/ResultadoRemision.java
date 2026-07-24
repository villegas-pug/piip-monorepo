package pe.gob.midagri.piip.reportes.entity;

/**
 * Resultado posible de una remisión registrada
 * manualmente (BR-128 y DDL 017, CHECK
 * {@code CK_RREM_RESULTADO}). La remisión
 * automática, el correo y los conectores externos
 * están fuera del alcance constitucional; este
 * enum solo refleja la evidencia registrada por
 * la Oficina de Modernización.
 */
public enum ResultadoRemision {
    EXITOSA,
    FALLIDA,
    PENDIENTE
}
