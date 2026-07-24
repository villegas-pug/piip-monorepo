package pe.gob.midagri.piip.seguridad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ROL")
@Getter @Setter @NoArgsConstructor
public class RolEntity {
    @Id @Column(name = "ID_ROL", nullable = false) private Integer id;
    @Column(name = "NOMBRE_ROL", nullable = false, length = 50) private String nombre;
    @Column(name = "DESCRIPCION", length = 500) private String descripcion;
    @Column(name = "NIVEL_ACCESO", nullable = false) private Integer nivelAcceso;
}
