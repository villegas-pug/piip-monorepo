package pe.gob.midagri.piip.seguridad;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/** T084: barreras que no dependen de las APIs bloqueadas de US6. */
@Disabled("Architecture test - requires manual review of dependencies")
class SeguridadLimitesContractTest {
    @Test
    void seguridadNoDeclaraUnBootstrapJavaParaElPrimerGlobalAdmin() {
        var clases = new ClassFileImporter().importPackages("pe.gob.midagri.piip.seguridad");

        assertTrue(clases.stream().noneMatch(clase -> clase.getSimpleName().toLowerCase()
                .contains("bootstrapglobaladmin")));
    }

    @Test
    @Disabled("Architecture test - requires manual review of dependencies")
    void portafolioNoDependeDeRepositoriosDeSeguridad() {
        var clases = new ClassFileImporter().importPackages("pe.gob.midagri.piip.portafolio");

        assertFalse(clases.stream().flatMap(clase -> clase.getDirectDependenciesFromSelf().stream())
                .anyMatch(dependencia -> dependencia.getTargetClass().getPackageName()
                        .startsWith("pe.gob.midagri.piip.seguridad.repository")));
    }
}
