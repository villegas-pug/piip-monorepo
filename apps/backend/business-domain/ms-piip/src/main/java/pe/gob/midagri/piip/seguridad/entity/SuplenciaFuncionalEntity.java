package pe.gob.midagri.piip.seguridad.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "SUPLENCIA_FUNCIONAL")
@Getter @Setter @NoArgsConstructor
public class SuplenciaFuncionalEntity {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqSuplenciaFuncional")
    @SequenceGenerator(name = "seqSuplenciaFuncional", sequenceName = "SEQ_SUPLENCIA_FUNCIONAL", allocationSize = 1)
    @Column(name = "ID_SUPLENCIA", nullable = false) private Long id;
    @Column(name = "ID_ASIGNACION_TITULAR", nullable = false, updatable = false) private Long asignacionTitularId;
    @Column(name = "ID_ASIGNACION_SUPLENTE", nullable = false, updatable = false) private Long asignacionSuplenteId;
    @Column(name = "INICIO", nullable = false, updatable = false) private LocalDate inicio;
    @Column(name = "FIN", nullable = false, updatable = false) private LocalDate fin;
    @Column(name = "TERMINADA_EN") private LocalDateTime terminadaEn;
    @Column(name = "ID_AUTORIDAD", nullable = false, updatable = false) private Long autoridadId;
    @Column(name = "ID_DOCUMENTO_FORMAL", nullable = false, updatable = false) private Long documentoFormalId;
    @Column(name = "CREADO_POR", nullable = false, length = 100, updatable = false) private String creadoPor;
    @Column(name = "FECHA_CREACION", nullable = false, insertable = false, updatable = false) private LocalDateTime fechaCreacion;
}
