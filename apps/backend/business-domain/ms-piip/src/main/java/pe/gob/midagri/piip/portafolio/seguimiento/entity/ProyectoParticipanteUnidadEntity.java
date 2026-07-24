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
 * Mapea la tabla PROYECTO_PARTICIPANTE_UNIDAD introducida por el
 * DDL 012 del portafolio (VIGENTE). Asocia una unidad
 * organizacional (UNIDAD_EJECUTORA) con un proyecto manteniendo
 * la vigencia mediante los campos INICIO y FIN: mientras FIN es
 * null la participacion esta vigente; cerrarla implica fijar FIN
 * a la fecha del servidor, nunca eliminar la fila (borrado logico
 * obligatorio para conservar el historial).
 *
 * <p>El UK UK_PPU_PROY_UNI impide duplicar la misma unidad en
 * el mismo proyecto; el CHECK CK_PPU_VIGENCIA garantiza que FIN
 * no sea anterior a INICIO. El rol de una unidad participante es
 * siempre Participante segun el contrato del modulo portafolio
 * (las unidades no pueden asumir el rol de Responsable titular).
 */
@Entity
@Table(name = "PROYECTO_PARTICIPANTE_UNIDAD")
@Getter
@Setter
@NoArgsConstructor
public class ProyectoParticipanteUnidadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqProyPartUnidad")
    @SequenceGenerator(name = "seqProyPartUnidad",
            sequenceName = "SEQ_PROY_PART_UNIDAD", allocationSize = 1)
    @Column(name = "ID_PROY_PART_UNIDAD", nullable = false)
    private Long id;

    @Column(name = "ID_PROYECTO", nullable = false)
    private Long idProyecto;

    @Column(name = "ID_UNIDAD", nullable = false)
    private Long idUnidad;

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
