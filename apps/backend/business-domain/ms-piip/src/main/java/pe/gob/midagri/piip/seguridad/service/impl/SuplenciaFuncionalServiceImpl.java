package pe.gob.midagri.piip.seguridad.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.gob.midagri.piip.auditoria.service.AuditService;
import pe.gob.midagri.piip.documentos.service.DocumentoService;
import pe.gob.midagri.piip.seguridad.dto.AssignmentAuthContext;
import pe.gob.midagri.piip.seguridad.dto.EarlyTerminationRequest;
import pe.gob.midagri.piip.seguridad.dto.SubstitutionDetail;
import pe.gob.midagri.piip.seguridad.dto.SubstitutionRequest;
import pe.gob.midagri.piip.seguridad.entity.RolEntity;
import pe.gob.midagri.piip.seguridad.entity.SuplenciaFuncionalEntity;
import pe.gob.midagri.piip.seguridad.entity.TipoEventoAsignacion;
import pe.gob.midagri.piip.seguridad.entity.UsuarioRolUnidadEntity;
import pe.gob.midagri.piip.seguridad.entity.UsuarioRolUnidadEventoEntity;
import pe.gob.midagri.piip.seguridad.repository.RolRepository;
import pe.gob.midagri.piip.seguridad.repository.SuplenciaFuncionalRepository;
import pe.gob.midagri.piip.seguridad.repository.UsuarioRepository;
import pe.gob.midagri.piip.seguridad.repository.UsuarioRolUnidadEventoRepository;
import pe.gob.midagri.piip.seguridad.repository.UsuarioRolUnidadRepository;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService;
import pe.gob.midagri.piip.seguridad.service.SuplenciaFuncionalService;

/** Autoridad JPA única para suplencias: serializa por asignación titular y audita cada resultado. */
@Service
public class SuplenciaFuncionalServiceImpl implements SuplenciaFuncionalService {
    private static final String GLOBAL_ADMIN = "GlobalAdmin";
    private static final String UNIDAD_ADMIN = "UnidadAdmin";
    private static final String EVALUADOR = "Evaluador";
    private static final String AUTORIDAD = "Autoridad";

    private final UsuarioRolUnidadRepository asignaciones;
    private final SuplenciaFuncionalRepository suplencias;
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final DocumentoService documentos;
    private final AutorizacionEfectivaService autorizacion;
    private final UsuarioRolUnidadEventoRepository eventos;
    private final AuditService auditoria;

    public SuplenciaFuncionalServiceImpl(UsuarioRolUnidadRepository asignaciones,
            SuplenciaFuncionalRepository suplencias, UsuarioRepository usuarios, RolRepository roles,
            DocumentoService documentos, AutorizacionEfectivaService autorizacion,
            UsuarioRolUnidadEventoRepository eventos, AuditService auditoria) {
        this.asignaciones = asignaciones; this.suplencias = suplencias; this.usuarios = usuarios;
        this.roles = roles; this.documentos = documentos; this.autorizacion = autorizacion;
        this.eventos = eventos; this.auditoria = auditoria;
    }

