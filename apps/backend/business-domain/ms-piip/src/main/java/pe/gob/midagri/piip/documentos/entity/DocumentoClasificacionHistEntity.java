package pe.gob.midagri.piip.documentos.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mapea la tabla append-only DOCUMENTO_CLASIFICACION_HIST introducida
 * por el DDL 004 (VIGENTE). Conserva la clasificación anterior y
 * nueva, la Autoridad decisora, el Evaluador registrador, el
 * documento formal de decisión, el motivo, la fecha del servidor y
 * el resultado aplicado.
 *
 * <p>Toda reclasificación PIIP, sea de documentos o de campos, se
 * registra exclusivamente mediante esta entidad; el CHECK
 * {@code CK_DCH_RESTRICTIVA} del DDL garantiza que el cambio
 * aplicable nunca sea menos restrictivo que la clasificación
 * anterior. La aplicación Java revalida adicionalmente la regla
 * constitucional antes de invocar esta inserción.
 */
@Entity
@Table(name = "DOCUMENTO_CLASIFICACION_HIST")
@Getter
@Setter
@NoArgsConstructor
public class DocumentoClasificacionHistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqDocumentoClasifHist")
    @SequenceGenerator(name = "seqDocumentoClasifHist",
            sequenceName = "SEQ_DOCUMENTO_CLASIF_HIST", allocationSize = 1)
    @Column(name = "ID_HISTORIAL", nullable = false)
    private Long id;

    @Column(name = "ID_DOCUMENTO", nullable = false, updatable = false)
    private Long documentoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "CLASIFICACION_ANTERIOR", length = 20)
    private ClasificacionDocumento clasificacionAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "CLASIFICACION_NUEVA", nullable = false, length = 20)
    private ClasificacionDocumento clasificacionNueva;

    @Column(name = "ID_AUTORIDAD_DECISORA", nullable = false, updatable = false)
    private Long autoridadDecisoraId;

    @Column(name = "ID_EVALUADOR_REGISTRADOR", nullable = false, updatable = false)
    private Long evaluadorRegistradorId;

    @Column(name = "ID_DOCUMENTO_DECISION", updatable = false)
    private Long documentoDecisionId;

    @Column(name = "MOTIVO", length = 2000)
    private String motivo;

    @Column(name = "FECHA_CAMBIO", insertable = false, updatable = false, nullable = false)
    private LocalDateTime fechaCambio;

    @Enumerated(EnumType.STRING)
    @Column(name = "RESULTADO", nullable = false, length = 20)
    private ResultadoClasificacion resultado;
}
