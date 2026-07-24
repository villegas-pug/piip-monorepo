package pe.gob.midagri.piip.portafolio.dto;

/**
 * Tipos de origen canonico de un proyecto directo (US3, Constitucion 5.0.0).
 *
 * <p>El proyecto directo representa una via excepcional que NO omite la evaluacion de
 * iniciativas nuevas; solo aplica a proyectos heredados de un sistema previo a PIIP o a
 * excepciones formalmente autorizadas por la Autoridad. Cada valor exige su propio
 * documento de soporte y se valida en el servicio de aplicacion.
 */
public enum TipoOrigenDirecto {

    /**
     * Proyecto heredado de un sistema previo a PIIP. Acredita inicio previo, acto formal y
     * ejecucion. Exige obligatoriamente {@code codigoOrigen} para identificar el acto o la
     * fuente heredada.
     */
    HEREDADO,

    /**
     * Excepcion formalmente aprobada por la Autoridad. Exige obligatoriamente un documento
     * formal de autorizacion de inicio.
     */
    EXCEPCION_FORMAL
}
