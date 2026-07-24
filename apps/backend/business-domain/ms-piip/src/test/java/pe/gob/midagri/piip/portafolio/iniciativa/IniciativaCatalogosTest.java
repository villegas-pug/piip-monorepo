package pe.gob.midagri.piip.portafolio.iniciativa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;
import pe.gob.midagri.piip.portafolio.entity.FuenteOrigen;
import pe.gob.midagri.piip.portafolio.entity.TipoRegistro;
import pe.gob.midagri.piip.portafolio.entity.TipoSolucion;

/**
 * Pruebas de catálogo canónico exigidas por la Constitución 5.0.0 sección
 * "Catálogos e invariantes canónicos del portafolio" y por el contrato
 * {@code specs/001-gestionar-portafolio-innovacion/contracts/portafolio.md}.
 *
 * <p>La presentación debe validar contra los valores canónicos: cualquier
 * desviación es una <em>NEEDS CLARIFICATION</em> o un rechazo.
 */
@DisplayName("US1 - Iniciativa: catálogos canónicos del portafolio")
class IniciativaCatalogosTest {

    @Test
    @DisplayName("Tipo de registro solo admite INICIATIVA y PROYECTO")
    void tipoRegistroCatalogo() {
        assertEquals(2, TipoRegistro.values().length);
        assertNotNull(TipoRegistro.valueOf("INICIATIVA"));
        assertNotNull(TipoRegistro.valueOf("PROYECTO"));
    }

    @Test
    @DisplayName("Tipo de solución solo admite POTENCIAL_ADAPTABLE y POR_DEFINIR")
    void tipoSolucionCatalogo() {
        assertEquals(2, TipoSolucion.values().length);
        assertNotNull(TipoSolucion.valueOf("POTENCIAL_ADAPTABLE"));
        assertNotNull(TipoSolucion.valueOf("POR_DEFINIR"));
    }

    @Test
    @DisplayName("Fuente u origen canónica del portafolio institucional")
    void fuenteOrigenCatalogo() {
        List<String> esperadas = Arrays.asList(
                "FICHA_INICIATIVA",
                "CONCURSO_INTERNO",
                "INNOVACION_ABIERTA",
                "PROPUESTA_JEFATURA",
                "OTROS");
        for (String f : esperadas) {
            assertNotNull(FuenteOrigen.valueOf(f),
                    () -> "El catálogo de FuenteOrigen debe incluir " + f);
        }
        assertEquals(esperadas.size(), FuenteOrigen.values().length,
                "FuenteOrigen no admite valores fuera del catálogo constitucional");
    }

    @Test
    @DisplayName("Estados canónicos del portafolio: once estados terminales y de flujo")
    void estadosPortafolioCanonicos() {
        List<String> esperados = Arrays.asList(
                "PRESENTADO",
                "NO_ADMISIBLE",
                "NO_APLICABLE",
                "INICIATIVA_APROBADA",
                "INICIATIVA_ARCHIVADA",
                "PROYECTO_EJECUCION",
                "SUSPENDIDO",
                "CANCELADO",
                "PRODUCTO_APROBADO",
                "PRODUCTO_NO_APROBADO",
                "FINALIZADO");
        for (String e : esperados) {
            assertNotNull(EstadoIniciativa.valueOf(e),
                    () -> "El portafolio debe reconocer el estado " + e);
        }
        assertEquals(esperados.size(), EstadoIniciativa.values().length,
                "Estados no pueden ser extendidos sin enmienda constitucional");
    }

    @Test
    @DisplayName("PRESENTADO es un estado canónico del portafolio")
    void presentadoEsEstadoCanónico() {
        assertTrue(Arrays.asList(EstadoIniciativa.values()).contains(EstadoIniciativa.PRESENTADO),
                "PRESENTADO debe existir en el catálogo canónico");
    }

    @Test
    @DisplayName("Estados terminales NO_ADMISIBLE, NO_APLICABLE e INICIATIVA_ARCHIVADA existen en el catálogo")
    void estadosTerminalesExisten() {
        // La constitución declara terminales a NO_ADMISIBLE, NO_APLICABLE e INICIATIVA_ARCHIVADA.
        // Esta prueba actúa como contrato: cuando se implemente la máquina de estados,
        // deberá rechazar transiciones de salida desde estos estados.
        List<EstadoIniciativa> terminales = Arrays.asList(
                EstadoIniciativa.NO_ADMISIBLE,
                EstadoIniciativa.NO_APLICABLE,
                EstadoIniciativa.INICIATIVA_ARCHIVADA);
        for (EstadoIniciativa terminal : terminales) {
            assertNotNull(terminal);
        }
    }
}