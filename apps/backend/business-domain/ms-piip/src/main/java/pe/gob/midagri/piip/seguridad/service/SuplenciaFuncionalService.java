package pe.gob.midagri.piip.seguridad.service;

import pe.gob.midagri.piip.seguridad.dto.AssignmentAuthContext;
import pe.gob.midagri.piip.seguridad.dto.EarlyTerminationRequest;
import pe.gob.midagri.piip.seguridad.dto.SubstitutionDetail;
import pe.gob.midagri.piip.seguridad.dto.SubstitutionRequest;

/** Contrato del caso de uso de suplencias funcionales. */
public interface SuplenciaFuncionalService {
    SubstitutionDetail crear(Long titularId, SubstitutionRequest request, AssignmentAuthContext contexto);
    SubstitutionDetail terminarAnticipadamente(Long suplenciaId, EarlyTerminationRequest request,
            AssignmentAuthContext contexto);
}
