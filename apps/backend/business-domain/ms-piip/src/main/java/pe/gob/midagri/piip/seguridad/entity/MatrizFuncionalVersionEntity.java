package pe.gob.midagri.piip.seguridad.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "MATRIZ_FUNCIONAL_VERSION")
@Getter @Setter @NoArgsConstructor
public class MatrizFuncionalVersionEntity {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqMatrizVersion")
    @SequenceGenerator(name = "seqMatrizVersion", sequenceName = "SEQ_MATRIZ_VERSION", allocationSize = 1)
    @Column(name = "ID_VERSION", nullable = false) private Long id;
    @Column(name = "CODIGO_VERSION", nullable = false, length = 30, updatable = false) private String codigoVersion;
    @Column(name = "ID_VERSION_ANTERIOR", updatable = false) private Long versionAnteriorId;
    // 021.1 permite NULL exclusivamente en la versión fundacional bootstrap.
    @Column(name = "ID_DOCUMENTO_APROBACION", updatable = false) private Long documentoAprobacionId;
    @Column(name = "VIGENTE_DESDE", nullable = false, updatable = false) private LocalDate vigenteDesde;
    @Column(name = "VIGENTE_HASTA", updatable = false) private LocalDate vigenteHasta;
    @Column(name = "ACTIVA", nullable = false, length = 1, updatable = false) private String activa;
    @Column(name = "CREADO_POR", nullable = false, length = 100, updatable = false) private String creadoPor;
    @Column(name = "FECHA_CREACION", nullable = false, insertable = false, updatable = false) private LocalDateTime fechaCreacion;
}
