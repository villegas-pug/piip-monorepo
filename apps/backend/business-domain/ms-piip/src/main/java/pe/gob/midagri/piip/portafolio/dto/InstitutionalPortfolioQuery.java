package pe.gob.midagri.piip.portafolio.dto;

import java.util.List;

/**
 * Criterios de búsqueda y paginación que el módulo
 * {@code consulta} entrega al módulo {@code portafolio} para
 * resolver una consulta institucional por ámbito. La forma se
 * entrega como DTO simple sin entidades JPA; los campos
 * categóricos se transmiten como cadena canónica para mantener
 * la independencia entre módulos.
 *
 * <p>El alcance y la privacidad los aplica el módulo
 * {@code consulta} a partir de la autorización efectiva validada
 * contra Oracle; este DTO solo describe los filtros neutros y los
 * límites de paginación canónicos.
 */
public record InstitutionalPortfolioQuery(
        String tipoRegistro,
        String codigo,
        String nombre,
        String estado,
        Long unidadId,
        Long responsableId,
        java.time.LocalDate fechaDesde,
        java.time.LocalDate fechaHasta,
        String orden,
        int pagina,
        int tamanio,
        List<Long> unidadesVisibles) {

    /** Normaliza el orden a un valor canónico; cualquier valor no
     * reconocido se interpreta como orden por código. */
    public String ordenSeguro() {
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

    /** Asegura una página válida en el dominio canónico. */
    public int paginaNormalizada() {
        return Math.max(0, pagina);
    }

    /** Asegura un tamaño válido y limitado para evitar enumeración. */
    public int tamanioNormalizado() {
        if (tamanio <= 0) {
            return 20;
        }
        return Math.min(tamanio, 100);
    }
}
