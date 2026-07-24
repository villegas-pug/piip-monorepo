package pe.gob.midagri.piip.portafolio.seguimiento.dto;

/**
 * Detalle canonico de una version de la planificacion de un proyecto
 * (US4, Constitucion 5.0.0 y DDL {@code 015_ciclos_resultados_cierre.sql}).
 *
 * <p>Es la representacion HTTP que retorna el servicio de seguimiento
 * y que consume el {@code SeguimientoController} para emitir 201
 * con ETag. Su contrato no expone entidades JPA.
 *
 * <p>El servicio tambien reutiliza este DTO como salida de la
 * edicion de campos 17, 19 y 23 (responsabilidad de T073), donde
 * los campos de planificacion reflejan la consolidacion del
 * Responsable durante {@code PROYECTO_EJECUCION}.
 */
public record PlanificacionResponse(
        long idPlanificacion,
        long idProyecto,
        String alcance,
        String objetivos,
        String entregables,
        String periodos,
        int version,
        long idVersionAnterior,
        String cerrado,
        String etag) {
}
