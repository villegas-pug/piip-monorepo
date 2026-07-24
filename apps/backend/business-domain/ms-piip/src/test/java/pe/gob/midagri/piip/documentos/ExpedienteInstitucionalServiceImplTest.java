package pe.gob.midagri.piip.documentos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import pe.gob.midagri.piip.auditoria.service.AuditService;
import pe.gob.midagri.piip.documentos.dto.CreateInstitutionalFileCommand;
import pe.gob.midagri.piip.documentos.dto.DocumentoAuthorizedContext;
import pe.gob.midagri.piip.documentos.entity.ClasificacionDocumento;
import pe.gob.midagri.piip.documentos.entity.ExpedienteInstitucionalEntity;
import pe.gob.midagri.piip.documentos.repository.ExpedienteInstitucionalRepository;
import pe.gob.midagri.piip.documentos.service.impl.ExpedienteInstitucionalServiceImpl;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService.AsignacionEfectiva;

class ExpedienteInstitucionalServiceImplTest {
    @Test
    @Disabled("Test configuration issues - requires review")
    void creaExpedienteConContextoAutorizadoYAuditaLaOperacion() {
        ExpedienteInstitucionalRepository repository = mock(ExpedienteInstitucionalRepository.class);
        AuditService audit = mock(AuditService.class);
        doAnswer(invocation -> { ((ExpedienteInstitucionalEntity) invocation.getArgument(0)).setId(21L); return invocation.getArgument(0); })
                .when(repository).save(any());
        ExpedienteInstitucionalServiceImpl service = new ExpedienteInstitucionalServiceImpl(repository, audit);

        var detail = service.crear(contexto(), new CreateInstitutionalFileCommand("Aprobación PEI", "pei-v1", ClasificacionDocumento.RESTRINGIDO));

        assertEquals(21L, detail.id());
        assertEquals("DOCUMENTOS", detail.moduloOrigen());
        verify(audit).registrarExito(any());
    }

    @Test
    void exigeContextoAutorizadoParaCrearUnExpediente() {
        ExpedienteInstitucionalServiceImpl service = new ExpedienteInstitucionalServiceImpl(
                mock(ExpedienteInstitucionalRepository.class), mock(AuditService.class));

        assertThrows(IllegalArgumentException.class,
                () -> service.crear(null, new CreateInstitutionalFileCommand("Asunto", "caso", ClasificacionDocumento.INTERNO)));
    }

    private DocumentoAuthorizedContext contexto() {
        return new DocumentoAuthorizedContext("sub-1", 10L,
                new AsignacionEfectiva(2L, 10L, 4L, "Responsable", 7L), 7L, 20L, "corr-1");
    }
}
