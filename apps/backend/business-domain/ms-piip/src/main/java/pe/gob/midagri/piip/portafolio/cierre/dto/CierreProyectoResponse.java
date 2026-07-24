package pe.gob.midagri.piip.portafolio.cierre.dto;

import java.time.LocalDate;
import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;

/** Resultado inmutable del cierre y de la transición a FINALIZADO. */
public record CierreProyectoResponse(
        Long idCierre,
        Long idProyecto,
        EstadoIniciativa estado,
        LocalDate fechaCierre,
        String etag) {
}
