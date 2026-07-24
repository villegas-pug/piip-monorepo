package pe.gob.midagri.piip.portafolio.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Conflictos de incorporación: código, duplicado o relación inválida.
 * Bloquea validación hasta resolución documentada por Evaluador.
 */
@Entity
@Table(name = "INCORPORACION_CONFLICTO")
@Getter @Setter @NoArgsConstructor
public class IncorporacionConflictoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqIncorporacionConflicto")
    @SequenceGenerator(name = "seqIncorporacionConflicto", sequenceName = "SEQ_INCORPORACION_CONFLICTO", allocationSize = 1)
    @Column(name = "ID_CONFLICTO", nullable = false)
    private Long id;

    @Column(name = "ID_INCORPORACION", nullable = false, updatable = false)
    private Long incorporacionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_CONFLICTO", nullable = false, length = 30)
    private TipoConflicto tipoConflicto;

    @Column(name = "ID_REGISTRO_CONFLICTIVO", nullable = false)
    private Long registroConflictivoId;

    @Column(name = "DESCRIPCION", length = 2000)
    private String descripcion;

    @Column(name = "RESUELTO", nullable = false, length = 1)
    private String resuelto;

    @Column(name = "ID_RESOLUTOR")
    private Long resolutorId;

    @Column(name = "FECHA_RESOLUCION")
    private LocalDateTime fechaResolucion;

    @Column(name = "ID_DOCUMENTO_RESOLUCION")
    private Long documentoResolucionId;
}
