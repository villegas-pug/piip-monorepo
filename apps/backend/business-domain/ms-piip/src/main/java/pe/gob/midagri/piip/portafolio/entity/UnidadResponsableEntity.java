package pe.gob.midagri.piip.portafolio.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Unidad responsable de un registro de portafolio.
 * UK registro/unidad; indicador principal.
 * Un índice único condicional evita dos principales.
 */
@Entity
@Table(name = "PROYECTO_UNIDAD_ORGANICA")
@Getter @Setter @NoArgsConstructor
public class UnidadResponsableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqProyectoUO")
    @SequenceGenerator(name = "seqProyectoUO", sequenceName = "SEQ_PROYECTO_UO", allocationSize = 1)
    @Column(name = "ID_PROY_UO", nullable = false)
    private Long id;

    @Column(name = "ID_PROYECTO", nullable = false, updatable = false)
    private Long registroPortafolioId;

    @Column(name = "NRO_ORDEN", nullable = false)
    private Integer nroOrden;

    @Column(name = "DESCRIPCION", nullable = false, length = 300)
    private String descripcion;

    @Column(name = "ABREVIATURA", nullable = false, length = 20)
    private String abreviatura;
}
