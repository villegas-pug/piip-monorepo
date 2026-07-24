package pe.gob.midagri.piip.organizacion.entity;

import java.time.LocalDate;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "CAT_ACTIVIDAD_POI_VERSION") @Getter @Setter
public class ActividadPoiVersionEntity {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqActividadPoiVersion")
    @SequenceGenerator(name = "seqActividadPoiVersion", sequenceName = "SEQ_ACTIVIDAD_POI_VERSION", allocationSize = 1)
    @Column(name = "ID_VERSION") private Long id;
    @Column(name = "CODIGO_VERSION", nullable = false, length = 30) private String codigoVersion;
    @Column(name = "ID_VERSION_ANTERIOR") private Long versionAnteriorId;
    @Column(name = "ID_DOCUMENTO_APROBACION", nullable = false) private Long documentoAprobacionId;
    @Column(name = "OFICINA_APROBADORA", nullable = false, length = 200) private String oficinaAprobadora;
    @Column(name = "VIGENTE_DESDE", nullable = false) private LocalDate vigenteDesde;
    @Column(name = "VIGENTE_HASTA") private LocalDate vigenteHasta;
    @Column(name = "ACTIVA", nullable = false, length = 1) private String activa;
    @Column(name = "CREADO_POR", nullable = false, length = 100, updatable = false) private String creadoPor;
}
