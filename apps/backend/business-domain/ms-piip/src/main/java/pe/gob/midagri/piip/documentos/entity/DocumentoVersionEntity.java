package pe.gob.midagri.piip.documentos.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Cada fila DOCUMENTO es una versión; no existe una tabla DOCUMENTO_VERSION paralela. */
@Entity @Table(name = "DOCUMENTO") @Getter @Setter
public class DocumentoVersionEntity {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqDocumento")
    @SequenceGenerator(name = "seqDocumento", sequenceName = "SEQ_DOCUMENTO", allocationSize = 1)
    @Column(name = "ID_DOCUMENTO") private Long id;
    @Column(name = "ID_PROYECTO") private Long registroPortafolioId;
    @Column(name = "ID_TIPO_DOC", nullable = false) private Integer tipoDocumentoId;
    @Column(name = "ESTADO_AL_CARGAR") private String estadoAlCargar;
    @Column(name = "NOMBRE_ORIGINAL", nullable = false) private String nombreOriginal;
    @Column(name = "MIME_TYPE", nullable = false) private String mimeType;
    @Column(name = "TAMANO_BYTES", nullable = false) private Long tamanoBytes;
    @Column(name = "HASH_SHA256", nullable = false, length = 64) private String hashSha256;
    @Column(name = "ID_USUARIO_CARGA", nullable = false) private Long usuarioCargaId;
    @Column(name = "FECHA_CARGA", insertable = false, updatable = false) private LocalDateTime fechaCarga;
    @Column(name = "ACTIVO", nullable = false) private String activo;
    @Column(name = "INMUTABLE", nullable = false) private String inmutable;
    @Column(name = "NUMERO_VERSION", nullable = false) private Integer numeroVersion;
    @Column(name = "ID_DOCUMENTO_ANTERIOR") private Long documentoAnteriorId;
    @Enumerated(EnumType.STRING) @Column(name = "CLASIFICACION") private ClasificacionDocumento clasificacion;
    @Lob @Basic(fetch = FetchType.LAZY) @Column(name = "CONTENIDO") private byte[] contenido;
    @Column(name = "FORMATO") private String formato;
    @Column(name = "ID_DOCUMENTO_SERIE") private Long serieId;
    // Huella 004 (VIGENTE): clasificación validada por el Evaluador con fecha del servidor.
    // Permanece nula hasta que el servicio registre el evento de validación.
    @Enumerated(EnumType.STRING) @Column(name = "CLASIFICACION_VALIDADA", length = 20) private ClasificacionDocumento clasificacionValidada;
    @Column(name = "CLASIFICACION_FECHA") private java.time.LocalDateTime clasificacionFecha;
    @Column(name = "ID_USUARIO_VALIDA") private Long usuarioValidaId;
}
