package pe.gob.midagri.piip.consulta;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;

/** T095: la proyección pública no puede incorporar una descarga documental. */
class ConsultaPublicaContratoTest {
    @Test
    void noExisteEndpointPublicoDeContenidoODescargaDocumental() {
        var clases = new ClassFileImporter().importPackages("pe.gob.midagri.piip");

        assertTrue(clases.stream().flatMap(clase -> clase.getMethods().stream())
                .filter(method -> method.isAnnotatedWith(GetMapping.class))
                .noneMatch(method -> method.getOwner().getPackageName().contains("consulta")
                        && method.getName().toLowerCase().matches(".*(contenido|descarga).*")));
    }
}
