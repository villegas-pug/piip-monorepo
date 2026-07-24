package pe.gob.midagri.piip.portafolio.service.impl;

import java.util.Optional;

import org.springframework.stereotype.Service;

import pe.gob.midagri.piip.portafolio.service.CatalogoUnidadReader;
import pe.gob.midagri.piip.seguridad.entity.UnidadEjecutoraEntity;
import pe.gob.midagri.piip.seguridad.repository.UnidadEjecutoraRepository;

/**
 * Implementación interina del puerto {@link CatalogoUnidadReader}.
 *
 * <p>El catálogo canónico de unidades ejecutoras reside en el módulo {@code seguridad}
 * ({@code UNIDAD_EJECUTORA}), y su atributo {@code codigo} es la fuente formalmente aprobada
 * del prefijo del código PIIP. La constitución exige que los módulos se comuniquen solo
 * mediante servicios, DTO o eventos; este adaptador existe como puente controlado hasta que
 * el servicio oficial de catálogo de unidades se exponga en {@code seguridad} (dependencia
 * prevista en US6).
 *
 * <p>Cuando dicho servicio exista, esta implementación debe reemplazarse para delegar en él
 * y el módulo {@code portafolio} no seguirá importando el repositorio de otro módulo. El
 * cambio queda registrado como {@code NEEDS CLARIFICATION} en el handoff de la tarea T049.
 */
@Service
public class CatalogoUnidadReaderImpl implements CatalogoUnidadReader {

    private static final String ESTADO_ACTIVO = "S";

    private final UnidadEjecutoraRepository unidadEjecutoraRepository;

    public CatalogoUnidadReaderImpl(UnidadEjecutoraRepository unidadEjecutoraRepository) {
        this.unidadEjecutoraRepository = unidadEjecutoraRepository;
    }

    @Override
    public Optional<String> prefijoUnidad(Long unidadId) {
        if (unidadId == null) {
            return Optional.empty();
        }
        Optional<UnidadEjecutoraEntity> unidad = unidadEjecutoraRepository.findById(unidadId);
        if (unidad.isEmpty()) {
            return Optional.empty();
        }
        if (!ESTADO_ACTIVO.equals(unidad.get().getActivo())) {
            return Optional.empty();
        }
        String codigo = unidad.get().getCodigo();
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(codigo);
    }
}
