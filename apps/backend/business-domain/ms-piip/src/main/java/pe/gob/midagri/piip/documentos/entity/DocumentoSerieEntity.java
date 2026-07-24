package pe.gob.midagri.piip.documentos.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "DOCUMENTO_SERIE") @Getter @Setter
public class DocumentoSerieEntity {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqDocumentoSerie")
    @SequenceGenerator(name = "seqDocumentoSerie", sequenceName = "SEQ_DOCUMENTO_SERIE", allocationSize = 1)
    @Column(name = "ID_SERIE") private Long id;
    @Column(name = "ID_TIPO_DOC", nullable = false, updatable = false) private Integer tipoDocumentoId;
    @Column(name = "ID_REGISTRO", updatable = false) private Long registroPortafolioId;
    @Column(name = "ID_EXPEDIENTE", updatable = false) private Long expedienteInstitucionalId;
    @Column(name = "TITULO", nullable = false) private String titulo;
    @Enumerated(EnumType.STRING) @Column(name = "CLASIFICACION_PROPUESTA", nullable = false) private ClasificacionDocumento clasificacionPropuesta;
    @Enumerated(EnumType.STRING) @Column(name = "CLASIFICACION_VALIDADA") private ClasificacionDocumento clasificacionValidada;
    @Column(name = "ACTIVA", nullable = false) private String activa;
    @Version @Column(name = "VERSION", nullable = false) private Long version;
    @Column(name = "CREADO_POR", nullable = false, updatable = false) private String creadoPor;
    @Column(name = "FECHA_CREACION", insertable = false, updatable = false) private LocalDateTime fechaCreacion;
}
