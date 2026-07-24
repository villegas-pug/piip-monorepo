package pe.gob.midagri.piip.reportes.entity;

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
 * Mapea la tabla REPORTE_ARCHIVO del DDL 017. Conserva
 * cada versión de un formato (PDF o XLSX) generado
 * desde el mismo snapshot. La UK
 * {@code UK_RA_REPORTE_FORMATO_VERSION} impide dos
 * versiones idénticas; el hash se valida contra
 * {@code REGEXP_LIKE(..., '^[0-9A-Fa-f]{64}$')}. El
 * contenido binario se almacena en la versión
 * documental referenciada por
 * {@code idDocumentoVersion}; el contrato no expone el
 * BLOB, solo metadatos.
 */
@Entity
@Table(name = "REPORTE_ARCHIVO")
@Getter
@Setter
@NoArgsConstructor
public class ReporteArchivoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqReporteArchivo")
    @SequenceGenerator(name = "seqReporteArchivo",
            sequenceName = "SEQ_REPORTE_ARCHIVO", allocationSize = 1)
    @Column(name = "ID_ARCHIVO", nullable = false, updatable = false)
    private Long id;

    @Column(name = "ID_REPORTE", nullable = false, updatable = false)
    private Long idReporte;

    @Enumerated(EnumType.STRING)
    @Column(name = "FORMATO", nullable = false, length = 10, updatable = false)
    private FormatoArchivoReporte formato;

    @Column(name = "VERSION", nullable = false, updatable = false)
    private Integer version;

    @Column(name = "HASH_SHA256", nullable = false, length = 64, updatable = false)
    private String hashSha256;

    @Column(name = "ID_DOCUMENTO_VERSION", nullable = false, updatable = false)
    private Long idDocumentoVersion;

    @Column(name = "CREADO_POR", nullable = false, length = 100, updatable = false)
    private String creadoPor;

    @Column(name = "FECHA_CREACION", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    /** Formatos canónicos definidos en el DDL 017 (CHECK {@code CK_RA_FORMATO}). */
    public enum FormatoArchivoReporte {
        PDF,
        XLSX
    }
}
