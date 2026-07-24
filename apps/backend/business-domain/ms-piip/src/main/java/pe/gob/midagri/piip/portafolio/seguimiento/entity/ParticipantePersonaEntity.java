package pe.gob.midagri.piip.portafolio.seguimiento.entity;

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
 * Mapea la tabla PARTICIPANTE_PERSONA introducida por el DDL 012
 * del portafolio (VIGENTE). Identifica a las personas que pueden
 * participar en un proyecto (con o sin cuenta PIIP). La
 * clasificacion por defecto es RESTRINGIDO y el CHECK
 * CK_PP_DATOS_MINIMOS exige que, si no hay cuenta vinculada, se
 * registren nombres completos.
 */
@Entity
@Table(name = "PARTICIPANTE_PERSONA")
@Getter
@Setter
@NoArgsConstructor
public class ParticipantePersonaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqParticipantePersona")
    @SequenceGenerator(name = "seqParticipantePersona",
            sequenceName = "SEQ_PARTICIPANTE_PERSONA", allocationSize = 1)
    @Column(name = "ID_PARTICIPANTE", nullable = false)
    private Long id;

    @Column(name = "ID_USUARIO")
    private Long usuarioId;

    @Column(name = "NOMBRES_COMPLETOS", nullable = false, length = 300)
    private String nombresCompletos;

    @Column(name = "INSTITUCION", length = 200)
    private String institucion;

    @Column(name = "FUNCION", length = 200)
    private String funcion;

    @Column(name = "CLASIFICACION", nullable = false, length = 20)
    private String clasificacion = "RESTRINGIDO";
}
