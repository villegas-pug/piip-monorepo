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
 * Mapea la tabla REPORTE_REMISION del DDL 017.
 * Conserva la remisión registrada manualmente por la
 * Oficina de Modernización: destinatario, fecha,
 * resultado y motivo. La UK
 * {@code UK_RREM_REPORTE_DESTINATARIO_FECHA} evita
 * duplicar el mismo evento exacto. No existe remisión
 * automática: el contrato registra la evidencia
 * declarada por el actor, no envía correos ni
 * sincroniza con sistemas externos.
 */
@Entity
@Table(name = "REPORTE_REMISION")
@Getter
@Setter
@NoArgsConstructor
public class ReporteRemisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqReporteRemision")
    @SequenceGenerator(name = "seqReporteRemision",
            sequenceName = "SEQ_REPORTE_REMISION", allocationSize = 1)
    @Column(name = "ID_REMISION", nullable = false, updatable = false)
    private Long id;

    @Column(name = "ID_REPORTE", nullable = false, updatable = false)
    private Long idReporte;

    @Column(name = "ID_DESTINATARIO", nullable = false, updatable = false)
    private Long idDestinatario;

    @Column(name = "FECHA_REMISION", insertable = false, updatable = false)
    private LocalDateTime fechaRemision;

    @Enumerated(EnumType.STRING)
    @Column(name = "RESULTADO", nullable = false, length = 20, updatable = false)
    private ResultadoRemision resultado;

    @Column(name = "MOTIVO", length = 2000, updatable = false)
    private String motivo;
}
