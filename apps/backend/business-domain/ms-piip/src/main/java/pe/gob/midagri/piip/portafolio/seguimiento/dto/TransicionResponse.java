package pe.gob.midagri.piip.portafolio.seguimiento.dto;

import java.time.LocalDateTime;

import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;

/**
 * Detalle canonico de una transicion de estado del proyecto
 * (suspension o cancelacion) conforme a la Constitucion 5.0.0 y
 * al DDL {@code TRANSICION_ESTADO} del incremento 009 + 014.
 * La implementacion completa del servicio se entrega en T074;
 * T072 declara el DTO para estabilizar el contrato HTTP del
 * {@code SeguimientoController}. No expone entidades JPA.
 */
public record TransicionResponse(
        long idTransicion,
        long idProyecto,
        EstadoIniciativa estadoAnterior,
        EstadoIniciativa estadoNuevo,
        long idUsuario,
        LocalDateTime fechaTransicion,
        String observaciones,
        long idDocumento,
        String etag) {
}
