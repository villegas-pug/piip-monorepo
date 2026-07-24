package pe.gob.midagri.piip.organizacion.entity;

import java.time.LocalDate;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "CAT_OBJETIVO_PEI") @Getter @Setter
public class ObjetivoPeiEntity {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqObjetivoPei")
    @SequenceGenerator(name = "seqObjetivoPei", sequenceName = "SEQ_OBJETIVO_PEI", allocationSize = 1)
    @Column(name = "ID_OBJETIVO") private Long id;
    @Column(name = "ID_VERSION", nullable = false) private Long versionId;
    @Column(name = "CODIGO", nullable = false, length = 30) private String codigo;
    @Column(name = "DESCRIPCION", nullable = false, length = 500) private String descripcion;
    @Column(name = "VIGENTE_DESDE", nullable = false) private LocalDate vigenteDesde;
    @Column(name = "VIGENTE_HASTA") private LocalDate vigenteHasta;
    @Column(name = "ACTIVO", nullable = false, length = 1) private String activo;
}
