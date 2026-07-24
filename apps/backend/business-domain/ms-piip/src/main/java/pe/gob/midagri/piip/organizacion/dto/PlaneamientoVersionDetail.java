package pe.gob.midagri.piip.organizacion.dto;
import java.time.LocalDate;
import java.util.List;
public record PlaneamientoVersionDetail(Long id, String codigoVersion, Long documentoAprobacionVersionId,
        String oficinaAprobadora, LocalDate vigenteDesde, LocalDate vigenteHasta, List<PlaneamientoOption> items) {}
