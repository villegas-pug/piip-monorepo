package pe.gob.midagri.piip.portafolio.seguimiento.service;

import java.util.List;

import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.CicloRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.CorreccionCicloRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.CicloResponse;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.EditarCamposEditablesRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.PlanificacionRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.PlanificacionResponse;

/**
 * Servicio de seguimiento del proyecto (US4, Constitucion 5.0.0,
 * DDL {@code 015_ciclos_resultados_cierre.sql} y contrato
 * {@code specs/001-gestionar-portafolio-innovacion/contracts/portafolio.md}).
 *
 * <p>Consolida:
 * <ul>
 *   <li>La planificacion del proyecto (alcance, objetivos,
 *       entregables y periodos) con version append-only y fila
 *       anterior conservada.</li>
 *   <li>La apertura y correccion de los ciclos quincenales
 *       obligatorios, donde cada ciclo registra objetivos,
 *       actividades, avance, dificultades, proximas acciones y
 *       evidencias relacionadas.</li>
 *   <li>El cierre inmutable de un ciclo; la version cerrada
 *       permanece sin cambios y la siguiente correccion crea una
 *       nueva version trazable.</li>
 *   <li>La edicion de los campos editables 17, 19 y 23 durante
 *       {@code PROYECTO_EJECUCION} (T073, implementacion
 *       pendiente fuera del alcance de T072).</li>
 * </ul>
 *
 * <p>El contrato es una interfaz; el servicio nunca retorna
 * entidades JPA y exige autorizacion efectiva Oracle en cada
 * operacion.
 */
public interface SeguimientoProyectoService {

    /**
     * Registra la planificacion del proyecto (primera version).
     * Exige proyecto en {@code PROYECTO_EJECUCION} y
     * responsabilidad efectiva del {@code Responsable} titular.
     * Devuelve 422 {@code VALIDATION_FAILED} si la planificacion
     * ya existe; cada correccion posterior invoca el endpoint
     * equivalente de T073 con la cabecera {@code If-Match}.
     */
    PlanificacionResponse registrarPlanificacion(long proyectoId,
            PlanificacionRequest request, PortafolioAuthContext ctx,
            String idempotencyKey, String payloadJson);

    /** Lista las versiones append-only de planificación autorizadas. */
    List<PlanificacionResponse> listarPlanificaciones(long proyectoId,
            PortafolioAuthContext ctx);

    /**
     * Registra la primera version de un ciclo quincenal del
     * proyecto con todos los campos editables (objetivos,
     * actividades, avance, dificultades, proximas acciones).
     * Devuelve 422 {@code INVALID_PERIOD_FORMAT} si el periodo no
     * cumple la CHECK {@code CK_CP_PERIODO} del DDL 015;
     * {@code CYCLE_INCOMPLETE} si faltan objetivos, actividades o
     * avance; {@code CYCLE_AVANCE_OUT_OF_RANGE} si el avance
     * esta fuera del rango [0, 100].
     */
    CicloResponse registrarCiclo(long proyectoId, CicloRequest request,
            PortafolioAuthContext ctx, String idempotencyKey, String payloadJson);

    /** Lista la última versión de cada ciclo del proyecto autorizado. */
    List<CicloResponse> listarCiclos(long proyectoId, PortafolioAuthContext ctx);

    /**
     * Corrige un ciclo existente creando una nueva version
     * append-only. La fila original nunca se modifica.
     */
    CicloResponse corregirCiclo(long proyectoId, long cicloId,
            CorreccionCicloRequest request, PortafolioAuthContext ctx,
            String idempotencyKey, String payloadJson);

    /** Lista la cadena completa de versiones de un ciclo autorizado. */
    List<CicloResponse> listarVersionesCiclo(long proyectoId, long cicloId,
            PortafolioAuthContext ctx);

    /**
     * Cierra el ciclo indicado fijando la fecha de cierre. Un
     * ciclo ya cerrado se rechaza con 409
     * {@code CYCLE_ALREADY_CLOSED}.
     */
    void cerrarCiclo(long proyectoId, long cicloId, PortafolioAuthContext ctx,
            String idempotencyKey, String payloadJson);

    /**
     * Edita los campos oficiales editables 17, 19 y 23 del
     * proyecto durante {@code PROYECTO_EJECUCION}. Implementacion
     * completa entregada en T073; T072 declara el metodo con
     * {@code UnsupportedOperationException} como contrato
     * estabilizador del {@code SeguimientoController}.
     */
    PlanificacionResponse editarCamposEditables(long proyectoId,
            EditarCamposEditablesRequest request, PortafolioAuthContext ctx,
            String idempotencyKey, String payloadJson);
}
