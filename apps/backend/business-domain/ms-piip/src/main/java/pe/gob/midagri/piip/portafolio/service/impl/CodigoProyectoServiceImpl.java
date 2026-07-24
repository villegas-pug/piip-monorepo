package pe.gob.midagri.piip.portafolio.service.impl;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import pe.gob.midagri.piip.portafolio.entity.SecuenciaCodigoEntity;
import pe.gob.midagri.piip.portafolio.exception.PortafolioValidationException;
import pe.gob.midagri.piip.portafolio.repository.SecuenciaCodigoRepository;
import pe.gob.midagri.piip.portafolio.service.CodigoProyectoService;

/**
 * Genera el código PIIP secuencial por unidad ejecutora bajo {@code PESSIMISTIC_WRITE}.
 * Formato: {@code AAAA-PREFIJO_UNIDAD-NNNNN}.
 *
 * <p>La consulta con bloqueo pesimista y el incremento del correlativo se ejecutan dentro de
 * la misma transacción de negocio, por lo que el resultado es atómico respecto al commit. La
 * creación de la secuencia cuando no existe puede coincidir con otra transacción concurrente;
 * en ese caso, la violación transitoria de la UK (ANIO, ID_UNIDAD) se resuelve con un
 * reintento dentro de la misma transacción lógica para mantener la idempotencia del correlativo.
 */
@Service
public class CodigoProyectoServiceImpl implements CodigoProyectoService {

    private static final int CODIGO_LARGO_MAXIMO = 25;

    private final SecuenciaCodigoRepository secuenciaCodigoRepository;

    public CodigoProyectoServiceImpl(SecuenciaCodigoRepository secuenciaCodigoRepository) {
        this.secuenciaCodigoRepository = secuenciaCodigoRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public String generarCodigo(Integer anio, Long unidadId, String prefijo) {
        if (anio == null || unidadId == null) {
            throw new PortafolioValidationException("VALIDATION_FAILED",
                    "El año y la unidad ejecutora son obligatorios para generar el correlativo.");
        }
        if (prefijo == null || prefijo.isBlank()) {
            throw PortafolioValidationException.prefijoNoDisponible();
        }

        SecuenciaCodigoEntity secuencia = cargarSecuenciaConPersistenciaInicial(anio, unidadId);

        secuencia.setUltimoNumero(secuencia.getUltimoNumero() + 1);
        secuencia = secuenciaCodigoRepository.save(secuencia);

        String numeroFormateado = String.format("%05d", secuencia.getUltimoNumero());
        String codigo = anio + "-" + prefijo + "-" + numeroFormateado;

        if (codigo.length() > CODIGO_LARGO_MAXIMO) {
            throw new PortafolioValidationException("CODE_TOO_LONG",
                    "El código generado excede la longitud máxima de " + CODIGO_LARGO_MAXIMO + " caracteres.");
        }

        return codigo;
    }

    /**
     * Recupera la secuencia bajo bloqueo pesimista. Si no existe, intenta crearla y,
     * ante la violación transitoria de la UK por concurrencia, vuelve a leer bajo bloqueo.
     */
    private SecuenciaCodigoEntity cargarSecuenciaConPersistenciaInicial(Integer anio, Long unidadId) {
        return secuenciaCodigoRepository.findForUpdate(anio, unidadId)
                .orElseGet(() -> crearSecuenciaConReintento(anio, unidadId));
    }

    private SecuenciaCodigoEntity crearSecuenciaConReintento(Integer anio, Long unidadId) {
        SecuenciaCodigoEntity nueva = new SecuenciaCodigoEntity();
        nueva.setAnio(anio);
        nueva.setUnidadId(unidadId);
        nueva.setUltimoNumero(0);
        try {
            return secuenciaCodigoRepository.save(nueva);
        } catch (DataIntegrityViolationException carrera) {
            // Una transacción concurrente creó la secuencia entre el findForUpdate vacío
            // y el INSERT actual. Reanudamos el bloqueo pesimista sobre la fila confirmada.
            return secuenciaCodigoRepository.findForUpdate(anio, unidadId)
                    .orElseThrow(() -> new IllegalStateException(
                            "La secuencia de código para el año " + anio + " y la unidad "
                                    + unidadId + " no quedó disponible tras la carrera de inserción.",
                            carrera));
        }
    }
}
