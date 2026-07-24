package pe.gob.midagri.piip.reportes.entity;

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
 * Mapea la tabla REPORTE_APROBACION del DDL 017. Cada
 * versión aprobada del reporte por la Oficina de
 * Modernización inserta una fila nueva; la UK
 * {@code UK_RAP_REPORTE_VERSION} garantiza una sola
 * aprobación por versión. El documento formal de
 * aprobación se referencia como versión documental
 * (idDocumentoAprobacion). La remisión solo se permite
 * contra la versión aprobada.
 */
@Entity
@Table(name = "REPORTE_APROBACION")
@Getter
@Setter
@NoArgsConstructor
public class ReporteAprobacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqReporteAprobacion")
    @SequenceGenerator(name = "seqReporteAprobacion",
            sequenceName = "SEQ_REPORTE_APROBACION", allocationSize = 1)
    @Column(name = "ID_APROBACION", nullable = false, updatable = false)
    private Long id;

    @Column(name = "ID_REPORTE", nullable = false, updatable = false)
    private Long idReporte;

    @Column(name = "ID_VERSION", nullable = false, updatable = false)
    private Integer idVersion;

    @Column(name = "ID_OFICINA", nullable = false, updatable = false)
    private Long idOficina;

    @Column(name = "ID_APROBADOR", nullable = false, updatable = false)
    private Long idAprobador;

    @Column(name = "ID_DOCUMENTO_APROBACION", nullable = false, updatable = false)
    private Long idDocumentoAprobacion;

    @Column(name = "FECHA_APROBACION", insertable = false, updatable = false)
    private LocalDateTime fechaAprobacion;
}
