package pe.gob.midagri.piip.auditoria.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "SOLICITUD_IDEMPOTENTE")
@Getter
@Setter
@NoArgsConstructor
public class SolicitudIdempotenteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqSolicitudIdempotente")
    @SequenceGenerator(name = "seqSolicitudIdempotente", sequenceName = "SEQ_SOLICITUD_IDEMPOTENTE", allocationSize = 1)
    @Column(name = "ID_SOLICITUD", nullable = false, updatable = false)
    private Long id;

    @Column(name = "CONSUMIDOR", nullable = false, length = 100, updatable = false)
    private String consumidor;
    @Column(name = "OPERACION", nullable = false, length = 100, updatable = false)
    private String operacion;
    @Column(name = "CLAVE", nullable = false, length = 100, updatable = false)
    private String clave;
    @Column(name = "HASH_PAYLOAD", nullable = false, length = 64, updatable = false)
    private String hashPayload;
    @Column(name = "RECURSO_TIPO", length = 50)
    private String recursoTipo;
    @Column(name = "RECURSO_ID")
    private Long recursoId;
    @Lob
    @Column(name = "RESPUESTA_JSON")
    private String respuestaJson;
    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO_TECNICO", nullable = false, length = 20)
    private EstadoSolicitudIdempotente estadoTecnico;
    @Column(name = "FECHA_EXPEDICION", nullable = false, insertable = false, updatable = false)
    private LocalDateTime fechaExpedicion;
    @Column(name = "FECHA_EXPIRACION", nullable = false)
    private LocalDateTime fechaExpiracion;
    @Column(name = "CREADO_POR", nullable = false, length = 100, updatable = false)
    private String creadoPor;
}