    @Override
    @Transactional
    public SubstitutionDetail crear(Long titularId, SubstitutionRequest request, AssignmentAuthContext contexto) {
        validarPeriodo(request, contexto, titularId);
        UsuarioRolUnidadEntity titular = asignaciones.findByIdForUpdate(titularId)
                .orElseThrow(() -> rechazar(contexto, null, "ASSIGNMENT_NOT_FOUND", titularId, HttpStatus.NOT_FOUND));
        String perfil = perfil(titular, contexto, titularId);
        AutorizacionEfectivaService.AsignacionEfectiva actor = autorizar(contexto, perfil, titular.getUnidadId());
        validarTitularVigente(titular, request, contexto, actor);
        if (usuarios.findById(request.suplenteUsuarioId()).filter(u -> "S".equals(u.getActivo())).isEmpty()) {
            throw rechazar(contexto, actor, "ASSIGNMENT_USER_NOT_ACTIVE", titularId, HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (!documentos.validarDocumentoInstitucionalAprobatorio(request.documentoFormalVersionId()).valido()) {
            throw rechazar(contexto, actor, "FORMAL_DOCUMENT_REQUIRED", titularId, HttpStatus.UNPROCESSABLE_ENTITY);
        }
        // El bloqueo del titular serializa dos solicitudes concurrentes incluso cuando aún no existe
        // una fila solapada; la consulta bloqueada mantiene el invariante de ausencia de solapes.
        if (!suplencias.findSolapadasForUpdate(titularId, request.inicio(), request.fin()).isEmpty()) {
            throw rechazar(contexto, actor, "SUBSTITUTION_OVERLAP", titularId, HttpStatus.CONFLICT);
        }

        UsuarioRolUnidadEntity suplente = new UsuarioRolUnidadEntity();
        suplente.setUsuarioId(request.suplenteUsuarioId()); suplente.setRolId(titular.getRolId());
        suplente.setUnidadId(titular.getUnidadId()); suplente.setCombinacionMatrizId(titular.getCombinacionMatrizId());
        suplente.setDocumentoFormalId(request.documentoFormalVersionId()); suplente.setFechaAsignacion(LocalDate.now());
        suplente.setFechaInicio(request.inicio()); suplente.setFechaFin(request.fin()); suplente.setActivo("S");
        suplente.setInactivaTemporalmente("N"); suplente.setAsignadoPor(contexto.actorSub());
        suplente = asignaciones.save(suplente);

        SuplenciaFuncionalEntity suplencia = new SuplenciaFuncionalEntity();
        suplencia.setAsignacionTitularId(titular.getId()); suplencia.setAsignacionSuplenteId(suplente.getId());
        suplencia.setInicio(request.inicio()); suplencia.setFin(request.fin()); suplencia.setAutoridadId(actor.usuarioId());
        suplencia.setDocumentoFormalId(request.documentoFormalVersionId()); suplencia.setCreadoPor(contexto.actorSub());
        suplencia = suplencias.save(suplencia);
        titular.setInactivaTemporalmente("S");
        asignaciones.save(titular);
        evento(titular.getId(), TipoEventoAsignacion.SUPLENCIA, actor, "SUPLENCIA_CREADA");
        evento(suplente.getId(), TipoEventoAsignacion.SUPLENCIA, actor, "SUPLENCIA_CREADA");
        exito(contexto, actor, "CREAR_SUPLENCIA", suplencia.getId(), Map.of("titularId", titularId.toString(),
                "suplenteUsuarioId", request.suplenteUsuarioId().toString()));
        return detalle(suplencia, suplente);
    }

    @Override
    @Transactional
    public SubstitutionDetail terminarAnticipadamente(Long suplenciaId, EarlyTerminationRequest request,
            AssignmentAuthContext contexto) {
        SuplenciaFuncionalEntity suplencia = suplencias.findByIdForUpdate(suplenciaId)
                .orElseThrow(() -> rechazar(contexto, null, "SUBSTITUTION_NOT_FOUND", suplenciaId, HttpStatus.NOT_FOUND));
        UsuarioRolUnidadEntity titular = asignaciones.findByIdForUpdate(suplencia.getAsignacionTitularId())
                .orElseThrow(() -> rechazar(contexto, null, "ASSIGNMENT_NOT_FOUND", suplencia.getAsignacionTitularId(), HttpStatus.NOT_FOUND));
        UsuarioRolUnidadEntity suplente = asignaciones.findByIdForUpdate(suplencia.getAsignacionSuplenteId())
                .orElseThrow(() -> rechazar(contexto, null, "ASSIGNMENT_NOT_FOUND", suplencia.getAsignacionSuplenteId(), HttpStatus.NOT_FOUND));
        String perfil = perfil(titular, contexto, titular.getId());
        AutorizacionEfectivaService.AsignacionEfectiva actor = autorizar(contexto, perfil, titular.getUnidadId());
        if (!actor.usuarioId().equals(suplencia.getAutoridadId())) {
            throw rechazar(contexto, actor, "SUBSTITUTION_TERMINATION_DENIED", suplenciaId, HttpStatus.FORBIDDEN);
        }
        if (suplencia.getTerminadaEn() != null) {
            throw rechazar(contexto, actor, "SUBSTITUTION_ALREADY_TERMINATED", suplenciaId, HttpStatus.CONFLICT);
        }
        if (request.documentoFormalVersionId() != null
                && !documentos.validarDocumentoInstitucionalAprobatorio(request.documentoFormalVersionId()).valido()) {
            throw rechazar(contexto, actor, "FORMAL_DOCUMENT_REQUIRED", suplenciaId, HttpStatus.UNPROCESSABLE_ENTITY);
        }
        suplencia.setTerminadaEn(LocalDateTime.now());
        suplencias.save(suplencia);
        suplente.setActivo("N");
        asignaciones.save(suplente);
        // El estado efectivo consulta la suplencia vigente. Esta marca deja evidencia y se limpia al
        // terminar; nunca reactiva una asignación revocada o vencida.
        titular.setInactivaTemporalmente("N");
        asignaciones.save(titular);
        evento(titular.getId(), TipoEventoAsignacion.ACTIVACION_TEMPORAL, actor, request.motivo().trim());
        evento(suplente.getId(), TipoEventoAsignacion.ACTIVACION_TEMPORAL, actor, request.motivo().trim());
        exito(contexto, actor, "TERMINAR_SUPLENCIA", suplenciaId, Map.of("motivo", request.motivo().trim()));
        return detalle(suplencia, suplente);
    }

    private void validarPeriodo(SubstitutionRequest request, AssignmentAuthContext contexto, Long recurso) {
        if (request.inicio() == null || request.fin() == null || request.fin().isBefore(request.inicio())) {
            throw rechazar(contexto, null, "INVALID_VALIDITY_PERIOD", recurso, HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private void validarTitularVigente(UsuarioRolUnidadEntity titular, SubstitutionRequest request,
            AssignmentAuthContext contexto, AutorizacionEfectivaService.AsignacionEfectiva actor) {
        if (!"S".equals(titular.getActivo()) || titular.getRevocadaEn() != null
                || request.inicio().isBefore(titular.getFechaInicio())
                || (titular.getFechaFin() != null && request.fin().isAfter(titular.getFechaFin()))) {
            throw rechazar(contexto, actor, "INVALID_VALIDITY_PERIOD", titular.getId(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private String perfil(UsuarioRolUnidadEntity asignacion, AssignmentAuthContext contexto, Long recurso) {
        return roles.findById(asignacion.getRolId()).map(RolEntity::getNombre)
                .orElseThrow(() -> rechazar(contexto, null, "ASSIGNMENT_NOT_FOUND", recurso, HttpStatus.NOT_FOUND));
    }

    private AutorizacionEfectivaService.AsignacionEfectiva autorizar(AssignmentAuthContext c, String perfil,
            Long unidad) {
        try {
            if (UNIDAD_ADMIN.equals(perfil) || EVALUADOR.equals(perfil) || AUTORIDAD.equals(perfil)
                    || GLOBAL_ADMIN.equals(perfil)) {
                return autorizacion.revalidarParaOperacionSensible(c.actorSub(), c.asignacionEfectivaId(),
                        GLOBAL_ADMIN, c.unidadEfectivaId());
            }
            try {
                return autorizacion.revalidarParaOperacionSensible(c.actorSub(), c.asignacionEfectivaId(),
                        GLOBAL_ADMIN, c.unidadEfectivaId());
            } catch (ResponseStatusException ignored) {
                return autorizacion.revalidarParaOperacionSensible(c.actorSub(), c.asignacionEfectivaId(),
                        UNIDAD_ADMIN, unidad);
            }
        } catch (ResponseStatusException ex) {
            throw rechazar(c, null, "ASSIGNMENT_ADMIN_DENIED", null, HttpStatus.FORBIDDEN);
        }
    }

    private void evento(Long asignacionId, TipoEventoAsignacion tipo,
            AutorizacionEfectivaService.AsignacionEfectiva actor, String motivo) {
        UsuarioRolUnidadEventoEntity evento = new UsuarioRolUnidadEventoEntity();
        evento.setAsignacionId(asignacionId); evento.setTipoEvento(tipo); evento.setUsuarioActorId(actor.usuarioId());
        evento.setUnidadActorId(actor.unidadId()); evento.setAsignacionEfectivaId(actor.id()); evento.setMotivo(motivo);
        eventos.append(evento);
    }

    private SubstitutionDetail detalle(SuplenciaFuncionalEntity suplencia, UsuarioRolUnidadEntity suplente) {
        return new SubstitutionDetail(suplencia.getId(), suplencia.getAsignacionTitularId(),
                suplencia.getAsignacionSuplenteId(), suplente.getUsuarioId(), suplencia.getInicio(), suplencia.getFin(),
                suplencia.getAutoridadId(), suplencia.getDocumentoFormalId(), suplencia.getTerminadaEn());
    }

    private ResponseStatusException rechazar(AssignmentAuthContext c, AutorizacionEfectivaService.AsignacionEfectiva a,
            String codigo, Long recurso, HttpStatus estado) {
        auditoria.registrarDenegacion(new AuditService.AuditCommand(c == null ? null : c.correlacionId(),
                a == null ? null : a.usuarioId(), c == null ? null : c.actorSub(), a == null ? null : a.id(),
                a == null ? null : a.perfil(), a == null ? null : a.unidadId(), "GESTIONAR_SUPLENCIA", "SEGURIDAD",
                "SUPLENCIA_FUNCIONAL", recurso, codigo, Map.of(), "INTERNO"));
        return new ResponseStatusException(estado, codigo);
    }

    private void exito(AssignmentAuthContext c, AutorizacionEfectivaService.AsignacionEfectiva a, String operacion,
            Long recurso, Map<String, String> cambios) {
        auditoria.registrarExito(new AuditService.AuditCommand(c.correlacionId(), a.usuarioId(), null, a.id(),
                a.perfil(), a.unidadId(), operacion, "SEGURIDAD", "SUPLENCIA_FUNCIONAL", recurso, "SUCCESS",
                cambios, "INTERNO"));
    }
}
