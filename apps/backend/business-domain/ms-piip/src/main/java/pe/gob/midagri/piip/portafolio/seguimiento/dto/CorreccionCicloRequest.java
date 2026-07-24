package pe.gob.midagri.piip.portafolio.seguimiento.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud HTTP para corregir un ciclo existente creando una
 * nueva version append-only (US4, Constitucion 5.0.0 y DDL
 * {@code 015_ciclos_resultados_cierre.sql}).
 *
 * <p>La correccion nunca actualiza la fila original: inserta una
 * nueva fila en {@code CICLO_PROYECTO} con {@code NUMERO_VERSION}
 * incrementado, conserva la fila cerrada y enlaza ambas mediante
 * {@code ID_VERSION_ANTERIOR}. El {@code motivo} es obligatorio y
 * alimenta la trazabilidad del cambio.
 */
public record CorreccionCicloRequest(
        @NotBlank @Size(max = 2000) String motivo,
        @NotBlank @Size(max = 2000) String objetivos,
        @NotBlank @Size(max = 2000) String actividades,
        @Min(0) @Max(100) Integer avance,
        @Size(max = 2000) String dificultades,
        @Size(max = 2000) String proximasAcciones) {
}
