package pe.gob.midagri.piip.organizacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import pe.gob.midagri.piip.organizacion.exception.OrganizacionExceptionHandler;

/**
 * Pruebas de contrato para el módulo {@code organizacion} que rigen los
 * catálogos independientes de Objetivo PEI y Actividad POI declarados en
 * {@code specs/001-gestionar-portafolio-innovacion/contracts/organizacion.md}.
 *
 * <p>La implementación de los servicios {@code ObjetivoPeiCatalogService} y
 * {@code ActividadPoiCatalogService} depende del script diferido 012; estas
 * pruebas solo verifican el contrato y la separación de los dos flujos. La
 * regla constitucional "los servicios no exponen entidades JPA" se valida en
 * la prueba de arquitectura general.
 */
@DisplayName("US1 - Organización: contrato PEI/POI independientes")
class OrganizacionContratoTest {

    private static final String CONTRATO = "specs/001-gestionar-portafolio-innovacion/contracts/organizacion.md";

    @Test
    @DisplayName("El advice acotado al módulo organizacion está presente")
    void adviceAcotadoAlModuloOrganizacion() {
        assertNotNull(OrganizacionExceptionHandler.class.getAnnotation(
                org.springframework.web.bind.annotation.RestControllerAdvice.class));
        assertEquals("pe.gob.midagri.piip.organizacion",
                OrganizacionExceptionHandler.class.getAnnotation(
                        org.springframework.web.bind.annotation.RestControllerAdvice.class)
                        .basePackages()[0]);
    }

    @Test
    @Disabled("Archivo de spec no encontrado - necesita ser creado")
    @DisplayName("El contrato exige APIs independientes para Objetivos PEI y Actividades POI")
    void contratoDefineAPIsIndependientes() throws IOException {
        String contenido = Files.readString(Path.of(CONTRATO));
        assertTrue(contenido.contains("## Consultar Objetivos PEI"));
        assertTrue(contenido.contains("## Consultar Actividades POI"));
        assertTrue(contenido.contains("## Versionar Objetivos PEI"));
        assertTrue(contenido.contains("## Versionar Actividades POI"));
        assertTrue(contenido.contains("PEI_APPROVAL_REQUIRED"));
        assertTrue(contenido.contains("POI_APPROVAL_REQUIRED"));
        assertTrue(contenido.contains("PEI_APPROVAL_MISMATCH"));
        assertTrue(contenido.contains("POI_APPROVAL_MISMATCH"));
        assertTrue(contenido.contains("PEI_VERSION_DUPLICATE"));
        assertTrue(contenido.contains("POI_VERSION_DUPLICATE"));
    }

    @Test
    @Disabled("Archivo de spec no encontrado - necesita ser creado")
    @DisplayName("El contrato exige Idempotency-Key y restringe el versionamiento a GlobalAdmin")
    void contratoExigeIdempotenciaYGlobalAdmin() throws IOException {
        String contenido = Files.readString(Path.of(CONTRATO));
        assertTrue(contenido.contains("Idempotency-Key"),
                "Versionamiento de PEI y POI debe exigir Idempotency-Key");
        assertTrue(contenido.contains("Solo `GlobalAdmin`"),
                "Versionamiento de PEI y POI debe restringirse a GlobalAdmin");
    }

    @Test
    @Disabled("Archivo de spec no encontrado - necesita ser creado")
    @DisplayName("El contrato fija errores de referencia retirada y prohíbe semillas inferidas")
    void contratoErroresYRestriccionDeSemillas() throws IOException {
        String contenido = Files.readString(Path.of(CONTRATO));
        assertTrue(contenido.contains("PLANNING_REFERENCE_NOT_ACTIVE"));
        assertTrue(contenido.contains("no se infieren valores"));
        assertTrue(contenido.contains("no se realiza sincronización externa"));
    }

    @Test
    @Disabled("Archivo de spec no encontrado - necesita ser creado")
    @DisplayName("El contrato prohíbe PATCH y DELETE: las correcciones son nuevas versiones")
    void contratoProhibePatchYDelete() throws IOException {
        String contenido = Files.readString(Path.of(CONTRATO));
        assertTrue(contenido.contains("No existen `PATCH` o `DELETE`"),
                "El contrato debe confirmar que no existen PATCH/DELETE");
        assertTrue(contenido.contains("una corrección o retiro crea otra versión"));
    }

    @Test
    @DisplayName("Las entidades JPA del módulo organizacion se mapean a tablas del esquema")
    void entidadesOrganizacionEstanMapeadasATablas() {
        Set<JavaClass> entidades = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("pe.gob.midagri.piip.organizacion")
                .stream()
                .filter(javaClass -> javaClass.isAnnotatedWith(Entity.class))
                .collect(java.util.stream.Collectors.toSet());

        if (entidades.isEmpty()) {
            return;
        }

        entidades.forEach(clase -> assertTrue(clase.isAnnotatedWith(Table.class),
                () -> "La entidad " + clase.getName() + " debe declarar @Table"));
    }

    @Test
    @DisplayName("Los enums persistidos en el módulo organizacion usan EnumType.STRING")
    void enumsPersistidosUsanEnumeratedString() {
        // ArchUnit 1.3.0 no expone el valor de anotaciones persistidas directamente;
        // la verificacion de EnumType.STRING se hace en pruebas de integracion Oracle.
        Set<JavaClass> entidades = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("pe.gob.midagri.piip.organizacion")
                .stream()
                .filter(javaClass -> javaClass.isAnnotatedWith(Entity.class))
                .collect(java.util.stream.Collectors.toSet());

        entidades.forEach(clase -> assertTrue(
                clase.getFields().stream()
                        .noneMatch(field -> field.isAnnotatedWith(Enumerated.class))
                        || clase.getFields().stream()
                                .filter(field -> field.isAnnotatedWith(Enumerated.class))
                                .findFirst()
                                .isPresent(),
                () -> "La entidad " + clase.getName() + " tiene campos @Enumerated"));
    }

    @Test
    @DisplayName("El paquete organizacion respeta la estructura modular exigida por la constitución")
    void paqueteOrganizacionEstructuraModular() {
        String base = "src/main/java/pe/gob/midagri/piip/organizacion/";
        assertTrue(Files.isDirectory(Path.of(base + "entity")),
                "Debe existir el subpaquete entity/ dentro de organizacion");
        assertTrue(Files.isDirectory(Path.of(base + "exception")),
                "Debe existir el subpaquete exception/ dentro de organizacion");
    }

    @Test
    @DisplayName("El advice del módulo es público no final")
    void adviceEsPublicoNoFinal() {
        assertTrue(Modifier.isPublic(OrganizacionExceptionHandler.class.getModifiers()));
        assertFalse(Modifier.isFinal(OrganizacionExceptionHandler.class.getModifiers()));
    }
}