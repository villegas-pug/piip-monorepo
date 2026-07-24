package pe.gob.midagri.piip.portafolio.cierre.dto;

import java.time.LocalDateTime;

import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;

/** Resultado de la transición canónica originada por una decisión de producto. */
public record DecisionProductoFinalResponse(
        Long idProyecto,
        EstadoIniciativa estado,
        String tipoProductoFinal,
        Long idTransicion,
        LocalDateTime fechaDecision,
        String etag) {
}
