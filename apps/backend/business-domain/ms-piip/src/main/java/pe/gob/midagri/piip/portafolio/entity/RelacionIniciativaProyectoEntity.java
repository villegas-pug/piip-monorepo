package pe.gob.midagri.piip.portafolio.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Relación inmutable entre una iniciativa aprobada y su único proyecto derivado.
 * UK por iniciativa y por proyecto.
 */
@Entity
@Table(name = "INICIATIVA_PROYECTO")
@Getter @Setter @NoArgsConstructor
public class RelacionIniciativaProyectoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqIniciativaProyecto")
    @SequenceGenerator(name = "seqIniciativaProyecto", sequenceName = "SEQ_INICIATIVA_PROYECTO", allocationSize = 1)
    @Column(name = "ID_RELACION", nullable = false)
    private Long id;

    @Column(name = "ID_INICIATIVA", nullable = false, updatable = false)
    private Long iniciativaId;

    @Column(name = "ID_PROYECTO", nullable = false, updatable = false)
    private Long proyectoId;

    @Column(name = "CREADA_POR", nullable = false, length = 100, updatable = false)
    private String creadaPor;

    @Column(name = "FECHA_CREACION", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;
}
