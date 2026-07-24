package pe.gob.midagri.piip.portafolio.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Secuencia de código PIIP por año y unidad ejecutora.
 * Formato del código: AAAA-PREFIJO_UNIDAD-NNNNN.
 * Bloqueo pesimista PESSIMISTIC_WRITE para generar códigos secuenciales concurrentes.
 *
 * <p>La restricción única por (ANIO, ID_UNIDAD) es la autoridad para impedir correlativos
 * duplicados cuando dos transacciones concurrentes intentan crear la misma secuencia. La
 * capa de servicio captura la violación transitoria y reintenta bajo el bloqueo pesimista.
 */
@Entity
@Table(
        name = "SECUENCIA_CODIGO",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_SECUENCIA_CODIGO_ANIO_UNIDAD",
                columnNames = {"ANIO", "ID_UNIDAD"}))
@Getter @Setter @NoArgsConstructor
public class SecuenciaCodigoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqSecuenciaCodigo")
    @SequenceGenerator(name = "seqSecuenciaCodigo", sequenceName = "SEQ_SECUENCIA_CODIGO", allocationSize = 1)
    @Column(name = "ID_SECUENCIA", nullable = false)
    private Long id;

    @Column(name = "ANIO", nullable = false)
    private Integer anio;

    @Column(name = "ID_UNIDAD", nullable = false)
    private Long unidadId;

    @Column(name = "ULTIMO_NUMERO", nullable = false)
    private Integer ultimoNumero;
}
