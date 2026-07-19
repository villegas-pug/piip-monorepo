package pe.gob.midagri.piip.tipodocumento.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "TIPO_DOCUMENTO")
@Getter
@Setter
public class TipoDocumentoEntity {

    @Id
    @Column(name = "ID_TIPO_DOC")
    private Integer id;

    @Column(name = "NOMBRE")
    private String nombre;

    @Column(name = "ESTADO_ASOCIADO")
    private String estadoAsociado;

    @Column(name = "OBLIGATORIO")
    private String obligatorio;

    @Column(name = "DESCRIPCION")
    private String descripcion;

    @Column(name = "ANEXO_NT")
    private String anexoNormativo;

    @Column(name = "ACTIVO")
    private String activo;
}
