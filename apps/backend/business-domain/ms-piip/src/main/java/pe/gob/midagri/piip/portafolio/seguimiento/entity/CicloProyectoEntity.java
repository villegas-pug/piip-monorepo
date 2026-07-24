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
 * Mapea la tabla {@code CICLO_PROYECTO} introducida por el DDL 015
 * del portafolio (VIGENTE). Representa la fila "actual" del ciclo
 * dentro de un proyecto; las correcciones insertan una nueva fila
 * con {@code NUMERO_VERSION} incrementado y
 * {@code ID_VERSION_ANTERIOR} apuntando a la fila anterior
 * (append-only). El CHECK {@code CK_CP_PERIODO} exige el formato
 * canonico {@code AAAA-Qn-Sn} y {@code CK_CP_AVANCE} restringe el
 * avance al rango [0, 100].
 */
@Entity
@Table(name = "CICLO_PROYECTO")
@Getter
@Setter
@NoArgsConstructor
public class CicloProyectoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqCicloProyecto")
    @SequenceGenerator(name = "seqCicloProyecto",
            sequenceName = "SEQ_CICLO_PROYECTO", allocationSize = 1)
    @Column(name = "ID_CICLO", nullable = false)
    private Long id;

    @Column(name = "ID_PROYECTO", nullable = false)
    private Long idProyecto;

    @Column(name = "PERIODO", nullable = false, length = 20)
    private String periodo;

    @Column(name = "NUMERO_VERSION", nullable = false)
    private int numeroVersion;

    @Column(name = "ID_VERSION_ANTERIOR")
    private Long idVersionAnterior;

    @Column(name = "OBJETIVOS", length = 2000)
    private String objetivos;

    @Column(name = "ACTIVIDADES", length = 2000)
    private String actividades;

    @Column(name = "AVANCE", precision = 5, scale = 2)
    private Integer avance;

    @Column(name = "DIFICULTADES", length = 2000)
    private String dificultades;

    @Column(name = "PROXIMAS_ACCIONES", length = 2000)
    private String proximasAcciones;

    @Column(name = "CERRADO", nullable = false, length = 1)
    private String cerrado = "N";

    @Column(name = "CREADO_POR", nullable = false, length = 100)
    private String creadoPor;

    @Column(name = "FECHA_CREACION", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "FECHA_CIERRE")
    private LocalDateTime fechaCierre;
}
