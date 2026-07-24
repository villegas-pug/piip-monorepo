package pe.gob.midagri.piip.portafolio.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Transición de estado append-only. Registra cada cambio de estado con actor, rol, unidad,
 * documento de referencia y observación.
 */
@Entity
@Table(name = "TRANSICION_ESTADO")
@Getter @Setter @NoArgsConstructor
public class TransicionEstadoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqTransicionEstado")
    @SequenceGenerator(name = "seqTransicionEstado", sequenceName = "SEQ_TRANSICION_ESTADO", allocationSize = 1)
    @Column(name = "ID_TRANSICION", nullable = false)
    private Long id;

    @Column(name = "ID_PROYECTO", nullable = false, updatable = false)
    private Long registroPortafolioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO_ANTERIOR", nullable = false, length = 30)
    private EstadoIniciativa estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO_NUEVO", nullable = false, length = 30)
    private EstadoIniciativa estadoNuevo;

    @Column(name = "ID_USUARIO", nullable = false)
    private Long usuarioId;

    @Column(name = "ID_ROL_EFECTIVO", nullable = false)
    private Integer rolEfectivoId;

    @Column(name = "ID_UNIDAD_EFECTIVA", nullable = false)
    private Long unidadEfectivaId;

    @Column(name = "FECHA_TRANSICION", insertable = false, updatable = false)
    private LocalDateTime fechaTransicion;

    @Column(name = "OBSERVACIONES", length = 2000)
    private String observaciones;

    @Column(name = "ID_DOCUMENTO_REF")
    private Long documentoRefId;
}
