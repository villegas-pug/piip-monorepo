package pe.gob.midagri.piip.seguridad.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import pe.gob.midagri.piip.seguridad.entity.RolEntity;
import pe.gob.midagri.piip.seguridad.entity.MatrizFuncionEntity;
import pe.gob.midagri.piip.seguridad.entity.MatrizFuncionPerfilUnidadEntity;
import pe.gob.midagri.piip.seguridad.entity.UnidadEjecutoraEntity;
import pe.gob.midagri.piip.seguridad.entity.UsuarioEntity;
import pe.gob.midagri.piip.seguridad.entity.UsuarioRolUnidadEntity;
import pe.gob.midagri.piip.seguridad.dto.EffectiveAssignmentOption;
import pe.gob.midagri.piip.seguridad.repository.MatrizFuncionPerfilUnidadRepository;
import pe.gob.midagri.piip.seguridad.repository.MatrizFuncionRepository;
import pe.gob.midagri.piip.seguridad.repository.RolRepository;
import pe.gob.midagri.piip.seguridad.repository.UnidadEjecutoraRepository;
import pe.gob.midagri.piip.seguridad.repository.UsuarioRepository;
import pe.gob.midagri.piip.seguridad.repository.UsuarioRolUnidadRepository;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService;

/** Implementación JPA de la autorización efectiva cuya fuente es Oracle PIIP. */
@Service
public class AutorizacionEfectivaServiceImpl implements AutorizacionEfectivaService {

    private static final String ASSIGNMENT_SCOPE_DENIED = "ASSIGNMENT_SCOPE_DENIED";

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolUnidadRepository usuarioRolUnidadRepository;
    private final MatrizFuncionPerfilUnidadRepository matrizCombinacionRepository;
    private final MatrizFuncionRepository matrizFuncionRepository;
    private final UnidadEjecutoraRepository unidadEjecutoraRepository;
    private final pe.gob.midagri.piip.seguridad.repository.SuplenciaFuncionalRepository suplenciaRepository;

