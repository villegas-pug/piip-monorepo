package pe.gob.midagri.piip.consulta.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Criterios de búsqueda y paginación que el módulo
 * {@code consulta} aplica antes de invocar al lector del
 * portafolio. El DTO mantiene la forma canónica de la API
 * pública (enums propios, tipos simples) y se traduce al
 * {@code InstitutionalPortfolioQuery} del módulo
 * {@code portafolio} mediante el conversor del servicio.
 */
public record InstitutionalPortfolioQuery(
        TipoRegistroConsulta tipoRegistro,
        String codigo,
        String nombre,
        String estado,
        Long unidadId,
        Long responsableId,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        String orden,
        int pagina,
        int tamanio) {

    public InstitutionalPortfolioQuery normalizar() {
        int paginaNormalizada = Math.max(0, pagina);
        int tamanioNormalizado;
        if (tamanio <= 0) {
            tamanioNormalizado = 20;
        } else {
            tamanioNormalizado = Math.min(tamanio, 100);
        }
        return new InstitutionalPortfolioQuery(tipoRegistro, codigo, nombre, estado, unidadId,
                responsableId, fechaDesde, fechaHasta, ordenSeguro(orden),
                paginaNormalizada, tamanioNormalizado);
    }

    public static InstitutionalPortfolioQuery normalizar(InstitutionalPortfolioQuery origen) {
        return origen == null ? null : origen.normalizar();
    }

    private static String ordenSeguro(String orden) {
        if (orden == null) {
            return "codigo";
        }
        return switch (orden.toLowerCase()) {
            case "nombre" -> "nombre";
            case "estado" -> "estado";
            case "fechainicio", "fecha_inicio", "fecha" -> "fechaInicio";
            case "codigo" -> "codigo";
            default -> "codigo";
        };
    }

    public String ordenSeguro() {
        return ordenSeguro(this.orden);
    }

    public int paginaNormalizada() {
        return Math.max(0, pagina);
    }

    public int tamanioNormalizado() {
        if (tamanio <= 0) {
            return 20;
        }
        return Math.min(tamanio, 100);
    }

    public List<Long> unidadesVisiblesVacias() {
        return List.of();
    }
}
