package pe.gob.midagri.piip.portafolio.service.impl;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import pe.gob.midagri.piip.auditoria.service.AuditService;
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.dto.ResponsibleReplacementDetail;
import pe.gob.midagri.piip.portafolio.dto.ResponsibleReplacementRequest;
import pe.gob.midagri.piip.portafolio.entity.RegistroPortafolioEntity;
import pe.gob.midagri.piip.portafolio.entity.TitularidadResponsableEntity;
import pe.gob.midagri.piip.portafolio.repository.RegistroPortafolioRepository;
import pe.gob.midagri.piip.portafolio.repository.TitularidadResponsableRepository;
import pe.gob.midagri.piip.portafolio.service.ResponsableTitularService;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService;

/**
 * Mantiene una única titularidad vigente bajo el bloqueo del agregado de portafolio.
 * La autorización efectiva pertenece exclusivamente al contrato de seguridad.
 */
@Service
public class ResponsableTitularServiceImpl implements ResponsableTitularService {

    private static final String UNIDAD_ADMIN = "UnidadAdmin";
    private final RegistroPortafolioRepository registroRepository;
    private final TitularidadResponsableRepository titularidadRepository;
    private final AutorizacionEfectivaService autorizacion;
    private final AuditService auditoria;

    public ResponsableTitularServiceImpl(
            RegistroPortafolioRepository registroRepository,
            TitularidadResponsableRepository titularidadRepository,
            AutorizacionEfectivaService autorizacion,
            AuditService auditoria) {
        this.registroRepository = registroRepository;
        this.titularidadRepository = titularidadRepository;
        this.autorizacion = autorizacion;
        this.auditoria = auditoria;
    }

    @Override
    @Transactional
    public ResponsibleReplacementDetail sustituir(
            Long registroId, ResponsibleReplacementRequest comando, PortafolioAuthContext contexto) {
        if (registroId == null || comando == null || comando.nuevoResponsableId() == null
                || comando.motivo() == null || comando.motivo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_FAILED");
        }

        RegistroPortafolioEntity registro = registroRepository.findByIdForUpdate(registroId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PORTFOLIO_RECORD_NOT_FOUND"));
        AutorizacionEfectivaService.AsignacionEfectiva actor = autorizar(contexto, registro);
        TitularidadResponsableEntity anterior = titularidadRepository
                .findByRegistroPortafolioIdAndFinIsNull(registroId)
                .orElseThrow(() -> conflicto(contexto, actor, registroId, "RESPONSIBLE_CARDINALITY"));

        if (anterior.getUsuarioId().equals(comando.nuevoResponsableId())) {
            throw conflicto(contexto, actor, registroId, "RESPONSIBLE_ALREADY_CURRENT");
        }

        LocalDate hoy = LocalDate.now();
        anterior.setFin(hoy);
        anterior.setMotivoSustitucion(comando.motivo().trim());
        anterior.setActorSustitucionId(actor.usuarioId());
        titularidadRepository.saveAndFlush(anterior);

        TitularidadResponsableEntity nueva = new TitularidadResponsableEntity();
        nueva.setRegistroPortafolioId(registroId);
        nueva.setUsuarioId(comando.nuevoResponsableId());
        nueva.setInicio(hoy);
        nueva.setMotivoSustitucion(comando.motivo().trim());
        nueva.setActorSustitucionId(actor.usuarioId());
        nueva.setCreadoPor(contexto.actorSub());
        nueva = titularidadRepository.saveAndFlush(nueva);

        auditoria.registrarExito(new AuditService.AuditCommand(
                contexto.correlacionId(), actor.usuarioId(), null, actor.id(), actor.perfil(), actor.unidadId(),
                "SUSTITUIR_RESPONSABLE_TITULAR", "PORTAFOLIO", "REGISTRO_PORTAFOLIO", registroId,
                "SUCCESS", Map.of("titularAnteriorId", anterior.getUsuarioId().toString(),
                        "nuevoResponsableId", nueva.getUsuarioId().toString()), "RESTRINGIDO"));
        return new ResponsibleReplacementDetail(anterior.getId(), anterior.getUsuarioId(), nueva.getId(),
                nueva.getUsuarioId(), nueva.getInicio());
    }

    private AutorizacionEfectivaService.AsignacionEfectiva autorizar(
            PortafolioAuthContext contexto, RegistroPortafolioEntity registro) {
        try {
            if (contexto == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ASSIGNMENT_SCOPE_DENIED");
            }
            return autorizacion.revalidarParaOperacionSensible(contexto.actorSub(),
                    contexto.asignacionEfectivaId(), UNIDAD_ADMIN, registro.getUnidadEjecutoraId());
        } catch (ResponseStatusException ex) {
            auditarDenegacion(contexto, registro.getId(), "ASSIGNMENT_SCOPE_DENIED");
            throw ex;
        }
    }

    private ResponseStatusException conflicto(PortafolioAuthContext contexto,
            AutorizacionEfectivaService.AsignacionEfectiva actor, Long registroId, String codigo) {
        auditoria.registrarDenegacion(new AuditService.AuditCommand(contexto.correlacionId(), actor.usuarioId(), null,
                actor.id(), actor.perfil(), actor.unidadId(), "SUSTITUIR_RESPONSABLE_TITULAR", "PORTAFOLIO",
                "REGISTRO_PORTAFOLIO", registroId, codigo, Map.of(), "RESTRINGIDO"));
        return new ResponseStatusException(HttpStatus.CONFLICT, codigo);
    }

    private void auditarDenegacion(PortafolioAuthContext contexto, Long registroId, String codigo) {
        auditoria.registrarDenegacion(new AuditService.AuditCommand(
                contexto == null ? null : contexto.correlacionId(), null,
                contexto == null ? null : contexto.actorSub(),
                contexto == null ? null : contexto.asignacionEfectivaId(), null, null,
                "SUSTITUIR_RESPONSABLE_TITULAR", "PORTAFOLIO", "REGISTRO_PORTAFOLIO", registroId,
                codigo, Map.of(), "RESTRINGIDO"));
    }
}
