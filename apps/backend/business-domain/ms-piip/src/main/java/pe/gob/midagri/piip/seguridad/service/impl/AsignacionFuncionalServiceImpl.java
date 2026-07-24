package pe.gob.midagri.piip.seguridad.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.gob.midagri.piip.auditoria.service.AuditService;
import pe.gob.midagri.piip.documentos.service.DocumentoService;
import pe.gob.midagri.piip.seguridad.dto.AssignmentAuthContext;
import pe.gob.midagri.piip.seguridad.dto.AssignmentChangeRequest;
import pe.gob.midagri.piip.seguridad.dto.AssignmentDetail;
import pe.gob.midagri.piip.seguridad.dto.AssignmentRequest;
import pe.gob.midagri.piip.seguridad.dto.RevocationRequest;
import pe.gob.midagri.piip.seguridad.entity.MatrizFuncionPerfilUnidadEntity;
import pe.gob.midagri.piip.seguridad.entity.RolEntity;
import pe.gob.midagri.piip.seguridad.entity.TipoEventoAsignacion;
import pe.gob.midagri.piip.seguridad.entity.UsuarioRolUnidadEntity;
import pe.gob.midagri.piip.seguridad.entity.UsuarioRolUnidadEventoEntity;
import pe.gob.midagri.piip.seguridad.repository.MatrizFuncionPerfilUnidadRepository;
import pe.gob.midagri.piip.seguridad.repository.RolRepository;
import pe.gob.midagri.piip.seguridad.repository.UsuarioRepository;
import pe.gob.midagri.piip.seguridad.repository.UsuarioRolUnidadEventoRepository;
import pe.gob.midagri.piip.seguridad.repository.UsuarioRolUnidadRepository;
import pe.gob.midagri.piip.seguridad.service.AsignacionFuncionalService;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService;

/** Autoridad única JPA para el ciclo ordinario de asignaciones funcionales. */
@Service
public class AsignacionFuncionalServiceImpl implements AsignacionFuncionalService {
    private static final String GLOBAL_ADMIN = "GlobalAdmin";
    private static final String UNIDAD_ADMIN = "UnidadAdmin";
    private static final String EVALUADOR = "Evaluador";
    private static final String AUTORIDAD = "Autoridad";
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final MatrizFuncionPerfilUnidadRepository combinaciones;
    private final UsuarioRolUnidadRepository asignaciones;
    private final UsuarioRolUnidadEventoRepository eventos;
    private final DocumentoService documentos;
    private final AutorizacionEfectivaService autorizacion;
    private final AuditService auditoria;

    public AsignacionFuncionalServiceImpl(UsuarioRepository usuarios, RolRepository roles,
            MatrizFuncionPerfilUnidadRepository combinaciones, UsuarioRolUnidadRepository asignaciones,
            UsuarioRolUnidadEventoRepository eventos, DocumentoService documentos,
            AutorizacionEfectivaService autorizacion, AuditService auditoria) {
        this.usuarios = usuarios; this.roles = roles; this.combinaciones = combinaciones;
        this.asignaciones = asignaciones; this.eventos = eventos; this.documentos = documentos;
        this.autorizacion = autorizacion; this.auditoria = auditoria;
    }

    @Override @Transactional
    public AssignmentDetail crear(AssignmentRequest request, AssignmentAuthContext contexto) {
        validarPeriodo(request.fechaInicio(), request.fechaFin(), contexto, null, null);
        if (usuarios.findById(request.usuarioId()).filter(u -> "S".equals(u.getActivo())).isEmpty())
            throw rechazar(contexto, null, "ASSIGNMENT_USER_NOT_ACTIVE", null, HttpStatus.UNPROCESSABLE_ENTITY);
        MatrizFuncionPerfilUnidadEntity combinacion = combinaciones.findById(request.matrizCombinacionId())
                .orElseThrow(() -> rechazar(contexto, null, "MATRIX_COMBINATION_NOT_ACTIVE", null, HttpStatus.UNPROCESSABLE_ENTITY));
        RolEntity rol = roles.findById(combinacion.getRolId())
                .orElseThrow(() -> rechazar(contexto, null, "MATRIX_COMBINATION_NOT_ACTIVE", null, HttpStatus.UNPROCESSABLE_ENTITY));
        combinacion = combinaciones.findVigenteParaAsignacion(combinacion.getId(), rol.getId(), combinacion.getUnidadId(), LocalDate.now())
                .orElseThrow(() -> rechazar(contexto, null, "MATRIX_COMBINATION_NOT_ACTIVE", null, HttpStatus.UNPROCESSABLE_ENTITY));
        // El alta administrativa jamás sustituye la semilla 021: sin antecedente histórico,
        // GlobalAdmin solo puede existir por la semilla SQL manual y de ejecución única.
        if (GLOBAL_ADMIN.equals(rol.getNombre()) && !asignaciones.existsGlobalAdminHistorico())
            throw rechazar(contexto, null, "GLOBAL_ADMIN_BOOTSTRAP_SEED_REQUIRED", null, HttpStatus.CONFLICT);
        var actor = autorizar(contexto, rol.getNombre(), combinacion.getUnidadId());
        validarDocumentoSiCorresponde(rol.getNombre(), request.documentoFormalVersionId(), contexto, actor, null);
        var nueva = new UsuarioRolUnidadEntity();
        nueva.setUsuarioId(request.usuarioId()); nueva.setRolId(rol.getId()); nueva.setUnidadId(combinacion.getUnidadId());
        nueva.setCombinacionMatrizId(combinacion.getId()); nueva.setDocumentoFormalId(request.documentoFormalVersionId());
        nueva.setFechaAsignacion(LocalDate.now()); nueva.setFechaInicio(request.fechaInicio()); nueva.setFechaFin(request.fechaFin());
        nueva.setActivo("S"); nueva.setInactivaTemporalmente("N"); nueva.setAsignadoPor(contexto.actorSub());
        nueva = asignaciones.save(nueva);
        evento(nueva, TipoEventoAsignacion.ALTA, actor, null);
        exito(contexto, actor, "CREAR_ASIGNACION", nueva.getId(), Map.of("perfil", rol.getNombre()));
        return detalle(nueva, rol.getNombre());
    }

