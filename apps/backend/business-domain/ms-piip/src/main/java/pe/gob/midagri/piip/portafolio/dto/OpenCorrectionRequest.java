package pe.gob.midagri.piip.portafolio.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Comando HTTP para abrir una subsanacion unica por iniciativa.
 *
 * <p>El plazo debe ser estrictamente posterior a la apertura de la subsanacion
 * (invariante determinista CK_SI_PLAZO del incremento 014.1). La lista de
 * incumplimientos es obligatoria: sin ella, la subsanacion no aporta
 * trazabilidad para el Responsable.
 */
public record OpenCorrectionRequest(
        @NotNull LocalDate venceEn,
        @NotEmpty List<String> incumplimientos
) {}
