package pe.gob.midagri.piip.seguridad.dto;

import java.time.LocalDate;
import java.util.List;

public record MatrixVersionDetail(
        Long id, String codigoVersion, Long versionAnteriorId, Long documentoAprobacionVersionId,
        LocalDate vigenteDesde, LocalDate vigenteHasta, boolean activa,
        List<MatrixFunctionRequest> funciones, List<MatrixCombination> combinaciones) {
}
