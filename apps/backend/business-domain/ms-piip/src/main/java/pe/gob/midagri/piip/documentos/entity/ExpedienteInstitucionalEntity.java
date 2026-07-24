package pe.gob.midagri.piip.documentos.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "EXPEDIENTE_INSTITUCIONAL") @Getter @Setter
public class ExpedienteInstitucionalEntity {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqExpediente")
    @SequenceGenerator(name = "seqExpediente", sequenceName = "SEQ_EXPEDIENTE_INSTITUCIONAL", allocationSize = 1)
    @Column(name = "ID_EXPEDIENTE") private Long id;
    @Column(name = "CODIGO", nullable = false, updatable = false) private String codigo;
    @Column(name = "ASUNTO", nullable = false) private String asunto;
    @Column(name = "MODULO_ORIGEN", nullable = false, updatable = false) private String moduloOrigen;
    @Column(name = "REFERENCIA_CASO_USO", nullable = false, updatable = false) private String referenciaCasoUso;
    @Column(name = "ID_UNIDAD") private Long unidadId;
    @Enumerated(EnumType.STRING) @Column(name = "CLASIFICACION", nullable = false) private ClasificacionDocumento clasificacion;
    @Column(name = "ACTIVO", nullable = false) private String activo;
    @Version @Column(name = "VERSION", nullable = false) private Long version;
    @Column(name = "CREADO_POR", nullable = false, updatable = false) private String creadoPor;
    @Column(name = "FECHA_CREACION", insertable = false, updatable = false) private LocalDateTime fechaCreacion;
}
