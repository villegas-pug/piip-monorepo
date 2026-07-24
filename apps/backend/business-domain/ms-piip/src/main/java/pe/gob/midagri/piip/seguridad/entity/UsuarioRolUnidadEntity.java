package pe.gob.midagri.piip.seguridad.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "USUARIO_ROL_UNIDAD")
@Getter @Setter @NoArgsConstructor
public class UsuarioRolUnidadEntity {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqUsuarioRolUnidad")
    @SequenceGenerator(name = "seqUsuarioRolUnidad", sequenceName = "SEQ_USUARIO_ROL_UNIDAD", allocationSize = 1)
    @Column(name = "ID_USR_ROL_UNIDAD", nullable = false) private Long id;
    @Column(name = "ID_USUARIO", nullable = false, updatable = false) private Long usuarioId;
    @Column(name = "ID_ROL", nullable = false, updatable = false) private Integer rolId;
    @Column(name = "ID_UNIDAD", nullable = false, updatable = false) private Long unidadId;
    @Column(name = "ACTIVO", nullable = false, length = 1) private String activo;
    @Column(name = "FECHA_ASIGNACION", nullable = false, updatable = false) private LocalDate fechaAsignacion;
    @Column(name = "ASIGNADO_POR", nullable = false, length = 100, updatable = false) private String asignadoPor;
    @Column(name = "FECHA_INICIO", nullable = false) private LocalDate fechaInicio;
    @Column(name = "FECHA_FIN") private LocalDate fechaFin;
    @Column(name = "REVOCADA_EN") private LocalDateTime revocadaEn;
    @Column(name = "REVOCADA_POR", length = 100) private String revocadaPor;
    @Column(name = "MOTIVO_REVOCACION", length = 2000) private String motivoRevocacion;
    @Column(name = "INACTIVA_TEMPORALMENTE", nullable = false, length = 1) private String inactivaTemporalmente;
    @Column(name = "ID_COMBINACION_MATRIZ") private Long combinacionMatrizId;
    @Column(name = "ID_DOCUMENTO_FORMAL") private Long documentoFormalId;
    @Version @Column(name = "VERSION", nullable = false) private Long version;
}
