package pe.gob.midagri.piip.auditoria;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.web.server.ResponseStatusException;
import pe.gob.midagri.piip.auditoria.entity.EstadoSolicitudIdempotente;
import pe.gob.midagri.piip.auditoria.entity.SolicitudIdempotenteEntity;
import pe.gob.midagri.piip.auditoria.repository.SolicitudIdempotenteRepository;
import pe.gob.midagri.piip.auditoria.service.IdempotencyService;
import pe.gob.midagri.piip.auditoria.service.impl.IdempotencyServiceImpl;

class IdempotencyServiceImplTest {
    private final SolicitudIdempotenteRepository repository = mock(SolicitudIdempotenteRepository.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private final IdempotencyServiceImpl service = new IdempotencyServiceImpl(repository, new ObjectMapper(), transactions);

    IdempotencyServiceImplTest() {
        when(transactions.getTransaction(any(TransactionDefinition.class))).thenReturn(new SimpleTransactionStatus());
    }

    @Test
    void reutilizaResultadoParaMismaClaveYMismoPayloadCanonico() {
        SolicitudIdempotenteEntity existente = completada("{\"id\":7}");
        when(repository.findByConsumidorOperacionClaveForUpdate("DOCUMENTOS", "CARGAR", "clave-1"))
                .thenReturn(Optional.of(existente));

        IdempotencyService.IdempotencyResult result = service.execute(request("{\"b\":2,\"a\":1}"),
                () -> new IdempotencyService.IdempotencyResponse("DOCUMENTO", 8L, "{}"));

        assertTrue(result.reutilizado());
        verify(repository, never()).createAndFlush(any());
    }

    @Test
    void rechazaClaveReutilizadaConPayloadDistinto() {
        SolicitudIdempotenteEntity existente = completada("{}");
        existente.setHashPayload("0".repeat(64));
        when(repository.findByConsumidorOperacionClaveForUpdate(any(), any(), any())).thenReturn(Optional.of(existente));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.execute(request("{\"a\":1}"), () -> null));

        org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    private IdempotencyService.IdempotencyRequest request(String payload) {
        return new IdempotencyService.IdempotencyRequest("DOCUMENTOS", "CARGAR", "clave-1", payload, "sub-1");
    }

    private SolicitudIdempotenteEntity completada(String respuesta) {
        SolicitudIdempotenteEntity entity = new SolicitudIdempotenteEntity();
        entity.setHashPayload("43258cff783fe7036d8a43033f830adfc60ec037382473548ac742b888292777");
        entity.setEstadoTecnico(EstadoSolicitudIdempotente.COMPLETADA);
        entity.setRecursoTipo("DOCUMENTO"); entity.setRecursoId(7L); entity.setRespuestaJson(respuesta);
        return entity;
    }
}
