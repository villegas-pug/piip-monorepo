package pe.gob.midagri.piip.portafolio.seguimiento.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mapea la tabla PRESENTACION_PRODUCTO_FINAL introducida por el
 * DDL 015 del portafolio (VIGENTE). Registra un evento append-only
 * de presentacion del producto final: la fila original nunca se
 * modifica, cada nueva presentacion crea una fila con VERSION
 * incrementada y referencia a la anterior.
 *
 * <p>El DDL 015 solo provee el campo DESCRIPCION (resumen
 * canonico de la presentacion). El DTO
 * {@code PresentacionProductoFinalRequest} exige adicionalmente
 * los campos 17 (Documentacion de la gestion), 19 (Resultados
 * clave), 23 (Nota) y el tipo de producto final. Estos campos
 * se serializan en {@code DESCRIPCION} con un formato JSON
 * canonico para preservar la fidelidad del DDL 015 sin inventar
 * columnas; el servicio de aplicacion controla la conversion
 * DTO &lt;-&gt; entity. Marcar estos campos como
 * {@code @Transient} garantiza que Hibernate no los valide
 * contra la tabla durante la fase ddl-auto=validate.
 *
 * <p>La presentacion NO realiza una transicion implicita: el
 * estado del proyecto permanece en PROYECTO_EJECUCION. La
 * decision formal la toma la Autoridad (T058) en una operacion
 * posterior.
 */
@Entity
@Table(name = "PRESENTACION_PRODUCTO_FINAL")
@Getter
@Setter
@NoArgsConstructor
public class PresentacionProductoFinalEntity {

    /** Separador canonico para serializar los campos extendidos en DESCRIPCION. */
    public static final String CAMPO_SEPARADOR = "|";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqPresentacionProductoFinal")
    @SequenceGenerator(name = "seqPresentacionProductoFinal",
            sequenceName = "SEQ_PRESENTACION_PRODUCTO_FINAL", allocationSize = 1)
    @Column(name = "ID_PRESENTACION", nullable = false)
    private Long id;

    @Column(name = "ID_PROYECTO", nullable = false)
    private Long idProyecto;

    @Column(name = "VERSION", nullable = false)
    private int version;

    @Column(name = "ID_VERSION_ANTERIOR")
    private Long idVersionAnterior;

    /**
     * Resumen canonico de la presentacion. Almacena tambien los
     * campos 17/19/23 y el tipo de producto final serializados
     * con el formato {@code TIPO|GESTION|RESULTADOS|NOTA} cuando
     * alguno esta presente; en caso contrario conserva el resumen
     * libre de la presentacion.
     */
    @Column(name = "DESCRIPCION", nullable = false, length = 2000)
    private String descripcion;

    @Column(name = "ID_RESPONSABLE", nullable = false)
    private Long idResponsable;

    @Column(name = "ID_DOCUMENTO_SUSTENTA", nullable = false)
    private Long idDocumentoSustenta;

    @Column(name = "FECHA_PRESENTACION", insertable = false, updatable = false)
    private LocalDateTime fechaPresentacion;

    // -----------------------------------------------------------------
    // Campos extendidos (no persistidos; controlados por el servicio
    // de aplicacion y serializados en DESCRIPCION para preservar la
    // fidelidad del DDL 015).
    // -----------------------------------------------------------------

    @Transient
    private String tipoProductoFinal;

    @Transient
    private String documentacionGestion;

    @Transient
    private String resultadosClave;

    @Transient
    private String nota;
}
