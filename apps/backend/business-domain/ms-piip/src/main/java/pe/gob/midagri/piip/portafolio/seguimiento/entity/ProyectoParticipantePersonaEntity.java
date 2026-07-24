package pe.gob.midagri.piip.portafolio.seguimiento.entity;

import java.time.LocalDate;
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
 * Mapea la tabla PROYECTO_PARTICIPANTE_PERSONA introducida por el
 * DDL 012 del portafolio (VIGENTE). Asocia una persona
 * (PARTICIPANTE_PERSONA) con un proyecto (PROYECTO) manteniendo
 * la vigencia mediante los campos INICIO y FIN: mientras FIN es
 * null la participacion esta vigente; cerrarla implica fijar FIN
 * a la fecha del servidor, nunca eliminar la fila (borrado logico
 * obligatorio para conservar el historial).
 *
 * <p>El UK UK_PPP_PROY_PART impide duplicar la misma persona en
 * el mismo proyecto; el CHECK CK_PPP_VIGENCIA garantiza que FIN
 * no sea anterior a INICIO.
 */
@Entity
@Table(name = "PROYECTO_PARTICIPANTE_PERSONA")
@Getter
@Setter
@NoArgsConstructor
public class ProyectoParticipantePersonaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqProyPartPersona")
    @SequenceGenerator(name = "seqProyPartPersona",
            sequenceName = "SEQ_PROY_PART_PERSONA", allocationSize = 1)
    @Column(name = "ID_PROY_PART_PERSONA", nullable = false)
    private Long id;

    @Column(name = "ID_PROYECTO", nullable = false)
    private Long idProyecto;

    @Column(name = "ID_PARTICIPANTE", nullable = false)
    private Long idParticipante;

    @Column(name = "INICIO", nullable = false)
    private LocalDate inicio;

    @Column(name = "FIN")
    private LocalDate fin;

    @Column(name = "ID_ACTOR", nullable = false)
    private Long idActor;

    @Column(name = "CREADO_POR", nullable = false, length = 100)
    private String creadoPor;

    @Column(name = "FECHA_CREACION", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;
}
