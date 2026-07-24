package pe.gob.midagri.piip.portafolio.seguimiento.service.impl;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import pe.gob.midagri.piip.documentos.service.DocumentoService;
import pe.gob.midagri.piip.portafolio.seguimiento.service.AptitudDocumentalService;

/**
 * Implementacion por defecto del servicio de aptitud documental
 * (US4, Constitucion 5.0.0). Delegada en el servicio transversal
 * de {@code DocumentoService} cuando esta disponible; si el bean
 * no esta inyectado (modo aislado o pruebas), aplica la politica
 * conservadora: solo se consideran aptos los tipos documentales
 * del catalogo canonico del modulo seguimiento.
 *
 * <p>Los tipos documentales validos para evidencia de un ciclo son
 * los definidos en el DDL 015 y la Constitucion:
 * {@code AutoevaluacionCicloTrabajo},
 * {@code SeguimientoAgilTableroKanban} y
 * {@code MatrizPlanificacionCiclos}. Cualquier otro tipo se
 * rechaza con {@code EVIDENCE_TYPE_NOT_ALLOWED} en la capa de
 * servicio.
 */
@Service
public class AptitudDocumentalServiceImpl implements AptitudDocumentalService {

    private static final Logger LOG =
            LoggerFactory.getLogger(AptitudDocumentalServiceImpl.class);

    /** Tipos documentales canonicos para evidencia de un ciclo. */
    public static final Set<String> TIPOS_CICLO_VALIDOS = Set.of(
            "AutoevaluacionCicloTrabajo",
            "SeguimientoAgilTableroKanban",
            "MatrizPlanificacionCiclos",
            "DocumentacionProductoFinal",
            "EvidenciaProductoFinal");

    private DocumentoService documentoService;

    @Autowired(required = false)
    public void setDocumentoService(DocumentoService documentoService) {
        this.documentoService = documentoService;
    }

    @Override
    public boolean esApto(long documentoId, String tipoDocumental) {
        if (tipoDocumental == null
                || !TIPOS_CICLO_VALIDOS.contains(tipoDocumental)) {
            return false;
        }
        if (documentoService == null) {
            // Modo aislado: si no podemos consultar el servicio de
            // documentos, conservamos la regla del tipo documental
            // canonico pero advertimos en el log para que la
            // operacion sea trazable.
            LOG.warn("DocumentoService no disponible; evaluando aptitud "
                    + "solo por tipo documental canonico. documentoId={}",
                    documentoId);
            return true;
        }
        try {
            return documentoService.validarEvidencia(documentoId, tipoDocumental).apto();
        } catch (RuntimeException ex) {
            LOG.warn("Fallo evaluando aptitud documental {}#{}: {}",
                    tipoDocumental, documentoId, ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean esAptoParaTransicion(long documentoId,
            TipoEvidenciaTransicion tipo) {
        if (tipo == null || documentoService == null) {
            return false;
        }
        try {
            return documentoService.validarEvidencia(documentoId,
                    tipo.tipoDocumental()).apto();
        } catch (RuntimeException ex) {
            LOG.warn("Fallo evaluando evidencia de transicion {}#{}: {}",
                    tipo.tipoDocumental(), documentoId, ex.getMessage());
            return false;
        }
    }
}
