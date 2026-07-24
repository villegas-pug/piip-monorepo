package pe.gob.midagri.piip.auditoria.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "AUDITORIA_EVENTO")
@Getter
@Setter
@NoArgsConstructor
public class AuditoriaEventoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqAuditoriaEvento")
    @SequenceGenerator(name = "seqAuditoriaEvento", sequenceName = "SEQ_AUDITORIA_EVENTO", allocationSize = 1)
    @Column(name = "ID_EVENTO", nullable = false, updatable = false)
    private Long id;

    @Column(name = "TIPO_EVENTO", nullable = false, length = 100, updatable = false)
    private String tipoEvento;

    @Column(name = "ENTIDAD_TIPO", nullable = false, length = 50, updatable = false)
    private String entidadTipo;

    @Column(name = "ENTIDAD_ID", nullable = false, updatable = false)
    private Long entidadId;

    @Lob
    @Column(name = "PAYLOAD_JSON", nullable = false, updatable = false)
    private String payloadJson;

    @Column(name = "ID_USUARIO", updatable = false)
    private Long usuarioId;

    @Column(name = "FECHA_EVENTO", nullable = false, insertable = false, updatable = false)
    private LocalDateTime fechaEvento;

    @Column(name = "PROCESADO", nullable = false, length = 1, insertable = false, updatable = false)
    private String procesado;
}
