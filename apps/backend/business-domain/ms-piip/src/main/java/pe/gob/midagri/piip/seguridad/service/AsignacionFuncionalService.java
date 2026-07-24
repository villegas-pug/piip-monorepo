package pe.gob.midagri.piip.seguridad.service;

import pe.gob.midagri.piip.seguridad.dto.AssignmentAuthContext;
import pe.gob.midagri.piip.seguridad.dto.AssignmentChangeRequest;
import pe.gob.midagri.piip.seguridad.dto.AssignmentDetail;
import pe.gob.midagri.piip.seguridad.dto.AssignmentRequest;
import pe.gob.midagri.piip.seguridad.dto.RevocationRequest;

public interface AsignacionFuncionalService {
    AssignmentDetail crear(AssignmentRequest request, AssignmentAuthContext contexto);
    AssignmentDetail cambiar(Long asignacionId, AssignmentChangeRequest request, Long versionEsperada,
            AssignmentAuthContext contexto);
    AssignmentDetail revocar(Long asignacionId, RevocationRequest request, AssignmentAuthContext contexto);
}
