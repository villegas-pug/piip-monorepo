package pe.gob.midagri.piip.seguridad.dto;

import java.time.LocalDate;

/** Opción de asignación que una identidad autenticada puede seleccionar como contexto. */
public record EffectiveAssignmentOption(
        Long id,
        Long matrizCombinacionId,
        String funcion,
        String perfil,
        String unidad,
        LocalDate inicio,
        LocalDate fin,
        String estadoEfectivo) {
}
