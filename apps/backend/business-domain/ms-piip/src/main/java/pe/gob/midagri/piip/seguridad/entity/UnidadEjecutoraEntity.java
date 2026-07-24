package pe.gob.midagri.piip.seguridad.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "UNIDAD_EJECUTORA")
@Getter @Setter @NoArgsConstructor
public class UnidadEjecutoraEntity {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqUnidadEjecutora")
    @SequenceGenerator(name = "seqUnidadEjecutora", sequenceName = "SEQ_UNIDAD_EJECUTORA", allocationSize = 1)
    @Column(name = "ID_UNIDAD", nullable = false) private Long id;
    @Column(name = "CODIGO_UNIDAD", nullable = false, length = 20) private String codigo;
    @Column(name = "NOMBRE", nullable = false, length = 200) private String nombre;
    @Column(name = "DESCRIPCION", length = 500) private String descripcion;
    @Column(name = "NIVEL_JERARQUICO", nullable = false) private Integer nivelJerarquico;
    @Column(name = "ID_UNIDAD_PADRE") private Long unidadPadreId;
    @Column(name = "ACTIVO", nullable = false, length = 1) private String activo;
    @Column(name = "FECHA_ACTIVACION", nullable = false, insertable = false) private LocalDate fechaActivacion;
    @Column(name = "CREADO_POR", nullable = false, length = 100, updatable = false) private String creadoPor;
    @Column(name = "FECHA_CREACION", nullable = false, insertable = false, updatable = false) private LocalDateTime fechaCreacion;
    @Column(name = "MODIFICADO_POR", length = 100) private String modificadoPor;
    @Column(name = "FECHA_MODIFICACION") private LocalDateTime fechaModificacion;
}
