package pe.gob.midagri.piip.portafolio.iniciativa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.gob.midagri.piip.portafolio.entity.SecuenciaCodigoEntity;
import pe.gob.midagri.piip.portafolio.exception.PortafolioValidationException;
import pe.gob.midagri.piip.portafolio.repository.SecuenciaCodigoRepository;
import pe.gob.midagri.piip.portafolio.service.CodigoProyectoService;
import pe.gob.midagri.piip.portafolio.service.impl.CodigoProyectoServiceImpl;

/**
 * Pruebas unitarias para {@link CodigoProyectoService} conforme al contrato
 * de presentación de iniciativas
 * ({@code specs/001-gestionar-portafolio-innovacion/contracts/portafolio.md})
 * y al criterio de correlativo único por año y unidad con
 * {@code PESSIMISTIC_WRITE}.
 *
 * <p>El formato inmutable es {@code AAAA-PREFIJO_UNIDAD-NNNNN} y el
 * correlativo nunca se reutiliza.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("US1 - Iniciativa: CodigoProyectoService (correlativo AAAA-PREFIJO_UNIDAD-NNNNN)")
class CodigoProyectoServiceTest {

    @Mock
    private SecuenciaCodigoRepository secuenciaCodigoRepository;

    private CodigoProyectoService codigoService;

    @BeforeEach
    void setUp() {
        codigoService = new CodigoProyectoServiceImpl(secuenciaCodigoRepository);
    }

    @Test
    @DisplayName("Genera código con prefijo de unidad y secuencia existente")
    void generarCodigo_conPrefijoYSecuenciaExistente_retornaCodigoFormateado() {
        SecuenciaCodigoEntity secuencia = new SecuenciaCodigoEntity();
        secuencia.setId(1L);
        secuencia.setAnio(2026);
        secuencia.setUnidadId(10L);
        secuencia.setUltimoNumero(0);
        when(secuenciaCodigoRepository.findForUpdate(2026, 10L)).thenReturn(Optional.of(secuencia));
        when(secuenciaCodigoRepository.save(any(SecuenciaCodigoEntity.class))).thenReturn(secuencia);

        String codigo = codigoService.generarCodigo(2026, 10L, "MIDAGRI");

        assertEquals("2026-MIDAGRI-00001", codigo);
        assertEquals(1, secuencia.getUltimoNumero());
        verify(secuenciaCodigoRepository, times(1)).save(any(SecuenciaCodigoEntity.class));
    }

    @Test
    @DisplayName("Crea nueva secuencia cuando no existe una para el año y la unidad")
    void generarCodigo_sinSecuenciaExistente_creaNuevaConNumeroInicial() {
        when(secuenciaCodigoRepository.findForUpdate(2026, 10L)).thenReturn(Optional.empty());
        when(secuenciaCodigoRepository.save(any(SecuenciaCodigoEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        String codigo = codigoService.generarCodigo(2026, 10L, "OGTI");

        assertEquals("2026-OGTI-00001", codigo);
        verify(secuenciaCodigoRepository, times(2)).save(any(SecuenciaCodigoEntity.class));
    }

    @Test
    @DisplayName("El correlativo es estrictamente incremental y nunca se reutiliza")
    void generarCodigo_incrementalNuncaReutiliza() {
        SecuenciaCodigoEntity secuencia = new SecuenciaCodigoEntity();
        secuencia.setId(1L);
        secuencia.setAnio(2026);
        secuencia.setUnidadId(10L);
        secuencia.setUltimoNumero(41);
        when(secuenciaCodigoRepository.findForUpdate(2026, 10L)).thenReturn(Optional.of(secuencia));
        when(secuenciaCodigoRepository.save(any(SecuenciaCodigoEntity.class))).thenReturn(secuencia);

        String siguiente = codigoService.generarCodigo(2026, 10L, "MIDAGRI");
        assertEquals("2026-MIDAGRI-00042", siguiente);
    }

    @Test
    @DisplayName("Rechaza prefijo nulo o en blanco con UNIT_PREFIX_NOT_AVAILABLE")
    void generarCodigo_sinPrefijo_rechaza() {
        assertThrows(PortafolioValidationException.class,
                () -> codigoService.generarCodigo(2026, 10L, null));
        assertThrows(PortafolioValidationException.class,
                () -> codigoService.generarCodigo(2026, 10L, "   "));
        verify(secuenciaCodigoRepository, never()).findForUpdate(anyInt(), anyLong());
        verify(secuenciaCodigoRepository, never()).save(any(SecuenciaCodigoEntity.class));
    }

    @Test
    @DisplayName("La generación es atómica bajo PESSIMISTIC_WRITE: dos presentaciones simultáneas producen códigos distintos")
    void generarCodigo_concurrente_generaCodigosDistintos() throws Exception {
        AtomicInteger contador = new AtomicInteger(0);
        when(secuenciaCodigoRepository.findForUpdate(2026, 10L)).thenAnswer(inv -> {
            SecuenciaCodigoEntity s = new SecuenciaCodigoEntity();
            s.setId(1L);
            s.setAnio(2026);
            s.setUnidadId(10L);
            s.setUltimoNumero(contador.getAndIncrement());
            return Optional.of(s);
        });
        when(secuenciaCodigoRepository.save(any(SecuenciaCodigoEntity.class))).thenAnswer(inv -> {
            return inv.getArgument(0);
        });

        int hilos = 8;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        List<Future<String>> futuros = new ArrayList<>();
        List<Callable<String>> tareas = new ArrayList<>();
        for (int i = 0; i < hilos; i++) {
            tareas.add(() -> codigoService.generarCodigo(2026, 10L, "MIDAGRI"));
        }
        for (Callable<String> t : tareas) {
            futuros.add(pool.submit(t));
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS),
                "Las tareas concurrentes deben finalizar en menos de 5 segundos");

        List<String> codigos = new ArrayList<>();
        for (Future<String> f : futuros) {
            codigos.add(f.get());
        }
        assertEquals(hilos, codigos.stream().distinct().count(),
                "Cada presentación concurrente debe generar un código distinto");
        // El repository debe ser invocado bajo bloqueo pesimista.
        verify(secuenciaCodigoRepository, atLeastOnce()).findForUpdate(2026, 10L);
    }

    @Test
    @DisplayName("El servicio expone contrato de interfaz y no entidades JPA")
    void servicioExponeContratoSinEntidadesJPA() {
        assertTrue(CodigoProyectoService.class.isInterface(),
                "El contrato del servicio debe ser una interfaz");
        for (var metodo : CodigoProyectoService.class.getDeclaredMethods()) {
            assertFalse(metodo.getReturnType().getName().contains("pe.gob.midagri.piip.portafolio.entity"),
                    () -> "El método " + metodo.getName() + " no debe retornar entidades JPA");
        }
    }

    @Test
    @DisplayName("El método generarCodigo requiere bloqueo pesimista en la consulta")
    void generarCodigo_usaFindForUpdatePesimista() {
        SecuenciaCodigoEntity secuencia = new SecuenciaCodigoEntity();
        secuencia.setId(1L);
        secuencia.setAnio(2026);
        secuencia.setUnidadId(10L);
        secuencia.setUltimoNumero(7);
        when(secuenciaCodigoRepository.findForUpdate(2026, 10L)).thenReturn(Optional.of(secuencia));
        when(secuenciaCodigoRepository.save(any(SecuenciaCodigoEntity.class))).thenReturn(secuencia);

        codigoService.generarCodigo(2026, 10L, "MIDAGRI");
        verify(secuenciaCodigoRepository).findForUpdate(2026, 10L);
    }

    @Test
    @DisplayName("El número formateado siempre tiene cinco dígitos")
    void generarCodigo_numeroConCincoDigitos() {
        SecuenciaCodigoEntity secuencia = new SecuenciaCodigoEntity();
        secuencia.setId(1L);
        secuencia.setAnio(2026);
        secuencia.setUnidadId(10L);
        secuencia.setUltimoNumero(0);
        when(secuenciaCodigoRepository.findForUpdate(2026, 10L)).thenReturn(Optional.of(secuencia));
        when(secuenciaCodigoRepository.save(any(SecuenciaCodigoEntity.class))).thenReturn(secuencia);

        String codigo = codigoService.generarCodigo(2026, 10L, "MIDAGRI");
        assertNotNull(codigo);
        // Formato AAAA-PREFIJO-NNNNN: el último segmento tiene cinco dígitos.
        String ultimoSegmento = codigo.substring(codigo.lastIndexOf('-') + 1);
        assertEquals(5, ultimoSegmento.length(),
                () -> "El último segmento del código debe tener 5 dígitos, pero fue " + ultimoSegmento);
    }
}
