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

/** Registro único e inmutable del cierre administrativo (DDL 015). */
@Entity
@Table(name = "CIERRE_PROYECTO")
@Getter
@Setter
@NoArgsConstructor
public class CierreProyectoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqCierreProyecto")
    @SequenceGenerator(name = "seqCierreProyecto", sequenceName = "SEQ_CIERRE_PROYECTO", allocationSize = 1)
    @Column(name = "ID_CIERRE", nullable = false)
    private Long id;

    @Column(name = "ID_PROYECTO", nullable = false)
    private Long idProyecto;

    @Lob
    @Column(name = "INFORME_FINAL")
    private String informeFinal;

    @Lob
    @Column(name = "RESULTADOS")
    private String resultados;

    @Lob
    @Column(name = "APRENDIZAJES")
    private String aprendizajes;

    @Column(name = "CONCLUSION", nullable = false, length = 2000)
    private String conclusion;

    @Column(name = "OBSERVACION", nullable = false, length = 2000)
    private String observacion;

    @Column(name = "ID_EVALUADOR", nullable = false)
    private Long idEvaluador;

    @Column(name = "FECHA_CIERRE", insertable = false, updatable = false)
    private LocalDateTime fechaCierre;
}
