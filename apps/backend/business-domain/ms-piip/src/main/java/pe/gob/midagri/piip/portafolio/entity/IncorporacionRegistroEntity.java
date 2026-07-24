package pe.gob.midagri.piip.portafolio.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Incorporacion individual de informacion existente.
 * Append-only; conserva fuente, fecha, Responsable, hash, datos originales,
 * observacion del Evaluador, vinculo a registro y version optimista.
 */
@Entity
@Table(name = "INCORPORACION_REGISTRO")
@Getter @Setter @NoArgsConstructor
public class IncorporacionRegistroEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqIncorporacionRegistro")
    @SequenceGenerator(name = "seqIncorporacionRegistro", sequenceName = "SEQ_INCORPORACION_REGISTRO", allocationSize = 1)
    @Column(name = "ID_INCORPORACION", nullable = false)
    private Long id;

    @Column(name = "FUENTE", nullable = false, length = 200)
    private String fuente;

    @Column(name = "FECHA_FUENTE", nullable = false)
    private LocalDate fechaFuente;

    @Column(name = "ID_RESPONSABLE", nullable = false)
    private Long responsableId;

    @Column(name = "ID_DOCUMENTO_FUENTE", nullable = false)
    private Long documentoFuenteId;

    @Column(name = "HASH_ORIGINAL", nullable = false, length = 64)
    private String hashOriginal;

    @Lob
    @Column(name = "DATOS_ORIGINALES")
    private String datosOriginales;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 20)
    private EstadoIncorporacion estado;

    @Column(name = "ID_REGISTRO_VINCULADO")
    private Long registroVinculadoId;

    @Column(name = "OBSERVACION", length = 2000)
    private String observacion;

    @Column(name = "CREADO_POR", nullable = false, length = 100, updatable = false)
    private String creadoPor;

    @Column(name = "FECHA_CREACION", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    /**
     * Version optimista para {@code If-Match} en correcciones, validaciones y resoluciones.
     * La incrementa JPA en cada commit, por lo que la entidad debe estar administrada.
     */
    @Version
    @Column(name = "VERSION", nullable = false)
    private Long version;
}
