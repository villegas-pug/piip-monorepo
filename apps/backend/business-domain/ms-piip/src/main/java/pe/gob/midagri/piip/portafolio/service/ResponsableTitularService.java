package pe.gob.midagri.piip.portafolio.service;

import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.dto.ResponsibleReplacementDetail;
import pe.gob.midagri.piip.portafolio.dto.ResponsibleReplacementRequest;

/** Caso de uso propietario de la sustitución de titularidad del portafolio. */
public interface ResponsableTitularService {

    ResponsibleReplacementDetail sustituir(
            Long registroId, ResponsibleReplacementRequest comando, PortafolioAuthContext contexto);
}
