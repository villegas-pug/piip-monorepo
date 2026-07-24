package pe.gob.midagri.piip.portafolio.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mapea la tabla PROYECTO y representa una INICIATIVA o un PROYECTO.
 * Código UK e inmutable con formato AAAA-PREFIJO_UNIDAD-NNNNN.
 */
@Entity
@Table(name = "PROYECTO")
@Getter @Setter @NoArgsConstructor
public class RegistroPortafolioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqProyecto")
    @SequenceGenerator(name = "seqProyecto", sequenceName = "SEQ_PROYECTO", allocationSize = 1)
    @Column(name = "ID_PROYECTO", nullable = false)
    private Long id;

    @Column(name = "CODIGO", nullable = false, length = 25)
    private String codigo;

    @Column(name = "CODIGO_ORIGEN", length = 50)
    private String codigoOrigen;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_REGISTRO", nullable = false, length = 20)
    private TipoRegistro tipoRegistro;

    @Column(name = "NOMBRE", nullable = false, length = 500)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_SOLUCION", nullable = false, length = 30)
    private TipoSolucion tipoSolucion;

    @Enumerated(EnumType.STRING)
    @Column(name = "FUENTE_ORIGEN", nullable = false, length = 50)
    private FuenteOrigen fuenteOrigen;

    @Column(name = "DETALLE_FUENTE", length = 500)
    private String detalleFuente;

    @Lob
    @Column(name = "DESCRIPCION", nullable = false)
    private String descripcion;

    @Column(name = "OBJETIVO_PEI", nullable = false, length = 500)
    private String objetivoPei;

    @Column(name = "ACTIVIDAD_POI", nullable = false, length = 500)
    private String actividadPoi;

    @Column(name = "ADMINISTRACION", length = 10)
    private String administracion;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 30)
    private EstadoIniciativa estado;

    @Column(name = "TIPO_PRODUCTO_FINAL", length = 40)
    private String tipoProductoFinal;

    @Lob
    @Column(name = "RESULTADOS_CLAVE")
    private String resultadosClave;

    @Column(name = "FECHA_INICIO", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "FECHA_CIERRE")
    private LocalDate fechaCierre;

    @Column(name = "ID_UNIDAD_EJECUTORA", nullable = false)
    private Long unidadEjecutoraId;

    @Column(name = "ID_RESPONSABLE", nullable = false)
    private Long responsableId;

    @Column(name = "CREADO_POR", nullable = false, length = 100)
    private String creadoPor;

    @Column(name = "FECHA_CREACION", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "MODIFICADO_POR", length = 100)
    private String modificadoPor;

    @Column(name = "FECHA_MODIFICACION")
    private LocalDateTime fechaModificacion;

    @Column(name = "CODIGO_PREFIJO", length = 20)
    private String codigoPrefijo;

    @Column(name = "PROBLEMA_PUBLICO", length = 2000)
    private String problemaPublico;

    @Column(name = "SOLUCION_PROPUESTA", length = 2000)
    private String solucionPropuesta;

    @Column(name = "COMPONENTE_DIGITAL", nullable = false, length = 1)
    private String componenteDigital;

    @Column(name = "DETALLE_COMPONENTE_DIGITAL", length = 500)
    private String detalleComponenteDigital;

    @Column(name = "NOTA", length = 1000)
    private String nota;

    @Column(name = "OBJETIVO_PEI_ID")
    private Long objetivoPeiId;

    @Column(name = "ACTIVIDAD_POI_ID")
    private Long actividadPoiId;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Long version;

    @Column(name = "SUBSANACION_ACTIVA", nullable = false, length = 1)
    private String subsanacionActiva;
}
