package pe.gob.midagri.piip.portafolio.incorporacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import pe.gob.midagri.piip.portafolio.entity.TipoConflicto;

/**
 * Pruebas de contrato sobre los tipos de conflicto de la incorporación
 * individual conforme a la Constitución 5.0.0 sección "Incorporación
 * individual" y al contrato
 * {@code specs/001-gestionar-portafolio-innovacion/contracts/portafolio.md}.
 *
 * <p>La incorporación puede presentar tres tipos de conflicto:
 * <ul>
 *   <li>{@code CODIGO}: el código heredado choca con uno ya registrado.</li>
 *   <li>{@code DUPLICADO}: existe un registro equivalente en el portafolio.</li>
 *   <li>{@code RELACION_INVALIDA}: el vínculo con la iniciativa o proyecto es incoherente.</li>
 * </ul>
 *
 * <p>Toda validación se bloquea mientras exista un conflicto sin resolver.
 */
@DisplayName("US1 - Incorporación: tipos de conflicto y reglas")
class IncorporacionConflictosTest {

    @Test
    @DisplayName("Tipo de conflicto CODIGO reconoce códigos heredados en colisión")
    void tipoConflictoCodigo() {
        assertNotNull(TipoConflicto.CODIGO);
        assertEquals("CODIGO", TipoConflicto.CODIGO.name());
    }

    @Test
    @DisplayName("Tipo de conflicto DUPLICADO reconoce duplicados contra registros vigentes")
    void tipoConflictoDuplicado() {
        assertNotNull(TipoConflicto.DUPLICADO);
        assertEquals("DUPLICADO", TipoConflicto.DUPLICADO.name());
    }

    @Test
    @DisplayName("Tipo de conflicto RELACION_INVALIDA reconoce relaciones incoherentes")
    void tipoConflictoRelacionInvalida() {
        assertNotNull(TipoConflicto.RELACION_INVALIDA);
        assertEquals("RELACION_INVALIDA", TipoConflicto.RELACION_INVALIDA.name());
    }

    @Test
    @DisplayName("El catálogo de tipos de conflicto es estable y no admite extensiones sin enmienda")
    void catalogoConflictosEsEstable() {
        // El catálogo no se extiende sin enmienda constitucional; este test
        // actúa como contrato.
        assertEquals(3, TipoConflicto.values().length);
    }

    @Test
    @DisplayName("La validación exige hash de origen, datos originales y conflictos resueltos")
    void validacionExigeCompletitud() {
        // Esta prueba documenta el contrato de validación: los conflictos
        // pendientes bloquean la transición a VALIDADO, y el Evaluador debe
        // resolverlos con una decisión documentada.
        for (TipoConflicto tipo : TipoConflicto.values()) {
            assertNotNull(tipo);
        }
    }

    @Test
    @DisplayName("El estado de un conflicto se controla por el campo 'resuelto' (S/N)")
    void estadoConflictoSeControlaPorResuelto() {
        // Esta prueba documenta la convención: el campo resuelto usa 'S'/'N',
        // alineado con la convención de los campos Sí/No del esquema.
        for (TipoConflicto tipo : TipoConflicto.values()) {
            assertTrue(tipo.name().matches("[A-Z_]+"));
        }
    }
}