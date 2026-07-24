package pe.gob.midagri.piip.portafolio.evaluacion;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mapea la tabla EVALUACION_INICIATIVA del incremento 014.
 *
 * <p>Registra la decision de admisibilidad del Evaluador con su rol y
 * unidad efectivos en el momento de la transicion. La UK
 * {@code UK_EI_INICIATIVA} garantiza una sola fila por iniciativa;
 * correcciones posteriores de la opinion tecnica crean una nueva
 * version documental sin reemplazar esta fila (historial append-only).
 */
@Entity
@Table(name = "EVALUACION_INICIATIVA")
@Getter @Setter @NoArgsConstructor
public class EvaluacionIniciativaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqEvaluacionIniciativa")
    @SequenceGenerator(name = "seqEvaluacionIniciativa", sequenceName = "SEQ_EVALUACION_INICIATIVA", allocationSize = 1)
    @Column(name = "ID_EVALUACION", nullable = false)
    private Long id;

    @Column(name = "ID_INICIATIVA", nullable = false)
    private Long iniciativaId;

    @Column(name = "ID_EVALUADOR", nullable = false)
    private Long evaluadorId;

    @Column(name = "ID_ROL_EFECTIVO")
    private Long rolEfectivoId;

    @Column(name = "ID_UNIDAD_EFECTIVA")
    private Long unidadEfectivaId;

    @Column(name = "FECHA_EVALUACION", nullable = false)
    private LocalDateTime fechaEvaluacion;

    @Column(name = "OBSERVACIONES", length = 2000)
    private String observaciones;

    @Column(name = "ID_DOCUMENTO_OPINION")
    private Long documentoOpinionId;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Long version;
}
