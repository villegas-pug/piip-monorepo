package pe.gob.midagri.piip.portafolio.service;

import java.util.Optional;

/**
 * Puerto del módulo {@code portafolio} para consultar el prefijo formalmente aprobado de una
 * unidad ejecutora. La constitución exige que el prefijo del código PIIP proceda de un valor
 * formalmente aprobado y nunca de un derivado del nombre, la abreviatura o la jerarquía.
 *
 * <p>El módulo constitucional propietario del catálogo de unidades ejecutoras es
 * {@code seguridad}. Mientras dicho módulo no exponga un servicio oficial para resolver el
 * prefijo (dependencia prevista en tareas posteriores de US6), la implementación por defecto
 * actúa como adaptador interino y queda marcada como {@code NEEDS CLARIFICATION}. Cuando el
 * servicio exista, la implementación deberá consumirlo y este puerto permanecerá estable.
 */
public interface CatalogoUnidadReader {

    /**
     * Devuelve el prefijo formalmente aprobado de la unidad ejecutora indicada.
     *
     * @param unidadId identificador de la unidad ejecutora principal
     * @return prefijo aprobado, presente solo si la unidad existe y tiene un valor aprobado
     */
    Optional<String> prefijoUnidad(Long unidadId);
}