    @Override @Transactional
    public AssignmentDetail cambiar(Long id, AssignmentChangeRequest request, Long versionEsperada, AssignmentAuthContext contexto) {
        validarPeriodo(request.fechaInicio(), request.fechaFin(), contexto, null, id);
        var asignacion = asignaciones.findByIdForUpdate(id).orElseThrow(() -> rechazar(contexto, null, "ASSIGNMENT_NOT_FOUND", id, HttpStatus.NOT_FOUND));
        String perfil = perfil(asignacion.getRolId(), contexto, id);
        var actor = autorizar(contexto, perfil, asignacion.getUnidadId());
        verificarVersion(asignacion, versionEsperada, contexto, actor, id);
        if (GLOBAL_ADMIN.equals(perfil)) validarDocumentoSiCorresponde(perfil, asignacion.getDocumentoFormalId(), contexto, actor, id);
        asignacion.setFechaInicio(request.fechaInicio()); asignacion.setFechaFin(request.fechaFin());
        asignacion = asignaciones.save(asignacion);
        evento(asignacion, TipoEventoAsignacion.MODIFICACION, actor, null);
        exito(contexto, actor, "CAMBIAR_ASIGNACION", id, Map.of("perfil", perfil));
        return detalle(asignacion, perfil);
    }

    @Override @Transactional
    public AssignmentDetail revocar(Long id, RevocationRequest request, AssignmentAuthContext contexto) {
        var asignacion = asignaciones.findByIdForUpdate(id).orElseThrow(() -> rechazar(contexto, null, "ASSIGNMENT_NOT_FOUND", id, HttpStatus.NOT_FOUND));
        String perfil = perfil(asignacion.getRolId(), contexto, id);
        var actor = autorizar(contexto, perfil, asignacion.getUnidadId());
        if (asignacion.getRevocadaEn() != null) throw rechazar(contexto, actor, "ASSIGNMENT_ALREADY_REVOKED", id, HttpStatus.CONFLICT);
        if (GLOBAL_ADMIN.equals(perfil)) {
            validarDocumentoSiCorresponde(perfil, request.documentoFormalVersionId(), contexto, actor, id);
            List<UsuarioRolUnidadEntity> globales = asignaciones.findGlobalAdminsEfectivosForUpdate();
            if (globales.size() <= 1 && globales.stream().anyMatch(a -> a.getId().equals(id)))
                throw rechazar(contexto, actor, "LAST_GLOBAL_ADMIN", id, HttpStatus.CONFLICT);
        }
        asignacion.setRevocadaEn(LocalDateTime.now()); asignacion.setRevocadaPor(contexto.actorSub());
        asignacion.setMotivoRevocacion(request.motivo().trim());
        asignacion = asignaciones.save(asignacion);
        evento(asignacion, TipoEventoAsignacion.REVOCACION, actor, request.motivo().trim());
        exito(contexto, actor, "REVOCAR_ASIGNACION", id, Map.of("perfil", perfil));
        return detalle(asignacion, perfil);
    }

