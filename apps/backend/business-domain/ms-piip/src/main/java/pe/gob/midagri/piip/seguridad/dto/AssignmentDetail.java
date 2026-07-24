package pe.gob.midagri.piip.seguridad.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AssignmentDetail(Long id, Long usuarioId, Long matrizCombinacionId, String perfil,
        Long unidadId, LocalDate fechaInicio, LocalDate fechaFin, Long documentoFormalVersionId,
        LocalDateTime revocadaEn, Long version) {}
