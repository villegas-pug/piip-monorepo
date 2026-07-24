package pe.gob.midagri.piip.documentos.service;

import pe.gob.midagri.piip.documentos.dto.CreateInstitutionalFileCommand;
import pe.gob.midagri.piip.documentos.dto.DocumentoAuthorizedContext;
import pe.gob.midagri.piip.documentos.dto.ExpedienteInstitucionalDetail;

public interface ExpedienteInstitucionalService {
    ExpedienteInstitucionalDetail crear(DocumentoAuthorizedContext contexto, CreateInstitutionalFileCommand comando);
}
