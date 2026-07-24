package pe.gob.midagri.piip.portafolio.dto;

import java.time.LocalDate;

/** Resultado de una sustitución de Responsable titular ya confirmada. */
public record ResponsibleReplacementDetail(
        Long titularidadAnteriorId,
        Long titularAnteriorId,
        Long titularidadNuevaId,
        Long nuevoResponsableId,
        LocalDate vigenteDesde) {
}
