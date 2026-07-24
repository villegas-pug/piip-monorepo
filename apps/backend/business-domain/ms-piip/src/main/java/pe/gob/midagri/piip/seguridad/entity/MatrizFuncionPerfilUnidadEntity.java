package pe.gob.midagri.piip.seguridad.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "MATRIZ_FUNCION_PERFIL_UNIDAD")
@Getter @Setter @NoArgsConstructor
public class MatrizFuncionPerfilUnidadEntity {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqMatrizCombinacion")
    @SequenceGenerator(name = "seqMatrizCombinacion", sequenceName = "SEQ_MATRIZ_COMBINACION", allocationSize = 1)
    @Column(name = "ID_COMBINACION", nullable = false) private Long id;
    @Column(name = "ID_VERSION", nullable = false, updatable = false) private Long versionId;
    @Column(name = "ID_FUNCION", nullable = false, updatable = false) private Long funcionId;
    @Column(name = "ID_ROL", nullable = false, updatable = false) private Integer rolId;
    @Column(name = "ID_UNIDAD", nullable = false, updatable = false) private Long unidadId;
    // 021.1 permite NULL exclusivamente para la combinación bootstrap.
    @Column(name = "ID_APROBADOR", updatable = false) private Long aprobadorId;
    @Column(name = "ID_REGISTRADOR", updatable = false) private Long registradorId;
    @Column(name = "ID_DOCUMENTO_APROBACION", updatable = false) private Long documentoAprobacionId;
    @Column(name = "ES_BOOTSTRAP", nullable = false, length = 1, updatable = false) private String esBootstrap;
    @Column(name = "VIGENTE_DESDE", nullable = false, updatable = false) private LocalDate vigenteDesde;
    @Column(name = "VIGENTE_HASTA", updatable = false) private LocalDate vigenteHasta;
    @Column(name = "ACTIVA", nullable = false, length = 1, updatable = false) private String activa;
    @Column(name = "CREADO_POR", nullable = false, length = 100, updatable = false) private String creadoPor;
    @Column(name = "FECHA_CREACION", nullable = false, insertable = false, updatable = false) private LocalDateTime fechaCreacion;
}
