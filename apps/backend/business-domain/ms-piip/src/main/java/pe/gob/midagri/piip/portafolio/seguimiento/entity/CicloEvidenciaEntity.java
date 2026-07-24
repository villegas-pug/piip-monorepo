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

/**
 * Mapea la tabla {@code CICLO_EVIDENCIA} introducida por el DDL 015
 * del portafolio (VIGENTE). Vincula un documento del portafolio a
 * un ciclo como evidencia opcional. La UK
 * {@code UK_CE_CICLO_DOC} garantiza unicidad por ciclo y
 * documento.
 */
@Entity
@Table(name = "CICLO_EVIDENCIA")
@Getter
@Setter
@NoArgsConstructor
public class CicloEvidenciaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqCicloEvidencia")
    @SequenceGenerator(name = "seqCicloEvidencia",
            sequenceName = "SEQ_CICLO_EVIDENCIA", allocationSize = 1)
    @Column(name = "ID_EVIDENCIA", nullable = false)
    private Long id;

    @Column(name = "ID_CICLO", nullable = false)
    private Long idCiclo;

    @Column(name = "ID_DOCUMENTO", nullable = false)
    private Long idDocumento;

    @Column(name = "CREADO_POR", nullable = false, length = 100)
    private String creadoPor;

    @Column(name = "FECHA_CREACION", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;
}
