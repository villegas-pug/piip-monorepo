package pe.gob.midagri.piip.seguridad.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "USUARIO")
@Getter @Setter @NoArgsConstructor
public class UsuarioEntity {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqUsuario")
    @SequenceGenerator(name = "seqUsuario", sequenceName = "SEQ_USUARIO", allocationSize = 1)
    @Column(name = "ID_USUARIO", nullable = false) private Long id;
    @Column(name = "KEYCLOAK_ID", nullable = false, length = 36) private String keycloakId;
    @Column(name = "LOGIN", length = 100) private String login;
    @Column(name = "NOMBRE_COMPLETO", length = 300) private String nombreCompleto;
    @Column(name = "CORREO", length = 200) private String correo;
    @Column(name = "ACTIVO", nullable = false, length = 1) private String activo;
    @Column(name = "CREADO_POR", nullable = false, length = 100, updatable = false) private String creadoPor;
    @Column(name = "FECHA_CREACION", nullable = false, insertable = false, updatable = false) private LocalDateTime fechaCreacion;
    @Column(name = "LOGIN_SINTETICO", nullable = false, length = 1) private String loginSintetico;
}
