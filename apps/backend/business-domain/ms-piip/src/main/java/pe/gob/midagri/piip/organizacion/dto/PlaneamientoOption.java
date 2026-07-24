package pe.gob.midagri.piip.organizacion.dto;
import java.time.LocalDate;
public record PlaneamientoOption(Long id, String codigo, String descripcion, LocalDate vigenteDesde, LocalDate vigenteHasta, boolean activo) {}
