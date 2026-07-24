package pe.gob.midagri.piip.auditoria;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.midagri.piip.auditoria.repository.AuditoriaAccesoRepository;
import pe.gob.midagri.piip.auditoria.repository.AuditoriaEventoRepository;
import pe.gob.midagri.piip.auditoria.service.AuditService;
import pe.gob.midagri.piip.auditoria.service.impl.AuditServiceImpl;

class AuditServiceImplTest {

    private final AuditoriaEventoRepository eventos = org.mockito.Mockito.mock(AuditoriaEventoRepository.class);
    private final AuditServiceImpl service = new AuditServiceImpl(
            eventos, org.mockito.Mockito.mock(AuditoriaAccesoRepository.class), new ObjectMapper());

    @Test
    void registraExitoComoEvidenciaAppendOnlyEnLaTransaccionDeNegocio() throws Exception {
        service.registrarExito(comando());

        verify(eventos).append(any());
        Method method = AuditServiceImpl.class.getMethod("registrarExito", AuditService.AuditCommand.class);
        org.junit.jupiter.api.Assertions.assertEquals(Propagation.REQUIRED, method.getAnnotation(Transactional.class).propagation());
    }

    @Test
    void registraDenegacionEnTransaccionIndependiente() throws Exception {
        service.registrarDenegacion(comando());

        verify(eventos).append(any());
        Method method = AuditServiceImpl.class.getMethod("registrarDenegacion", AuditService.AuditCommand.class);
        org.junit.jupiter.api.Assertions.assertEquals(Propagation.REQUIRES_NEW, method.getAnnotation(Transactional.class).propagation());
    }

    @Test
    void fallaCerradoCuandoNoPuedePersistirLaEvidencia() {
        when(eventos.append(any())).thenThrow(new RuntimeException("Oracle no disponible"));

        assertThrows(IllegalStateException.class, () -> service.registrarExito(comando()));
    }

    @Test
    void rechazaDatosSensiblesAntesDePersistirLaAuditoria() {
        AuditService.AuditCommand sensible = new AuditService.AuditCommand(
                "corr-1", 1L, null, 2L, "Responsable", 3L, "CARGAR", "DOCUMENTOS",
                "DOCUMENTO", 4L, "SUCCESS", Map.of("token", "secreto"), "INTERNO");

        assertThrows(IllegalArgumentException.class, () -> service.registrarExito(sensible));
        org.mockito.Mockito.verifyNoInteractions(eventos);
    }

    private AuditService.AuditCommand comando() {
        return new AuditService.AuditCommand("corr-1", 1L, null, 2L, "Responsable", 3L,
                "CARGAR", "DOCUMENTOS", "DOCUMENTO", 4L, "SUCCESS", Map.of("serieId", "5"), "INTERNO");
    }
}
