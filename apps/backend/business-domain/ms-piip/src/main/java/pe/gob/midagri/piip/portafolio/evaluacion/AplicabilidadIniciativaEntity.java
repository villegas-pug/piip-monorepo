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
 * Mapea la tabla APLICABILIDAD_INICIATIVA del incremento 014/014.1.
 *
 * <p>Es la decision de aplicabilidad del Evaluador. El CHECK
 * {@code CK_AI_RESULTADO} limita el valor de resultado a APLICABLE o
 * NO_APLICABLE; {@code CK_AI_MOTIVO} exige motivo no nulo cuando el
 * resultado es NO_APLICABLE. Es una operacion independiente de la
 * admision (US2): no se confunde con NO_ADMISIBLE.
 */
@Entity
@Table(name = "APLICABILIDAD_INICIATIVA")
@Getter @Setter @NoArgsConstructor
public class AplicabilidadIniciativaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqAplicabilidadIniciativa")
    @SequenceGenerator(name = "seqAplicabilidadIniciativa", sequenceName = "SEQ_APLICABILIDAD_INICIATIVA", allocationSize = 1)
    @Column(name = "ID_APLICABILIDAD", nullable = false)
    private Long id;

    @Column(name = "ID_INICIATIVA", nullable = false)
    private Long iniciativaId;

    @Column(name = "RESULTADO", nullable = false, length = 20)
    private String resultado;

    @Column(name = "MOTIVO", length = 2000)
    private String motivo;

    @Column(name = "ID_EVALUADOR", nullable = false)
    private Long evaluadorId;

    @Column(name = "FECHA", nullable = false)
    private LocalDateTime fecha;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Long version;
}
