package pe.gob.midagri.piip.portafolio.seguimiento.service.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import pe.gob.midagri.piip.auditoria.service.AuditService;
import pe.gob.midagri.piip.auditoria.service.IdempotencyService;
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;
import pe.gob.midagri.piip.portafolio.entity.RegistroPortafolioEntity;
import pe.gob.midagri.piip.portafolio.entity.TipoRegistro;
import pe.gob.midagri.piip.portafolio.entity.TitularidadResponsableEntity;
import pe.gob.midagri.piip.portafolio.repository.RegistroPortafolioRepository;
import pe.gob.midagri.piip.portafolio.repository.TitularidadResponsableRepository;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.AltaPersonaRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.AltaUnidadRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.BajaParticipanteRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.ParticipanteResponse;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.ResponsibleReplacementRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.entity.ParticipantePersonaEntity;
import pe.gob.midagri.piip.portafolio.seguimiento.entity.ProyectoParticipantePersonaEntity;
import pe.gob.midagri.piip.portafolio.seguimiento.entity.ProyectoParticipanteUnidadEntity;
import pe.gob.midagri.piip.portafolio.seguimiento.repository.ParticipantePersonaRepository;
import pe.gob.midagri.piip.portafolio.seguimiento.repository.ProyectoParticipantePersonaRepository;
import pe.gob.midagri.piip.portafolio.seguimiento.repository.ProyectoParticipanteUnidadRepository;
import pe.gob.midagri.piip.portafolio.seguimiento.service.ParticipanteProyectoService;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService;

/** Participantes con vigencia y titularidad única del proyecto. */
@Service
public class ParticipanteProyectoServiceImpl implements ParticipanteProyectoService {
    private static final String PERFIL_RESPONSABLE = "Responsable";
    private final RegistroPortafolioRepository registroRepository;
    private final ParticipantePersonaRepository personaRepository;
    private final ProyectoParticipantePersonaRepository personaProyectoRepository;
    private final ProyectoParticipanteUnidadRepository unidadProyectoRepository;
    private final TitularidadResponsableRepository titularidadRepository;
    private final AutorizacionEfectivaService autorizacionService;
    private final AuditService auditService;
    private final IdempotencyService idempotencyService;

