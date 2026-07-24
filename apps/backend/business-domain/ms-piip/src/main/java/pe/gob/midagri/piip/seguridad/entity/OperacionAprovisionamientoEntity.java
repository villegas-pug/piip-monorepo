package pe.gob.midagri.piip.seguridad.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "OPERACION_APROVISIONAMIENTO")
@Getter @Setter @NoArgsConstructor
public class OperacionAprovisionamientoEntity {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqOperacionAprovisionamiento")
    @SequenceGenerator(name = "seqOperacionAprovisionamiento", sequenceName = "SEQ_OPERACION_APROVISIONAMIENTO", allocationSize = 1)
    @Column(name = "ID_OPERACION", nullable = false) private Long id;
    @Column(name = "CLAVE_IDEMPOTENTE", nullable = false, length = 100, updatable = false) private String claveIdempotente;
    @Column(name = "HASH_PAYLOAD", nullable = false, length = 64, updatable = false) private String hashPayload;
    @Column(name = "ID_USUARIO_OBJETIVO") private Long usuarioObjetivoId;
    @Column(name = "ID_UNIDAD_OBJETIVO") private Long unidadObjetivoId;
    @Column(name = "KEYCLOAK_ID", length = 36) private String keycloakId;
    @Enumerated(EnumType.STRING) @Column(name = "ESTADO_TECNICO", nullable = false, length = 30)
    private EstadoOperacionAprovisionamiento estadoTecnico;
    @Column(name = "INTENTO", nullable = false) private Integer intento;
    @Column(name = "ERROR_RECUPERABLE", nullable = false, length = 1) private String errorRecuperable;
    @Column(name = "RESULTADO_ORACLE", length = 2000) private String resultadoOracle;
    @Column(name = "CREADO_POR", nullable = false, length = 100, updatable = false) private String creadoPor;
    @Column(name = "FECHA_CREACION", nullable = false, insertable = false, updatable = false) private LocalDateTime fechaCreacion;
    @Column(name = "FECHA_CIERRE") private LocalDateTime fechaCierre;
}
