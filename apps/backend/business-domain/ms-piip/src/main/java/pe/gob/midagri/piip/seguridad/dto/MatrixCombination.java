package pe.gob.midagri.piip.seguridad.dto;

import java.time.LocalDate;

public record MatrixCombination(
        Long id, Long matrizVersionId, String funcion, String perfil, Long unidadId,
        LocalDate vigenteDesde, LocalDate vigenteHasta, boolean activa,
        Long documentoAprobacionVersionId, Long aprobadorUsuarioId, Long registradorUsuarioId) {
}