    public ParticipanteProyectoServiceImpl(RegistroPortafolioRepository registroRepository,
            ParticipantePersonaRepository personaRepository,
            ProyectoParticipantePersonaRepository personaProyectoRepository,
            ProyectoParticipanteUnidadRepository unidadProyectoRepository,
            TitularidadResponsableRepository titularidadRepository,
            AutorizacionEfectivaService autorizacionService, AuditService auditService,
            IdempotencyService idempotencyService) {
        this.registroRepository = registroRepository;
        this.personaRepository = personaRepository;
        this.personaProyectoRepository = personaProyectoRepository;
        this.unidadProyectoRepository = unidadProyectoRepository;
        this.titularidadRepository = titularidadRepository;
        this.autorizacionService = autorizacionService;
        this.auditService = auditService;
        this.idempotencyService = idempotencyService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipanteResponse> listarHistorico(long proyectoId, PortafolioAuthContext ctx,
            String idempotencyKey, String payloadJson) {
        RegistroPortafolioEntity proyecto = cargarProyecto(proyectoId);
        autorizarResponsable(ctx, proyecto);
        List<ParticipanteResponse> resultado = new ArrayList<>();
        for (ProyectoParticipantePersonaEntity relacion : personaProyectoRepository
                .findByIdProyectoOrderByInicioAsc(proyectoId)) {
            personaRepository.findById(relacion.getIdParticipante()).ifPresent(persona ->
                    resultado.add(respuestaPersona(relacion, persona, esTitular(proyectoId, persona))));
        }
        for (ProyectoParticipanteUnidadEntity relacion : unidadProyectoRepository
                .findByIdProyectoOrderByInicioAsc(proyectoId)) {
            resultado.add(new ParticipanteResponse(relacion.getId(), proyectoId, null,
                    relacion.getIdUnidad(), "Participante", null, null, null,
                    relacion.getFin() == null ? "VIGENTE" : "BAJA", relacion.getInicio(),
                    relacion.getFin(), etag(relacion.getId(), relacion.getFin())));
        }
        return resultado;
    }

    @Override
    public ParticipanteResponse altaPersona(long proyectoId, AltaPersonaRequest request,
            PortafolioAuthContext ctx, String idempotencyKey, String payloadJson) {
        return ejecutarIdempotente("ALTA_PARTICIPANTE_PERSONA", proyectoId, idempotencyKey,
                payloadJson, () -> altaPersonaInterna(proyectoId, request, ctx));
    }

    @Transactional
    ParticipanteResponse altaPersonaInterna(long proyectoId, AltaPersonaRequest request,
            PortafolioAuthContext ctx) {
        RegistroPortafolioEntity proyecto = cargarProyectoEjecucion(proyectoId);
        autorizarResponsable(ctx, proyecto);
        validarPersona(request);
        ParticipantePersonaEntity persona = resolverPersona(request);
        if (personaProyectoRepository.findByIdProyectoAndIdParticipante(proyectoId, persona.getId()).isPresent()) {
            throw conflicto(ctx, proyectoId, "PARTICIPANT_ALREADY_EXISTS", "La persona ya participa en el proyecto.");
        }
        if (PERFIL_RESPONSABLE.equals(request.rol())) {
            if (persona.getUsuarioId() == null || titularidadRepository
                    .findByRegistroPortafolioIdAndFinIsNull(proyectoId).isPresent()) {
                throw conflicto(ctx, proyectoId, "RESPONSIBLE_CARDINALITY",
                        "Solo puede existir un Responsable titular vigente y debe tener cuenta PIIP.");
            }
        }
        ProyectoParticipantePersonaEntity relacion = new ProyectoParticipantePersonaEntity();
        relacion.setIdProyecto(proyectoId); relacion.setIdParticipante(persona.getId());
        relacion.setInicio(LocalDate.now()); relacion.setIdActor(ctx.actorUsuarioId());
        relacion.setCreadoPor(ctx.actorSub());
        relacion = personaProyectoRepository.save(relacion);
        if (PERFIL_RESPONSABLE.equals(request.rol())) {
            TitularidadResponsableEntity titular = new TitularidadResponsableEntity();
            titular.setRegistroPortafolioId(proyectoId); titular.setUsuarioId(persona.getUsuarioId());
            titular.setInicio(relacion.getInicio()); titular.setCreadoPor(ctx.actorSub());
            titularidadRepository.save(titular);
        }
        auditar(ctx, "ALTA_PARTICIPANTE_PERSONA", relacion.getId(), Map.of("proyectoId", String.valueOf(proyectoId)));
        return respuestaPersona(relacion, persona, PERFIL_RESPONSABLE.equals(request.rol()));
    }

    @Override
    public ParticipanteResponse altaUnidad(long proyectoId, AltaUnidadRequest request,
            PortafolioAuthContext ctx, String idempotencyKey, String payloadJson) {
        return ejecutarIdempotente("ALTA_PARTICIPANTE_UNIDAD", proyectoId, idempotencyKey,
                payloadJson, () -> altaUnidadInterna(proyectoId, request, ctx));
    }

    @Transactional
    ParticipanteResponse altaUnidadInterna(long proyectoId, AltaUnidadRequest request,
            PortafolioAuthContext ctx) {
        RegistroPortafolioEntity proyecto = cargarProyectoEjecucion(proyectoId);
        autorizarResponsable(ctx, proyecto);
        if (request == null || request.unidadId() == null) throw validacion("La unidad es obligatoria.");
        if (unidadProyectoRepository.findByIdProyectoAndIdUnidad(proyectoId, request.unidadId()).isPresent()) {
            throw conflicto(ctx, proyectoId, "PARTICIPANT_ALREADY_EXISTS", "La unidad ya participa en el proyecto.");
        }
        ProyectoParticipanteUnidadEntity relacion = new ProyectoParticipanteUnidadEntity();
        relacion.setIdProyecto(proyectoId); relacion.setIdUnidad(request.unidadId());
        relacion.setInicio(LocalDate.now()); relacion.setIdActor(ctx.actorUsuarioId()); relacion.setCreadoPor(ctx.actorSub());
        relacion = unidadProyectoRepository.save(relacion);
        auditar(ctx, "ALTA_PARTICIPANTE_UNIDAD", relacion.getId(), Map.of("proyectoId", String.valueOf(proyectoId)));
        return new ParticipanteResponse(relacion.getId(), proyectoId, null, relacion.getIdUnidad(), "Participante",
                null, null, null, "VIGENTE", relacion.getInicio(), null, etag(relacion.getId(), null));
    }

    @Override
    public void bajaParticipante(long proyectoId, long participacionId, BajaParticipanteRequest request,
            PortafolioAuthContext ctx, String idempotencyKey, String payloadJson) {
        ejecutarIdempotente("BAJA_PARTICIPANTE", proyectoId, idempotencyKey, payloadJson, () -> {
            bajaInterna(proyectoId, participacionId, request, ctx); return null;
        });
    }

    @Override
    @Transactional
    public ParticipanteResponse sustituirResponsable(long proyectoId, ResponsibleReplacementRequest request,
            PortafolioAuthContext ctx, String idempotencyKey, String payloadJson) {
        if (request == null || request.nuevoResponsableId() == null || vacio(request.motivo())) throw validacion("El nuevo Responsable y motivo son obligatorios.");
        RegistroPortafolioEntity proyecto = cargarProyectoEjecucion(proyectoId);
        autorizarUnidadAdmin(ctx, proyecto);
        TitularidadResponsableEntity anterior = titularidadRepository.findByRegistroPortafolioIdAndFinIsNull(proyectoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "RESPONSIBLE_CARDINALITY"));
        ParticipantePersonaEntity nueva = personaRepository.findByUsuarioId(request.nuevoResponsableId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "RESPONSIBLE_PERSON_REQUIRED"));
        if (anterior.getUsuarioId().equals(nueva.getUsuarioId())) throw conflicto(ctx, proyectoId, "RESPONSIBLE_CARDINALITY", "El Responsable nuevo debe ser distinto del vigente.");
        ProyectoParticipantePersonaEntity participacion = personaProyectoRepository.findByIdProyectoAndIdParticipante(proyectoId, nueva.getId()).orElseGet(() -> {
            ProyectoParticipantePersonaEntity creada = new ProyectoParticipantePersonaEntity(); creada.setIdProyecto(proyectoId); creada.setIdParticipante(nueva.getId()); creada.setInicio(LocalDate.now()); creada.setIdActor(ctx.actorUsuarioId()); creada.setCreadoPor(ctx.actorSub()); return personaProyectoRepository.save(creada);
        });
        anterior.setFin(LocalDate.now()); anterior.setMotivoSustitucion(request.motivo().trim()); anterior.setActorSustitucionId(ctx.actorUsuarioId());
        titularidadRepository.saveAndFlush(anterior);
        TitularidadResponsableEntity reemplazo = new TitularidadResponsableEntity(); reemplazo.setRegistroPortafolioId(proyectoId); reemplazo.setUsuarioId(nueva.getUsuarioId()); reemplazo.setInicio(LocalDate.now()); reemplazo.setMotivoSustitucion(request.motivo().trim()); reemplazo.setActorSustitucionId(ctx.actorUsuarioId()); reemplazo.setCreadoPor(ctx.actorSub()); titularidadRepository.save(reemplazo);
        auditar(ctx, "SUSTITUIR_RESPONSABLE", reemplazo.getId(), Map.of("proyectoId", String.valueOf(proyectoId), "anterior", String.valueOf(anterior.getUsuarioId()), "nuevo", String.valueOf(nueva.getUsuarioId())));
        return respuestaPersona(participacion, nueva, true);
    }

    @Transactional
    void bajaInterna(long proyectoId, long participacionId, BajaParticipanteRequest request, PortafolioAuthContext ctx) {
        RegistroPortafolioEntity proyecto = cargarProyectoEjecucion(proyectoId); autorizarResponsable(ctx, proyecto);
        if (request == null || request.fechaBaja() == null) throw validacion("La fecha de baja es obligatoria.");
        var persona = personaProyectoRepository.findById(participacionId);
        if (persona.isPresent()) {
            ProyectoParticipantePersonaEntity relacion = persona.get(); verificarProyecto(relacion.getIdProyecto(), proyectoId);
            ParticipantePersonaEntity participante = personaRepository.findById(relacion.getIdParticipante())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PARTICIPANT_NOT_FOUND"));
            if (esTitular(proyectoId, participante)) throw conflicto(ctx, proyectoId, "RESPONSIBLE_REPLACEMENT_REQUIRED",
                    "El Responsable titular se sustituye atómicamente; no se da de baja.");
            cerrar(relacion.getInicio(), relacion.getFin(), request.fechaBaja()); relacion.setFin(request.fechaBaja()); personaProyectoRepository.save(relacion);
        } else {
            ProyectoParticipanteUnidadEntity relacion = unidadProyectoRepository.findById(participacionId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PARTICIPATION_NOT_FOUND"));
            verificarProyecto(relacion.getIdProyecto(), proyectoId); cerrar(relacion.getInicio(), relacion.getFin(), request.fechaBaja());
            relacion.setFin(request.fechaBaja()); unidadProyectoRepository.save(relacion);
        }
        auditar(ctx, "BAJA_PARTICIPANTE", participacionId, Map.of("proyectoId", String.valueOf(proyectoId)));
    }

    private ParticipantePersonaEntity resolverPersona(AltaPersonaRequest request) {
        if (request.personaId() != null) return personaRepository.findByUsuarioId(request.personaId()).orElseGet(() -> crearPersona(request));
        return crearPersona(request);
    }
    private ParticipantePersonaEntity crearPersona(AltaPersonaRequest request) {
        ParticipantePersonaEntity persona = new ParticipantePersonaEntity(); persona.setUsuarioId(request.personaId());
        persona.setNombresCompletos(request.nombresCompletos().trim()); persona.setInstitucion(request.institucion().trim());
        persona.setFuncion(request.funcion().trim()); return personaRepository.save(persona);
    }
    private void validarPersona(AltaPersonaRequest request) {
        if (request == null || request.rol() == null || !(PERFIL_RESPONSABLE.equals(request.rol()) || "Participante".equals(request.rol()))
                || vacio(request.nombresCompletos()) || vacio(request.institucion()) || vacio(request.funcion())) throw validacion("Los datos mínimos de la persona son obligatorios.");
    }
    private boolean esTitular(long proyectoId, ParticipantePersonaEntity persona) {
        return persona.getUsuarioId() != null && titularidadRepository.findByRegistroPortafolioIdAndFinIsNull(proyectoId)
                .map(t -> persona.getUsuarioId().equals(t.getUsuarioId())).orElse(false);
    }
    private RegistroPortafolioEntity cargarProyecto(long id) { return registroRepository.findById(id).filter(r -> r.getTipoRegistro() == TipoRegistro.PROYECTO)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND")); }
    private RegistroPortafolioEntity cargarProyectoEjecucion(long id) { RegistroPortafolioEntity p = cargarProyecto(id); if (p.getEstado() != EstadoIniciativa.PROYECTO_EJECUCION) throw new ResponseStatusException(HttpStatus.CONFLICT, "PROJECT_NOT_IN_EXECUTION"); return p; }
    private void autorizarResponsable(PortafolioAuthContext ctx, RegistroPortafolioEntity p) {
        if (ctx == null || !PERFIL_RESPONSABLE.equals(ctx.perfilEfectivo()) || ctx.actorSub() == null || ctx.asignacionEfectivaId() == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ASSIGNMENT_SCOPE_DENIED");
        AutorizacionEfectivaService.AsignacionEfectiva asignacion = autorizacionService.revalidarParaOperacionSensible(ctx.actorSub(), ctx.asignacionEfectivaId(), PERFIL_RESPONSABLE, p.getUnidadEjecutoraId());
        if (ctx.actorUsuarioId() == null || !ctx.actorUsuarioId().equals(asignacion.usuarioId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ASSIGNMENT_SCOPE_DENIED");
    }
    private void autorizarUnidadAdmin(PortafolioAuthContext ctx, RegistroPortafolioEntity p) {
        if (ctx == null || !"UnidadAdmin".equals(ctx.perfilEfectivo()) || ctx.actorSub() == null || ctx.asignacionEfectivaId() == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ASSIGNMENT_SCOPE_DENIED");
        AutorizacionEfectivaService.AsignacionEfectiva asignacion = autorizacionService.revalidarParaOperacionSensible(ctx.actorSub(), ctx.asignacionEfectivaId(), "UnidadAdmin", p.getUnidadEjecutoraId());
        if (ctx.actorUsuarioId() == null || !ctx.actorUsuarioId().equals(asignacion.usuarioId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ASSIGNMENT_SCOPE_DENIED");
    }
    private void auditar(PortafolioAuthContext ctx, String op, Long id, Map<String,String> cambios) { auditService.registrarExito(new AuditService.AuditCommand(ctx.correlacionId(), ctx.actorUsuarioId(), null, ctx.asignacionEfectivaId(), ctx.perfilEfectivo(), ctx.unidadEfectivaId(), op, "PORTAFOLIO", "PARTICIPACION_PROYECTO", id, "SUCCESS", cambios, "RESTRINGIDO")); }
    private ResponseStatusException conflicto(PortafolioAuthContext c,long p,String code,String detail) { auditService.registrarDenegacion(new AuditService.AuditCommand(c.correlacionId(),c.actorUsuarioId(),null,c.asignacionEfectivaId(),c.perfilEfectivo(),c.unidadEfectivaId(),"PARTICIPACION", "PORTAFOLIO", "PARTICIPACION_PROYECTO",p,code,Map.of("detalle",detail),"RESTRINGIDO")); return new ResponseStatusException(HttpStatus.CONFLICT,code+": "+detail); }
    @SuppressWarnings("unchecked")
    private <T> T ejecutarIdempotente(String op,long proyectoId,String key,String payload, java.util.function.Supplier<T> action) { if (key == null || key.isBlank()) throw validacion("Idempotency-Key es obligatoria."); final Object[] resultado = new Object[1]; idempotencyService.execute(new IdempotencyService.IdempotencyRequest("PORTAFOLIO",op,key,payload,"unknown"), () -> { resultado[0]=action.get(); return new IdempotencyService.IdempotencyResponse("PROYECTO",proyectoId,"{}"); }); return (T) resultado[0]; }
    private static ParticipanteResponse respuestaPersona(ProyectoParticipantePersonaEntity r, ParticipantePersonaEntity p, boolean titular) { return new ParticipanteResponse(r.getId(),r.getIdProyecto(),p.getId(),null,titular?"Responsable":"Participante",p.getNombresCompletos(),p.getInstitucion(),p.getFuncion(),r.getFin()==null?"VIGENTE":"BAJA",r.getInicio(),r.getFin(),etag(r.getId(),r.getFin())); }
    private static String etag(Long id,LocalDate fin){return "\""+id+"-"+(fin==null?0:fin.toEpochDay())+"\"";} private static boolean vacio(String s){return s==null||s.isBlank();} private static ResponseStatusException validacion(String s){return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"VALIDATION_FAILED: "+s);} private static void verificarProyecto(Long actual,long esperado){if(actual == null || actual.longValue() != esperado)throw new ResponseStatusException(HttpStatus.CONFLICT,"PARTICIPATION_PROJECT_MISMATCH");} private static void cerrar(LocalDate inicio,LocalDate fin,LocalDate baja){if(fin!=null)throw new ResponseStatusException(HttpStatus.CONFLICT,"PARTICIPATION_ALREADY_CLOSED");if(baja.isBefore(inicio))throw validacion("La baja no puede preceder al alta.");}
}
