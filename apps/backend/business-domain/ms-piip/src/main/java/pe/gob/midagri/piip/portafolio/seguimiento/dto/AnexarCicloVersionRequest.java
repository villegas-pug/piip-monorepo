package pe.gob.midagri.piip.portafolio.seguimiento.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud HTTP para anexar una version a un ciclo existente
 * (US4, Constitucion 5.0.0 y DDL
 * {@code 015_ciclos_resultados_cierre.sql}).
 *
 * <p>La operacion es append-only: la fila original nunca se
 * modifica; se inserta una nueva fila en una tabla de versiones
 * del ciclo que mantiene el historial completo con
 * {@code NUMERO_VERSION} incrementado y referencia a la version
 * anterior. El {@code motivo} es obligatorio para trazabilidad.
 */
public record AnexarCicloVersionRequest(
        @NotBlank @Size(max = 2000) String motivo,
        @NotBlank @Size(max = 2000) String objetivos,
        @NotBlank @Size(max = 2000) String actividades,
        @Min(0) @Max(100) Integer avance,
        @Size(max = 2000) String dificultades,
        @Size(max = 2000) String proximasAcciones) {
}
