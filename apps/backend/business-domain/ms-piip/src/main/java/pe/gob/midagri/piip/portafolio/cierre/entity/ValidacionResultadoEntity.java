package pe.gob.midagri.piip.portafolio.cierre.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Validación de los resultados registrados por el Responsable (DDL 015). */
@Entity
@Table(name = "VALIDACION_RESULTADO")
@Getter
@Setter
@NoArgsConstructor
public class ValidacionResultadoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqValidacionResultado")
    @SequenceGenerator(name = "seqValidacionResultado", sequenceName = "SEQ_VALIDACION_RESULTADO", allocationSize = 1)
    @Column(name = "ID_VALIDACION", nullable = false)
    private Long id;

    @Column(name = "ID_PROYECTO", nullable = false)
    private Long idProyecto;

    @Column(name = "ID_RESPONSABLE", nullable = false)
    private Long idResponsable;

    @Column(name = "ID_EVALUADOR", nullable = false)
    private Long idEvaluador;

    @Lob
    @Column(name = "RESULTADOS_CLAVE")
    private String resultadosClave;

    @Column(name = "VALIDADO_EN", insertable = false, updatable = false)
    private LocalDateTime validadoEn;

    @Column(name = "CREADO_POR", nullable = false)
    private String creadoPor;

    @Column(name = "FECHA_CREACION", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;
}