    private AutorizacionEfectivaService.AsignacionEfectiva autorizar(AssignmentAuthContext c, String perfilObjetivo, Long unidadObjetivo) {
        try {
            if (UNIDAD_ADMIN.equals(perfilObjetivo) || EVALUADOR.equals(perfilObjetivo) || AUTORIDAD.equals(perfilObjetivo) || GLOBAL_ADMIN.equals(perfilObjetivo))
                return autorizacion.revalidarParaOperacionSensible(c.actorSub(), c.asignacionEfectivaId(), GLOBAL_ADMIN, c.unidadEfectivaId());
            try { return autorizacion.revalidarParaOperacionSensible(c.actorSub(), c.asignacionEfectivaId(), GLOBAL_ADMIN, c.unidadEfectivaId()); }
            catch (ResponseStatusException ignored) { return autorizacion.revalidarParaOperacionSensible(c.actorSub(), c.asignacionEfectivaId(), UNIDAD_ADMIN, unidadObjetivo); }
        } catch (ResponseStatusException ex) { throw rechazar(c, null, "ASSIGNMENT_ADMIN_DENIED", null, HttpStatus.FORBIDDEN); }
    }
    private void validarDocumentoSiCorresponde(String perfil, Long documentoId, AssignmentAuthContext c, AutorizacionEfectivaService.AsignacionEfectiva a, Long recurso) {
        if ((GLOBAL_ADMIN.equals(perfil) || EVALUADOR.equals(perfil) || AUTORIDAD.equals(perfil))
                && (documentoId == null || !documentos.validarDocumentoInstitucionalAprobatorio(documentoId).valido()))
            throw rechazar(c, a, "FORMAL_DOCUMENT_REQUIRED", recurso, HttpStatus.UNPROCESSABLE_ENTITY);
    }
    private void validarPeriodo(LocalDate inicio, LocalDate fin, AssignmentAuthContext c, AutorizacionEfectivaService.AsignacionEfectiva a, Long recurso) { if (fin != null && fin.isBefore(inicio)) throw rechazar(c, a, "INVALID_VALIDITY_PERIOD", recurso, HttpStatus.UNPROCESSABLE_ENTITY); }
    private void verificarVersion(UsuarioRolUnidadEntity a, Long esperada, AssignmentAuthContext c, AutorizacionEfectivaService.AsignacionEfectiva actor, Long id) { if (esperada == null) throw rechazar(c, actor, "IF_MATCH_REQUIRED", id, HttpStatus.PRECONDITION_REQUIRED); if (!esperada.equals(a.getVersion())) throw rechazar(c, actor, "STATE_CHANGED", id, HttpStatus.PRECONDITION_FAILED); }
    private String perfil(Integer rolId, AssignmentAuthContext c, Long id) { return roles.findById(rolId).map(RolEntity::getNombre).orElseThrow(() -> rechazar(c, null, "ASSIGNMENT_NOT_FOUND", id, HttpStatus.NOT_FOUND)); }
    private void evento(UsuarioRolUnidadEntity a, TipoEventoAsignacion tipo, AutorizacionEfectivaService.AsignacionEfectiva actor, String motivo) { var e = new UsuarioRolUnidadEventoEntity(); e.setAsignacionId(a.getId()); e.setTipoEvento(tipo); e.setUsuarioActorId(actor.usuarioId()); e.setUnidadActorId(actor.unidadId()); e.setAsignacionEfectivaId(actor.id()); e.setMotivo(motivo); eventos.append(e); }
    private AssignmentDetail detalle(UsuarioRolUnidadEntity a, String perfil) { return new AssignmentDetail(a.getId(), a.getUsuarioId(), a.getCombinacionMatrizId(), perfil, a.getUnidadId(), a.getFechaInicio(), a.getFechaFin(), a.getDocumentoFormalId(), a.getRevocadaEn(), a.getVersion()); }
    private ResponseStatusException rechazar(AssignmentAuthContext c, AutorizacionEfectivaService.AsignacionEfectiva a, String codigo, Long id, HttpStatus status) { auditoria.registrarDenegacion(new AuditService.AuditCommand(c == null ? null : c.correlacionId(), a == null ? null : a.usuarioId(), c == null ? null : c.actorSub(), a == null ? null : a.id(), a == null ? null : a.perfil(), a == null ? null : a.unidadId(), "GESTIONAR_ASIGNACION", "SEGURIDAD", "ASIGNACION_FUNCIONAL", id, codigo, Map.of(), "INTERNO")); return new ResponseStatusException(status, codigo); }
    private void exito(AssignmentAuthContext c, AutorizacionEfectivaService.AsignacionEfectiva a, String op, Long id, Map<String, String> cambios) { auditoria.registrarExito(new AuditService.AuditCommand(c.correlacionId(), a.usuarioId(), null, a.id(), a.perfil(), a.unidadId(), op, "SEGURIDAD", "ASIGNACION_FUNCIONAL", id, "SUCCESS", cambios, "INTERNO")); }
}
