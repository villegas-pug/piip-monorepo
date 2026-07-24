package pe.gob.midagri.piip.portafolio.seguimiento.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mapea la tabla {@code PLANIFICACION_PROYECTO} introducida por el
 * DDL 015 del portafolio (VIGENTE). Cada correccion de la
 * planificacion crea una nueva fila con {@code VERSION} incrementado
 * y {@code ID_VERSION_ANTERIOR} apuntando a la fila anterior; la fila
 * original se conserva (append-only).
 *
 * <p>Las CHECKs canonicas {@code CK_PP_VERSION_MIN} y
 * {@code CK_PP_CERRADA} se validan a nivel de Oracle; la unicidad
 * por proyecto y version la aporta {@code UK_PP_PROY_VERSION}.
 */
@Entity
@Table(name = "PLANIFICACION_PROYECTO")
@Getter
@Setter
@NoArgsConstructor
public class PlanificacionProyectoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqPlanificacionProyecto")
    @SequenceGenerator(name = "seqPlanificacionProyecto",
            sequenceName = "SEQ_PLANIFICACION_PROYECTO", allocationSize = 1)
    @Column(name = "ID_PLANIFICACION", nullable = false)
    private Long id;

    @Column(name = "ID_PROYECTO", nullable = false)
    private Long idProyecto;

    @Column(name = "ALCANCE", length = 2000)
    private String alcance;

    @Column(name = "OBJETIVOS", length = 2000)
    private String objetivos;

    @Lob
    @Column(name = "ENTREGABLES")
    private String entregables;

    @Lob
    @Column(name = "PERIODOS")
    private String periodos;

    @Column(name = "VERSION", nullable = false)
    private int version;

    @Column(name = "ID_VERSION_ANTERIOR")
    private Long idVersionAnterior;

    @Column(name = "CERRADA", nullable = false, length = 1)
    private String cerrada = "N";

    @Column(name = "CREADO_POR", nullable = false, length = 100)
    private String creadoPor;

    @Column(name = "FECHA_CREACION", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;
}
