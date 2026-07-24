package pe.gob.midagri.piip.seguridad.service;

import org.springframework.data.domain.Page;
import pe.gob.midagri.piip.seguridad.dto.MatrixAuthContext;
import pe.gob.midagri.piip.seguridad.dto.MatrixCombination;
import pe.gob.midagri.piip.seguridad.dto.MatrixDeactivationRequest;
import pe.gob.midagri.piip.seguridad.dto.MatrixFunction;
import pe.gob.midagri.piip.seguridad.dto.MatrixVersionDetail;
import pe.gob.midagri.piip.seguridad.dto.MatrixVersionRequest;

public interface MatrizAsignacionService {
    MatrixVersionDetail crearVersion(MatrixVersionRequest request, MatrixAuthContext contexto);
    java.util.List<MatrixFunction> listarFunciones(MatrixAuthContext contexto);
    Page<MatrixVersionDetail> listarVersiones(int pagina, int tamanio, MatrixAuthContext contexto);
    java.util.List<MatrixCombination> listarCombinaciones(Long versionId, MatrixAuthContext contexto);
    MatrixVersionDetail inactivarCombinacion(Long combinacionId, MatrixDeactivationRequest request,
            MatrixAuthContext contexto);
}
