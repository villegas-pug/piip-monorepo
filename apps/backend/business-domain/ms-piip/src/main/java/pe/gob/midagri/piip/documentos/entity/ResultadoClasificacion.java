package pe.gob.midagri.piip.documentos.entity;

/**
 * Resultado canónico del registro de una reclasificación documental.
 * Coincide con el CHECK {@code CK_DCH_RESULTADO} del DDL 004.
 */
public enum ResultadoClasificacion {
    APLICADA,
    RECHAZADA,
    REVERTIDA
}
