package pe.gob.midagri.piip.portafolio.seguimiento.entity;

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

/** Relación inmutable entre una presentación final y su evidencia (DDL 025). */
@Entity
@Table(name = "PRESENTACION_PRODUCTO_FINAL_EVIDENCIA")
@Getter
@Setter
@NoArgsConstructor
public class PresentacionProductoFinalEvidenciaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqPpfEvidencia")
    @SequenceGenerator(name = "seqPpfEvidencia", sequenceName = "SEQ_PPF_EVIDENCIA", allocationSize = 1)
    @Column(name = "ID_EVIDENCIA", nullable = false)
    private Long id;

    @Column(name = "ID_PRESENTACION", nullable = false, updatable = false)
    private Long idPresentacion;

    @Column(name = "ID_DOCUMENTO", nullable = false, updatable = false)
    private Long idDocumento;

    @Column(name = "CREADO_POR", nullable = false, length = 100, updatable = false)
    private String creadoPor;

    @Column(name = "FECHA_CREACION", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;
}
