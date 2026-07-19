package pe.gob.midagri.piip.tipodocumento.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.gob.midagri.piip.tipodocumento.dto.TipoDocumentoResponse;
import pe.gob.midagri.piip.tipodocumento.exception.TipoDocumentoNotFoundException;
import pe.gob.midagri.piip.tipodocumento.mapper.TipoDocumentoMapper;
import pe.gob.midagri.piip.tipodocumento.repository.TipoDocumentoRepository;
import pe.gob.midagri.piip.tipodocumento.service.TipoDocumentoService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TipoDocumentoServiceImpl implements TipoDocumentoService {

    private final TipoDocumentoRepository repository;
    private final TipoDocumentoMapper mapper;

    @Override
    public List<TipoDocumentoResponse> findAll() {
        return this.repository.findAll().stream().map(this.mapper::toResponse).toList();
    }

    @Override
    public TipoDocumentoResponse findById(Integer id) {
        return this.repository.findById(id)
                .map(this.mapper::toResponse)
                .orElseThrow(() -> new TipoDocumentoNotFoundException(id));
    }
}
