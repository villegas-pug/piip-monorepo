package pe.gob.midagri.piip.documentos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "TIPO_DOCUMENTO")
@Getter @Setter
public class TipoDocumentoEntity {
    @Id @Column(name = "ID_TIPO_DOC") private Integer id;
    @Column(name = "NOMBRE", nullable = false) private String nombre;
    @Column(name = "ESTADO_ASOCIADO") private String estadoAsociado;
    @Column(name = "OBLIGATORIO", nullable = false) private String obligatorio;
    @Column(name = "DESCRIPCION") private String descripcion;
    @Column(name = "ANEXO_NT") private String anexoNormativo;
    @Column(name = "ACTIVO", nullable = false) private String activo;
    @Enumerated(EnumType.STRING) @Column(name = "CONTEXTO", nullable = false) private ContextoTipoDocumento contexto;
    @Enumerated(EnumType.STRING) @Column(name = "CLASIFICACION_DEFECTO") private ClasificacionDocumento clasificacionDefecto;
}
