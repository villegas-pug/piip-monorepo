package pe.gob.midagri.piip.seguridad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "MATRIZ_FUNCION")
@Getter @Setter @NoArgsConstructor
public class MatrizFuncionEntity {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqMatrizFuncion")
    @SequenceGenerator(name = "seqMatrizFuncion", sequenceName = "SEQ_MATRIZ_FUNCION", allocationSize = 1)
    @Column(name = "ID_FUNCION", nullable = false) private Long id;
    @Column(name = "ID_VERSION", nullable = false, updatable = false) private Long versionId;
    @Column(name = "CODIGO", nullable = false, length = 30, updatable = false) private String codigo;
    @Column(name = "DESCRIPCION", nullable = false, length = 500, updatable = false) private String descripcion;
    @Column(name = "ACTIVA", nullable = false, length = 1, updatable = false) private String activa;
}
