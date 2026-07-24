package pe.gob.midagri.piip.portafolio.evaluacion;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mapea la tabla SUBSANACION_INICIATIVA del incremento 014/014.1.
 *
 * <p>La subsanacion es unica por iniciativa: la UK
 * {@code UK_SI_INICIATIVA} garantiza a nivel de fila que solo exista una
 * fila abierta. El plazo se valida con la invariante determinista
 * {@code CK_SI_PLAZO} (PLAZO > APERTURA_EN) a nivel de CHECK. La
 * persistencia es append-only: correcciones del Responsable crean nuevas
 * filas en INCORPORACION_CAMBIO/REGISTRO y esta fila nunca se elimina.
 */
@Entity
@Table(name = "SUBSANACION_INICIATIVA")
@Getter @Setter @NoArgsConstructor
public class SubsanacionIniciativaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqSubsanacionIniciativa")
    @SequenceGenerator(name = "seqSubsanacionIniciativa", sequenceName = "SEQ_SUBSANACION_INICIATIVA", allocationSize = 1)
    @Column(name = "ID_SUBSANACION", nullable = false)
    private Long id;

    @Column(name = "ID_INICIATIVA", nullable = false)
    private Long iniciativaId;

    @Column(name = "PLAZO", nullable = false)
    private LocalDate plazo;

    @Lob
    @Column(name = "INCUMPLIMIENTOS", nullable = false)
    private String incumplimientos;

    @Column(name = "APERTURA_EN", nullable = false)
    private LocalDateTime aperturaEn;

    @Column(name = "ATENCION_EN")
    private LocalDateTime atencionEn;

    @Column(name = "ID_ACTOR", nullable = false)
    private Long actorId;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Long version;
}
