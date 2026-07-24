package pe.gob.midagri.piip.portafolio.evaluacion;

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
 * Mapea la tabla APLICABILIDAD_CRITERIO del incremento 014/014.1.
 * Conserva la lista estructurada de criterios de aplicabilidad anclada
 * a la aplicabilidad padre. La UK {@code UK_AC_APLICABILIDAD_CLAVE}
 * evita duplicar criterios con la misma clave por iniciativa.
 */
@Entity
@Table(name = "APLICABILIDAD_CRITERIO")
@Getter @Setter @NoArgsConstructor
public class AplicabilidadCriterioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqAplicabilidadCriterio")
    @SequenceGenerator(name = "seqAplicabilidadCriterio", sequenceName = "SEQ_APLICABILIDAD_CRITERIO", allocationSize = 1)
    @Column(name = "ID_CRITERIO", nullable = false)
    private Long id;

    @Column(name = "ID_APLICABILIDAD", nullable = false)
    private Long aplicabilidadId;

    @Column(name = "CLAVE", nullable = false, length = 50)
    private String clave;

    @Column(name = "VALOR", nullable = false, length = 500)
    private String valor;

    @Column(name = "ORDEN", nullable = false)
    private Integer orden;
}
