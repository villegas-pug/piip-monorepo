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
 * Mapea la tabla REPORTE_INSTITUCIONAL del DDL 017.
 * Conserva los cortes semestrales canónicos (30/06 y
 * 31/12) validados por la CHECK {@code CK_RE_CORTE},
 * la clasificación y el estado técnico. Su
 * {@code idSnapshot} se fija en una segunda fase para
 * respetar el orden del DDL (la FK se crea tras
 * REPORTE_SNAPSHOT). La fila es append-only: cualquier
 * corrección crea una nueva fila con
 * {@code idVersionAnterior} enlazado, no se muta el
 * reporte aprobado.
 */
@Entity
@Table(name = "REPORTE_INSTITUCIONAL")
@Getter
@Setter
@NoArgsConstructor
public class ReporteInstitucionalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqReporteInstitucional")
    @SequenceGenerator(name = "seqReporteInstitucional",
            sequenceName = "SEQ_REPORTE_INSTITUCIONAL", allocationSize = 1)
    @Column(name = "ID_REPORTE", nullable = false, updatable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO", nullable = false, length = 30, updatable = false)
    private TipoReporte tipo;

    @Column(name = "ANIO", nullable = false, updatable = false)
    private Integer anio;

    /** Semestre 1 o 2 para SEMESTRAL; nulo para EXTRAORDINARIO (CHECK {@code CK_RE_SEMESTRE}). */
    @Column(name = "SEMESTRE", updatable = false)
    private Integer semestre;

    @Column(name = "PERIODO", nullable = false, length = 30, updatable = false)
    private String periodo;

    @Column(name = "FECHA_CORTE", nullable = false, updatable = false)
    private LocalDate fechaCorte;

    /** Filtros, ámbito y serialización canónica del solicitante. */
    @Lob
    @Column(name = "PARAMETROS", updatable = false)
    private String parametros;

    @Column(name = "ID_SNAPSHOT", updatable = false)
    private Long idSnapshot;

    @Column(name = "VERSION_DATOS", nullable = false, updatable = false)
    private Integer versionDatos;

    @Enumerated(EnumType.STRING)
    @Column(name = "CLASIFICACION", nullable = false, length = 20, updatable = false)
    private ClasificacionReporte clasificacion;

    @Column(name = "ID_GENERADOR", nullable = false, updatable = false)
    private Long idGenerador;

    @Column(name = "FECHA_GENERACION", insertable = false, updatable = false)
    private LocalDateTime fechaGeneracion;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO_TECNICO", nullable = false, length = 20)
    private EstadoTecnicoReporte estadoTecnico;
}
