package pe.gob.midagri.piip.portafolio.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cambios append-only de una incorporación individual.
 * Registra antes/después y motivo de cada corrección.
 */
@Entity
@Table(name = "INCORPORACION_CAMBIO")
@Getter @Setter @NoArgsConstructor
public class IncorporacionCambioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqIncorporacionCambio")
    @SequenceGenerator(name = "seqIncorporacionCambio", sequenceName = "SEQ_INCORPORACION_CAMBIO", allocationSize = 1)
    @Column(name = "ID_CAMBIO", nullable = false)
    private Long id;

    @Column(name = "ID_INCORPORACION", nullable = false, updatable = false)
    private Long incorporacionId;

    @Lob
    @Column(name = "DATOS_ANTES")
    private String datosAntes;

    @Lob
    @Column(name = "DATOS_DESPUES")
    private String datosDespues;

    @Column(name = "MOTIVO", length = 2000)
    private String motivo;

    @Column(name = "ID_ACTOR", nullable = false)
    private Long actorId;

    @Column(name = "FECHA_CAMBIO", insertable = false, updatable = false)
    private LocalDateTime fechaCambio;
}
