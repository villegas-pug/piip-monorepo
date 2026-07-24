package pe.gob.midagri.piip.reportes.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mapea la tabla REPORTE_SNAPSHOT del DDL 017. Conserva
 * el CLOB JSON canónico con totales, indicadores y
 * detalle del mismo corte, parámetros y versión. La
 * serialización es determinista: claves ordenadas,
 * números y fechas normalizados; el hash SHA-256 se
 * calcula sobre esa cadena y se valida contra la CHECK
 * {@code UK_RS_HASH}. Un mismo hash no puede
 * reutilizarse en dos snapshots distintos. PDF y XLSX
 * referencian este mismo {@code idSnapshot}, por lo que
 * ambas salidas se generan a partir de la misma fuente
 * de datos sin reconstruir el corte.
 */
@Entity
@Table(name = "REPORTE_SNAPSHOT")
@Getter
@Setter
@NoArgsConstructor
public class ReporteSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqReporteSnapshot")
    @SequenceGenerator(name = "seqReporteSnapshot",
            sequenceName = "SEQ_REPORTE_SNAPSHOT", allocationSize = 1)
    @Column(name = "ID_SNAPSHOT", nullable = false, updatable = false)
    private Long id;

    @Lob
    @Column(name = "PAYLOAD_JSON", nullable = false, updatable = false)
    private String payloadJson;

    @Column(name = "VERSION_ESQUEMA", updatable = false)
    private Integer versionEsquema;

    @Column(name = "HASH_SHA256", nullable = false, length = 64, updatable = false)
    private String hashSha256;

    @Column(name = "FECHA_CORTE", nullable = false, updatable = false)
    private LocalDate fechaCorte;

    @Lob
    @Column(name = "PARAMETROS", updatable = false)
    private String parametros;

    @Enumerated(EnumType.STRING)
    @Column(name = "CLASIFICACION", length = 20, updatable = false)
    private ClasificacionReporte clasificacion;

    @Column(name = "CREADO_POR", nullable = false, length = 100, updatable = false)
    private String creadoPor;

    @Column(name = "FECHA_CREACION", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;
}
