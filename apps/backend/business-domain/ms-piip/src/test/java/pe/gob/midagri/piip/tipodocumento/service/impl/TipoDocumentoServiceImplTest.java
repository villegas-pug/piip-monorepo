package pe.gob.midagri.piip.tipodocumento.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.gob.midagri.piip.tipodocumento.dto.TipoDocumentoResponse;
import pe.gob.midagri.piip.tipodocumento.entity.TipoDocumentoEntity;
import pe.gob.midagri.piip.tipodocumento.exception.TipoDocumentoNotFoundException;
import pe.gob.midagri.piip.tipodocumento.mapper.TipoDocumentoMapper;
import pe.gob.midagri.piip.tipodocumento.repository.TipoDocumentoRepository;

@ExtendWith(MockitoExtension.class)
class TipoDocumentoServiceImplTest {

    @Mock
    private TipoDocumentoRepository repository;

    @Mock
    private TipoDocumentoMapper mapper;

    @InjectMocks
    private TipoDocumentoServiceImpl service;

    @Test
    void findAllReturnsMappedCatalogEntries() {
        TipoDocumentoEntity entity = new TipoDocumentoEntity();
        TipoDocumentoResponse response = new TipoDocumentoResponse(1, "Ficha", "PRESENTADO", true, null, null, true);
        when(this.repository.findAll()).thenReturn(List.of(entity));
        when(this.mapper.toResponse(entity)).thenReturn(response);

        assertThat(this.service.findAll()).containsExactly(response);
    }

    @Test
    void findByIdRejectsAnUnknownCatalogEntry() {
        when(this.repository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.service.findById(99))
                .isInstanceOf(TipoDocumentoNotFoundException.class);
    }
}
