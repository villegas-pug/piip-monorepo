package pe.gob.midagri.piip.tipodocumento.dto;

public record TipoDocumentoResponse(
        Integer id,
        String nombre,
        String estadoAsociado,
        boolean obligatorio,
        String descripcion,
        String anexoNormativo,
        boolean activo) {
}
