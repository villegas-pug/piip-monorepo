package pe.gob.midagri.piip.auditoria.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "AUDITORIA_ACCESO")
@Getter
@Setter
@NoArgsConstructor
public class AuditoriaAccesoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqAuditoriaAcceso")
    @SequenceGenerator(name = "seqAuditoriaAcceso", sequenceName = "SEQ_AUDITORIA_ACCESO", allocationSize = 1)
    @Column(name = "ID_AUDIT", nullable = false, updatable = false)
    private Long id;

    @Column(name = "ID_USUARIO", updatable = false)
    private Long usuarioId;

    @Column(name = "ENDPOINT", nullable = false, length = 300, updatable = false)
    private String endpoint;

    @Column(name = "METODO_HTTP", nullable = false, length = 10, updatable = false)
    private String metodoHttp;

    @Column(name = "CODIGO_RESPUESTA", nullable = false, updatable = false)
    private Integer codigoRespuesta;

    @Column(name = "IP_CLIENTE", nullable = false, length = 45, updatable = false)
    private String ipCliente;

    @Column(name = "FECHA_HORA", nullable = false, insertable = false, updatable = false)
    private LocalDateTime fechaHora;

    @Column(name = "DURACION_MS", updatable = false)
    private Integer duracionMs;

    @Column(name = "ID_ROL_EFECTIVO", updatable = false)
    private Integer rolEfectivoId;

    @Column(name = "ID_UNIDAD_EFECTIVA", updatable = false)
    private Long unidadEfectivaId;

    @Column(name = "ID_ASIGNACION_EFECTIVA", updatable = false)
    private Long asignacionEfectivaId;
}
