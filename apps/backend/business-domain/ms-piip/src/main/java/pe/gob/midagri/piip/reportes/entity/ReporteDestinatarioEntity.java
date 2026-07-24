package pe.gob.midagri.piip.reportes.entity;

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
 * Mapea la tabla REPORTE_DESTINATARIO del DDL 017.
 * Conserva los destinatarios aprobados por la Oficina
 * de Modernización. La UK
 * {@code UK_RD_APROBACION_TIPO_ENTIDAD} impide
 * duplicar la misma entidad dentro de una aprobación;
 * el CHECK {@code CK_RD_TIPO_DESTINATARIO} restringe
 * los tipos a los previstos por BR-125.
 */
@Entity
@Table(name = "REPORTE_DESTINATARIO")
@Getter
@Setter
@NoArgsConstructor
public class ReporteDestinatarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqReporteDestinatario")
    @SequenceGenerator(name = "seqReporteDestinatario",
            sequenceName = "SEQ_REPORTE_DESTINATARIO", allocationSize = 1)
    @Column(name = "ID_DESTINATARIO", nullable = false, updatable = false)
    private Long id;

    @Column(name = "ID_APROBACION", nullable = false, updatable = false)
    private Long idAprobacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_DESTINATARIO", nullable = false, length = 30, updatable = false)
    private TipoDestinatarioReporte tipoDestinatario;

    @Column(name = "ID_ENTIDAD", nullable = false, updatable = false)
    private Long idEntidad;

    @Column(name = "NOMBRE", nullable = false, length = 200, updatable = false)
    private String nombre;
}
