package pe.gob.midagri.piip.tipodocumento.service;

import java.util.List;

import pe.gob.midagri.piip.tipodocumento.dto.TipoDocumentoResponse;

public interface TipoDocumentoService {

    List<TipoDocumentoResponse> findAll();

    TipoDocumentoResponse findById(Integer id);
}
