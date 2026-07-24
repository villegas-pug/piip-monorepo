package pe.gob.midagri.piip.reportes.dto;

import java.util.List;

/**
 * Indicador institucional exigido por BR-122
 * (admisibilidad, aprobación, cierre, cumplimiento
 * de ciclos). Cuando el denominador es cero, el
 * indicador no aplica y se devuelve
 * {@code aplicable=false} sin porcentaje; la
 * serialización omite {@code porcentaje} en ese
 * caso. Si el denominador es positivo, se calcula
 * {@code porcentaje = numerador * 100 / denominador}
 * y se redondea a dos decimales.
 */
public record IndicadorReporte(
        String nombre,
        Long numerador,
        Long denominador,
        Double porcentaje,
        boolean aplicable,
        String detalle) {

    /** Fábrica que aplica la regla de denominador cero del BR-122. */
    public static IndicadorReporte calcular(String nombre, Long numerador,
            Long denominador, String detalle) {
        if (numerador == null || numerador < 0) {
            numerador = 0L;
        }
        if (denominador == null || denominador <= 0) {
            return new IndicadorReporte(nombre, numerador,
                    denominador == null ? 0L : denominador,
                    null, false, detalle);
        }
        double porcentaje = Math.round(
                (numerador * 10000.0d) / denominador) / 100.0d;
        return new IndicadorReporte(nombre, numerador, denominador,
                porcentaje, true, detalle);
    }
}
