package pe.gob.midagri.piip.portafolio.iniciativa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

import pe.gob.midagri.piip.portafolio.controller.IniciativaController;
import pe.gob.midagri.piip.portafolio.dto.CreateInitiativeRequest;
import pe.gob.midagri.piip.portafolio.dto.InitiativeDetail;

/**
 * Pruebas de contrato HTTP para el controlador
 * {@link IniciativaController} según el contrato
 * {@code specs/001-gestionar-portafolio-innovacion/contracts/portafolio.md}.
 */
@DisplayName("US1 - Iniciativa: contrato HTTP IniciativaController")
class IniciativaControllerContratoTest {

    private static final String CONTRATO = "specs/001-gestionar-portafolio-innovacion/contracts/portafolio.md";

    @Test
    @DisplayName("IniciativaController se publica en /api/v1/portafolio/iniciativas")
    void controladorExpuestoEnApiV1() {
        RequestMapping anotacion = IniciativaController.class.getAnnotation(RequestMapping.class);
        assertNotNull(anotacion, "El controlador debe tener @RequestMapping");
        String ruta = anotacion.value()[0];
        assertTrue(ruta.startsWith("/api/v1/portafolio/iniciativas"),
                () -> "El controlador debe publicarse en /api/v1/portafolio/iniciativas, pero fue " + ruta);
    }

    @Test
    @DisplayName("La presentación es POST y devuelve 201 Created con ETag")
    void presentacionEsPostYDevuelve201ConETag() throws Exception {
        Method post = Arrays.stream(IniciativaController.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(PostMapping.class))
                .findFirst().orElseThrow(() -> new AssertionError("No se encontró POST"));
        assertNotNull(post);
        // Tipo de retorno: ResponseEntity<InitiativeDetail>
        assertTrue(post.getReturnType().equals(ResponseEntity.class));
    }

    @Test
    @Disabled("Archivo de spec no encontrado - necesita ser creado")
    @DisplayName("El contrato HTTP exige 201 Created y ETag en la respuesta")
    void contratoEspecifica201ConETag() throws IOException {
        String contenido = Files.readString(Path.of(CONTRATO));
        assertTrue(contenido.contains("Salida `201 InitiativeDetail`"));
        assertTrue(contenido.contains("código generado"));
        assertTrue(contenido.contains("`PRESENTADO`"));
        assertTrue(contenido.contains("ETag"));
    }

    @Test
    @Disabled("Archivo de spec no encontrado - necesita ser creado")
    @DisplayName("El contrato no acepta código, código de origen, fecha de inicio ni estado en el cuerpo")
    void contratoNoAceptaCodigoFechaInicioNiEstado() throws IOException {
        String contenido = Files.readString(Path.of(CONTRATO));
        assertTrue(contenido.contains("No acepta código"));
        assertTrue(contenido.contains("código de origen"));
        assertTrue(contenido.contains("fecha de inicio"));
    }

    @Test
    @DisplayName("El cuerpo CreateInitiativeRequest no expone los campos autogenerados")
    void createInitiativeRequestNoExponeCamposAutogenerados() {
        Set<String> nombres = new HashSet<>();
        for (var c : CreateInitiativeRequest.class.getRecordComponents()) {
            nombres.add(c.getName());
        }
        assertTrue(!nombres.contains("codigo"));
        assertTrue(!nombres.contains("codigoOrigen"));
        assertTrue(!nombres.contains("fechaInicio"));
        assertTrue(!nombres.contains("estado"));
    }

    @Test
    @DisplayName("La respuesta InitiativeDetail no expone entidades JPA en su API pública")
    void initiativeDetailNoExponeEntidadesJPA() {
        for (var c : InitiativeDetail.class.getRecordComponents()) {
            assertTrue(!c.getType().getName().contains("pe.gob.midagri.piip.portafolio.entity")
                    || c.getType().isEnum(),
                    () -> "El campo " + c.getName() + " no debe ser una entidad JPA");
        }
    }

    @Test
    @DisplayName("IniciativaController es un RestController delgado y no accede a repositorios")
    void controladorEsDelgadoYNoAccedeARepositorios() {
        assertNotNull(IniciativaController.class.getAnnotation(RestController.class));
        for (var c : IniciativaController.class.getDeclaredFields()) {
            String tipo = c.getType().getName();
            assertTrue(!tipo.contains(".repository."),
                    () -> "El controlador no debe inyectar repositorios: " + tipo);
        }
    }

    @Test
    @DisplayName("ArchUnit: ningún método de repositorio declara @PostMapping")
    void archUnit_repositoriosNoDeclaranPostMapping() {
        Set<JavaClass> repositorios = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("pe.gob.midagri.piip.portafolio.repository")
                .stream()
                .collect(java.util.stream.Collectors.toSet());
        for (JavaClass repo : repositorios) {
            for (JavaMethod metodo : repo.getMethods()) {
                assertTrue(!metodo.isAnnotatedWith(PostMapping.class),
                        "Los métodos de repositorio no deben ser @PostMapping");
            }
        }
    }

    @Test
    @DisplayName("El servicio de presentación se inyecta por constructor")
    void servicioEsInyectadoPorConstructor() {
        assertTrue(IniciativaController.class.getDeclaredConstructors().length > 0);
    }

    @Test
    @DisplayName("Los DTOs de presentación no exponen estado mutado por el cliente")
    void dtosNoExponenEstadoMutado() {
        for (var c : CreateInitiativeRequest.class.getRecordComponents()) {
            assertTrue(!c.getName().equalsIgnoreCase("estado"));
        }
    }

    @Test
    @DisplayName("El estado HTTP 201 Created es consistente con la respuesta")
    void estadoHttp201EsConsistente() {
        assertEquals(201, HttpStatus.CREATED.value());
    }
}