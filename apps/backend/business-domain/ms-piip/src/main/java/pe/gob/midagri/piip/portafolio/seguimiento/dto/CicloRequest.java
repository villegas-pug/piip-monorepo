package pe.gob.midagri.piip.portafolio.seguimiento.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Solicitud HTTP para registrar la primera version de un ciclo
 * quincenal (US4, Constitucion 5.0.0, DDL
 * {@code 015_ciclos_resultados_cierre.sql} y contrato
 * {@code specs/001-gestionar-portafolio-innovacion/contracts/portafolio.md}).
 *
 * <p>El {@code periodo} debe cumplir la expresion regular canonica
 * {@code ^[0-9]{4}-Q[1-4]-S[1-2]$} exigida por la CHECK
 * {@code CK_CP_PERIODO} del DDL 015. El {@code avance}, cuando se
 * informa, debe estar en el rango [0, 100] validado por la CHECK
 * {@code CK_CP_AVANCE}. Los campos {@code objetivos},
 * {@code actividades} y {@code avance} son obligatorios para que el
 * ciclo sea completo (regla CYCLE_INCOMPLETE).
 */
public record CicloRequest(
        @NotBlank @Pattern(regexp = "^[0-9]{4}-Q[1-4]-S[1-2]$",
                message = "INVALID_PERIOD_FORMAT") String periodo,
        @NotBlank @Size(max = 2000) String objetivos,
        @NotBlank @Size(max = 2000) String actividades,
        @Min(0) @Max(100) Integer avance,
        @Size(max = 2000) String dificultades,
        @Size(max = 2000) String proximasAcciones) {
}
