package pe.gob.midagri.piip.documentos.dto;

import pe.gob.midagri.piip.documentos.entity.ContextoTipoDocumento;

/**
 * Propietario excluyente (XOR) de la serie documental en la carga inicial.
 * Exactamente uno de los identificadores debe ser no nulo y coincidir con el tipo declarado.
 * La pertenencia se conserva inmutable una vez formalizada la serie.
 */
public record DocumentOwnerDto(
        ContextoTipoDocumento tipo,
        Long registroPortafolioId,
        Long expedienteInstitucionalId) {

    /**
     * En esta fase T023 solo se admite propietario institucional; el controlador aplica
     * esta invariante antes de delegar al servicio para conservar la regla XOR y la
     * pertenencia inmutable exigidas por la constitucion.
     */
    public boolean esExpedienteInstitucional() {
        return tipo == ContextoTipoDocumento.INSTITUCIONAL
                && expedienteInstitucionalId != null
                && registroPortafolioId == null;
    }
}
