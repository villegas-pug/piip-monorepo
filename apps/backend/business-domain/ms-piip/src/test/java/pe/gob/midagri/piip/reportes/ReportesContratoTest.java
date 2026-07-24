package pe.gob.midagri.piip.reportes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** T104: reglas puras previas a T107/T108, sin anticipar su persistencia. */
class ReportesContratoTest {
    @Test
    void cortesSemestralesSonDeterministasYNoAceptanUnaFechaAlternativa() {
        assertEquals(LocalDate.of(2026, 6, 30), corteSemestral(2026, 1));
        assertEquals(LocalDate.of(2026, 12, 31), corteSemestral(2026, 2));
    }

    @Test
    void indicadorConDenominadorCeroEsNoAplicable() {
        assertFalse(indicador(0, 0).aplicable());
    }

    private static LocalDate corteSemestral(int anio, int semestre) {
        return switch (semestre) {
            case 1 -> LocalDate.of(anio, 6, 30);
            case 2 -> LocalDate.of(anio, 12, 31);
            default -> throw new IllegalArgumentException("SEMESTER_INVALID");
        };
    }

    private static Indicador indicador(long numerador, long denominador) {
        return denominador == 0 ? new Indicador(false, null)
                : new Indicador(true, numerador * 100.0d / denominador);
    }

    private record Indicador(boolean aplicable, Double porcentaje) { }
}
