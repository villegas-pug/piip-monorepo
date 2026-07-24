package pe.gob.midagri.piip.seguridad.service;

import java.util.List;

import pe.gob.midagri.piip.seguridad.dto.EffectiveAssignmentOption;

/**
 * Autoridad única para comprobar una asignación efectiva almacenada en Oracle.
 * Los perfiles del JWT no intervienen en esta decisión.
 */
public interface AutorizacionEfectivaService {

    /**
     * Lista los contextos propios a partir de la identidad autenticada y de la información efectiva
     * registrada en Oracle. El JWT solo identifica al usuario; no aporta autoridad funcional.
     */
    List<EffectiveAssignmentOption> listarAsignacionesPropias(String sub);

    /**
     * Revalida y bloquea la asignación seleccionada inmediatamente antes de una mutación sensible.
     *
     * @param sub identificador estable de la identidad autenticada
     * @param asignacionId única asignación efectiva seleccionada para la operación
     * @param perfilRequerido perfil canónico requerido por el caso de uso
     * @param unidadRecursoId unidad propietaria exacta del recurso
     * @return contexto efectivo, sin exponer entidades de persistencia
     * @throws org.springframework.web.server.ResponseStatusException si la asignación no es efectiva
     *         o no cubre el perfil y unidad solicitados
     */
    AsignacionEfectiva revalidarParaOperacionSensible(
            String sub, Long asignacionId, String perfilRequerido, Long unidadRecursoId);

    /** Revalida una asignación institucional seleccionada para consultas sin perfil específico. */
    AsignacionEfectiva revalidarAsignacionInstitucional(String sub, Long asignacionId, Long unidadRecursoId);

    /** Contexto mínimo que los módulos consumidores pueden conservar para la operación autorizada. */
    record AsignacionEfectiva(
            Long id, Long usuarioId, Long combinacionMatrizId, String perfil, Long unidadId) {
    }
}