    public AutorizacionEfectivaServiceImpl(
            RolRepository rolRepository,
            UsuarioRepository usuarioRepository,
            UsuarioRolUnidadRepository usuarioRolUnidadRepository,
            MatrizFuncionPerfilUnidadRepository matrizCombinacionRepository,
            MatrizFuncionRepository matrizFuncionRepository,
            UnidadEjecutoraRepository unidadEjecutoraRepository,
            pe.gob.midagri.piip.seguridad.repository.SuplenciaFuncionalRepository suplenciaRepository) {
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioRolUnidadRepository = usuarioRolUnidadRepository;
        this.matrizCombinacionRepository = matrizCombinacionRepository;
        this.matrizFuncionRepository = matrizFuncionRepository;
        this.unidadEjecutoraRepository = unidadEjecutoraRepository;
        this.suplenciaRepository = suplenciaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EffectiveAssignmentOption> listarAsignacionesPropias(String sub) {
        if (esVacio(sub)) {
            return List.of();
        }

        return usuarioRepository.findByKeycloakId(sub)
                .map(usuario -> usuarioRolUnidadRepository.findByUsuarioIdOrderByFechaInicioDesc(usuario.getId())
                        .stream()
                        .map(asignacion -> aOpcion(usuario, asignacion))
                        .toList())
                .orElseGet(List::of);
    }

    /**
     * El bloqueo pesimista evita que una revocación, cambio de vigencia o suplencia concurrente
     * deje vigente una comprobación realizada antes de la mutación del recurso.
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public AsignacionEfectiva revalidarParaOperacionSensible(
            String sub, Long asignacionId, String perfilRequerido, Long unidadRecursoId) {
        if (esVacio(sub) || asignacionId == null || esVacio(perfilRequerido) || unidadRecursoId == null) {
            throw accesoDenegado();
        }

        RolEntity perfil = rolRepository.findByNombre(perfilRequerido)
                .orElseThrow(this::accesoDenegado);

        UsuarioRolUnidadEntity asignacion = usuarioRolUnidadRepository
                .findAsignacionEfectivaForUpdate(asignacionId, sub, perfil.getId(), unidadRecursoId)
                .orElseThrow(this::accesoDenegado);

        return new AsignacionEfectiva(
                asignacion.getId(),
                asignacion.getUsuarioId(),
                asignacion.getCombinacionMatrizId(),
                perfil.getNombre(),
                asignacion.getUnidadId());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public AsignacionEfectiva revalidarAsignacionInstitucional(String sub, Long asignacionId, Long unidadRecursoId) {
        if (esVacio(sub) || asignacionId == null || unidadRecursoId == null) throw accesoDenegado();
        UsuarioRolUnidadEntity asignacion = usuarioRolUnidadRepository
                .findAsignacionInstitucionalForUpdate(asignacionId, sub, unidadRecursoId)
                .orElseThrow(this::accesoDenegado);
        RolEntity perfil = rolRepository.findById(asignacion.getRolId()).orElseThrow(this::accesoDenegado);
        return new AsignacionEfectiva(asignacion.getId(), asignacion.getUsuarioId(),
                asignacion.getCombinacionMatrizId(), perfil.getNombre(), asignacion.getUnidadId());
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private EffectiveAssignmentOption aOpcion(UsuarioEntity usuario, UsuarioRolUnidadEntity asignacion) {
        MatrizFuncionPerfilUnidadEntity combinacion = asignacion.getCombinacionMatrizId() == null
                ? null
                : matrizCombinacionRepository.findById(asignacion.getCombinacionMatrizId()).orElse(null);
        RolEntity perfil = rolRepository.findById(asignacion.getRolId()).orElse(null);
        UnidadEjecutoraEntity unidad = unidadEjecutoraRepository.findById(asignacion.getUnidadId()).orElse(null);
        MatrizFuncionEntity funcion = combinacion == null
                ? null
                : matrizFuncionRepository.findById(combinacion.getFuncionId()).orElse(null);

        return new EffectiveAssignmentOption(
                asignacion.getId(),
                asignacion.getCombinacionMatrizId(),
                funcion == null ? null : funcion.getDescripcion(),
                perfil == null ? null : perfil.getNombre(),
                unidad == null ? null : unidad.getNombre(),
                asignacion.getFechaInicio(),
                asignacion.getFechaFin(),
                estadoEfectivo(usuario, asignacion, combinacion));
    }

    private String estadoEfectivo(
            UsuarioEntity usuario,
            UsuarioRolUnidadEntity asignacion,
            MatrizFuncionPerfilUnidadEntity combinacion) {
        LocalDate hoy = LocalDate.now();
        if (!"S".equals(usuario.getActivo())) {
            return "USUARIO_INACTIVO";
        }
        if (!"S".equals(asignacion.getActivo())) {
            return "INACTIVA";
        }
        if (asignacion.getRevocadaEn() != null) {
            return "REVOCADA";
        }
        if (suplenciaRepository.existsVigenteParaTitular(asignacion.getId())) {
            return "INACTIVA_TEMPORALMENTE";
        }
        if (asignacion.getFechaInicio().isAfter(hoy)) {
            return "PENDIENTE";
        }
        if (asignacion.getFechaFin() != null && asignacion.getFechaFin().isBefore(hoy)) {
            return "VENCIDA";
        }
        if (combinacion == null || !combinacionVigente(combinacion, hoy)) {
            return "COMBINACION_NO_VIGENTE";
        }
        return "VIGENTE";
    }

    private boolean combinacionVigente(MatrizFuncionPerfilUnidadEntity combinacion, LocalDate hoy) {
        return "S".equals(combinacion.getActiva())
                && !combinacion.getVigenteDesde().isAfter(hoy)
                && (combinacion.getVigenteHasta() == null || !combinacion.getVigenteHasta().isBefore(hoy));
    }

    private ResponseStatusException accesoDenegado() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, ASSIGNMENT_SCOPE_DENIED);
    }
}
