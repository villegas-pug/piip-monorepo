package pe.gob.midagri.piip.portafolio.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Titularidad vigente de un registro de portafolio.
 * Un índice único condicional evita dos titularidades abiertas.
 */
@Entity
@Table(name = "PROYECTO_RESPONSABLE")
@Getter @Setter @NoArgsConstructor
public class TitularidadResponsableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqProyectoResponsable")
    @SequenceGenerator(name = "seqProyectoResponsable", sequenceName = "SEQ_PROYECTO_RESPONSABLE", allocationSize = 1)
    @Column(name = "ID_TITULARIDAD", nullable = false)
    private Long id;

    @Column(name = "ID_PROYECTO", nullable = false, updatable = false)
    private Long registroPortafolioId;

    @Column(name = "ID_USUARIO", nullable = false, updatable = false)
    private Long usuarioId;

    @Column(name = "INICIO", nullable = false)
    private LocalDate inicio;

    @Column(name = "FIN")
    private LocalDate fin;

    @Column(name = "MOTIVO_SUSTITUCION", length = 2000)
    private String motivoSustitucion;

    @Column(name = "ID_ACTOR_SUSTITUCION")
    private Long actorSustitucionId;

    @Column(name = "CREADO_POR", nullable = false, length = 100, updatable = false)
    private String creadoPor;

    @Column(name = "FECHA_CREACION", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;
}
