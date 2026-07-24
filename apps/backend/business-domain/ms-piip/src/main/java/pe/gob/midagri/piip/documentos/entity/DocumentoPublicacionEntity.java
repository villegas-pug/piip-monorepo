package pe.gob.midagri.piip.documentos.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mapea la tabla DOCUMENTO_PUBLICACION introducida por el DDL
 * 004 (VIGENTE). Registra la confirmación explícita de un
 * Evaluador sobre la publicación de un documento, con título
 * público validado, fecha del servidor y referencia a la
 * asignación efectiva. La UK impide duplicar publicaciones para
 * el mismo documento.
 */
@Entity
@Table(name = "DOCUMENTO_PUBLICACION")
@Getter
@Setter
@NoArgsConstructor
public class DocumentoPublicacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqDocumentoPublicacion")
    @SequenceGenerator(name = "seqDocumentoPublicacion",
            sequenceName = "SEQ_DOCUMENTO_PUBLICACION", allocationSize = 1)
    @Column(name = "ID_PUBLICACION", nullable = false)
    private Long id;

    @Column(name = "ID_DOCUMENTO", nullable = false, updatable = false)
    private Long documentoId;

    @Column(name = "TITULO_PUBLICO", nullable = false, length = 500)
    private String tituloPublico;

    @Column(name = "ID_EVALUADOR_CONFIRMADOR", nullable = false)
    private Long evaluadorConfirmadorId;

    @Column(name = "ID_ASIGNACION_EFECTIVA", nullable = false)
    private Long asignacionEfectivaId;

    @Column(name = "FECHA_PUBLICACION", insertable = false, updatable = false, nullable = false)
    private LocalDateTime fechaPublicacion;
}
