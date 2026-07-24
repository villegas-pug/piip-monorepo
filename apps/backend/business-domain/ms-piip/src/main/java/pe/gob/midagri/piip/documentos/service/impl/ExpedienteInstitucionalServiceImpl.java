package pe.gob.midagri.piip.documentos.service.impl;

import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.midagri.piip.auditoria.service.AuditService;
import pe.gob.midagri.piip.documentos.dto.*;
import pe.gob.midagri.piip.documentos.entity.ExpedienteInstitucionalEntity;
import pe.gob.midagri.piip.documentos.repository.ExpedienteInstitucionalRepository;
import pe.gob.midagri.piip.documentos.service.ExpedienteInstitucionalService;

@Service
public class ExpedienteInstitucionalServiceImpl implements ExpedienteInstitucionalService {
    private final ExpedienteInstitucionalRepository repository;
    private final AuditService auditService;
    public ExpedienteInstitucionalServiceImpl(ExpedienteInstitucionalRepository repository, AuditService auditService) {
        this.repository = repository; this.auditService = auditService;
    }
    @Override @Transactional
    public ExpedienteInstitucionalDetail crear(DocumentoAuthorizedContext contexto, CreateInstitutionalFileCommand comando) {
        validarContexto(contexto);
        if (comando == null || comando.asunto() == null || comando.asunto().isBlank()
                || comando.referenciaCasoUso() == null || comando.referenciaCasoUso().isBlank()) {
            throw new IllegalArgumentException("El asunto y la referencia del caso de uso son obligatorios.");
        }
        ExpedienteInstitucionalEntity expediente = new ExpedienteInstitucionalEntity();
        expediente.setCodigo("EI-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        expediente.setAsunto(comando.asunto());
        expediente.setModuloOrigen("DOCUMENTOS");
        expediente.setReferenciaCasoUso(comando.referenciaCasoUso());
        expediente.setUnidadId(contexto.unidadRecursoId());
        expediente.setClasificacion(comando.clasificacion()); expediente.setActivo("S");
        expediente.setCreadoPor(contexto.actorSub());
        repository.save(expediente);
        auditService.registrarExito(auditoria(contexto, "CREAR_EXPEDIENTE", "EXPEDIENTE_INSTITUCIONAL", expediente.getId(), comando.clasificacion().name()));
        return new ExpedienteInstitucionalDetail(expediente.getId(), expediente.getCodigo(), expediente.getAsunto(),
                expediente.getModuloOrigen(), expediente.getReferenciaCasoUso(), expediente.getUnidadId(),
                expediente.getClasificacion(), expediente.getVersion());
    }
    private void validarContexto(DocumentoAuthorizedContext c) { if (c == null || c.actorUsuarioId() == null || c.asignacionEfectiva() == null) throw new IllegalArgumentException("Contexto documental autorizado obligatorio."); }
    private AuditService.AuditCommand auditoria(DocumentoAuthorizedContext c, String op, String tipo, Long id, String clasificacion) {
        return new AuditService.AuditCommand(c.correlacionId(), c.actorUsuarioId(), null, c.asignacionEfectiva().id(), c.asignacionEfectiva().perfil(), c.unidadRecursoId(), op, "DOCUMENTOS", tipo, id, "SUCCESS", Map.of(), clasificacion);
    }
}
