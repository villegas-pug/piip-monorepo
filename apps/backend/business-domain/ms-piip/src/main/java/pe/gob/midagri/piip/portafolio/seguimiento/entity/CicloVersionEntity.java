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
 * Modelo logico de la cadena append-only de versiones de un ciclo
 * del proyecto (US4, Constitucion 5.0.0). Conceptualmente cada
 * correccion crea una nueva fila conservando la anterior; el
 * contrato del repositorio {@code CicloVersionRepository} permite
 * persistir y consultar este historial sin destruir las versiones
 * previas. El DDL canonico final se publicara en un script
 * posterior; T072 fija la API y la firma JPA como contrato
 * estabilizador para los servicios y controladores de US4.
 */
@Entity
@Table(name = "CICLO_PROYECTO_VERSION")
@Getter
@Setter
@NoArgsConstructor
public class CicloVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqCicloProyectoVersion")
    @SequenceGenerator(name = "seqCicloProyectoVersion",
            sequenceName = "SEQ_CICLO_PROYECTO_VERSION", allocationSize = 1)
    @Column(name = "ID_VERSION", nullable = false)
    private Long id;

    @Column(name = "ID_CICLO", nullable = false)
    private Long idCiclo;

    @Column(name = "NUMERO_VERSION", nullable = false)
    private int numeroVersion;

    @Column(name = "MOTIVO", length = 2000)
    private String motivo;

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

    @Column(name = "CREADO_POR", nullable = false, length = 100)
    private String creadoPor;

    @Column(name = "FECHA_CREACION", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;
}
