package pe.gob.midagri.piip.seguridad.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "USUARIO_ROL_UNIDAD_EVENTO")
@Getter @Setter @NoArgsConstructor
public class UsuarioRolUnidadEventoEntity {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqUruEvento")
    @SequenceGenerator(name = "seqUruEvento", sequenceName = "SEQ_URU_EVENTO", allocationSize = 1)
    @Column(name = "ID_EVENTO", nullable = false, updatable = false) private Long id;
    @Column(name = "ID_ASIGNACION", nullable = false, updatable = false) private Long asignacionId;
    @Enumerated(EnumType.STRING) @Column(name = "TIPO_EVENTO", nullable = false, length = 30, updatable = false)
    private TipoEventoAsignacion tipoEvento;
    @Column(name = "ID_USUARIO_ACTOR", updatable = false) private Long usuarioActorId;
    @Column(name = "ID_ROL_ACTOR", updatable = false) private Integer rolActorId;
    @Column(name = "ID_UNIDAD_ACTOR", updatable = false) private Long unidadActorId;
    @Column(name = "FECHA_EVENTO", nullable = false, insertable = false, updatable = false) private LocalDateTime fechaEvento;
    @Column(name = "MOTIVO", length = 2000, updatable = false) private String motivo;
    @Column(name = "ID_ASIGNACION_EFECTIVA", updatable = false) private Long asignacionEfectivaId;
}
