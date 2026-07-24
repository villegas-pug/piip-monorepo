package pe.gob.midagri.piip.architecture;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.persistence.Entity;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

/**
 * Pruebas ArchUnit que blindan los límites arquitectónicos del monolito
 * modular PIIP definidos en la Constitución 4.0.0 y la tarea T118.
 *
 * <p>Las reglas se evalúan sobre el paquete raíz
 * {@code pe.gob.midagri.piip} y utilizan {@code allowEmptyShould(true)}
 * para mantenerse verdes mientras los módulos no existan o se encuentren
 * parcialmente implementados.
 */
@AnalyzeClasses(packages = "pe.gob.midagri.piip", importOptions = ImportOption.DoNotIncludeTests.class)
class ModularArchitectureTest {

    /**
     * Módulos constitucionales de PIIP conforme a la Constitución 4.0.0.
     * Cada módulo debe tener exactamente un {@link RestControllerAdvice}
     * acotado a su propio paquete.
     */
    private static final List<String> CONSTITUTIONAL_MODULES = List.of(
            "organizacion", "seguridad", "portafolio", "documentos",
            "reportes", "consulta", "auditoria");

    /**
     * Paquetes donde se permiten conectores funcionales externos.
     * Únicamente el adaptador de Keycloak dentro de seguridad/service/impl
     * y los servicios de consulta pública (sin conectores externos).
     */
    private static final List<String> ALLOWED_EXTERNAL_CONNECTOR_PACKAGES = List.of(
            "pe.gob.midagri.piip.config..",
            "pe.gob.midagri.piip.seguridad.config..",
            "pe.gob.midagri.piip.seguridad.service.impl..",
            "pe.gob.midagri.piip.consulta..");

    /**
     * (1) Los controladores no deben depender de repositorios.
     * Los controladores delegan en servicios; nunca consumen repositorios.
     */
    @ArchTest
    static final ArchRule controllers_should_not_depend_on_repositories =
            noClasses()
                    .that().areAnnotatedWith(RestController.class)
                    .should().dependOnClassesThat().resideInAPackage("..repository..")
                    .allowEmptyShould(true)
                    .because("Los controladores delegan en servicios; no deben consumir repositorios");

    /**
     * (2) Los servicios no deben exponer entidades JPA en su API pública.
     * Los contratos de servicio exponen DTO o tipos simples.
     */
    @ArchTest
    static final ArchRule services_should_not_expose_jpa_entities =
            noMethods()
                    .that().areDeclaredInClassesThat().resideInAPackage("..service..")
                    .and().arePublic()
                    .should(new PublicServiceApiMustNotExposeEntityRule())
                    .allowEmptyShould(true)
                    .because("Los contratos de servicio exponen DTO; no deben devolver entidades JPA");

    /**
     * (3) Los repositorios de un módulo constitucional no deben ser
     * accedidos desde otro módulo constitucional ni desde fuera del mismo.
     * Combina una regla por cada módulo declarado en la Constitución.
     */
    @ArchTest
    static final ArchRule repositories_should_not_be_accessed_from_other_modules =
            noClasses()
                    .that().resideInAPackage("pe.gob.midagri.piip..")
                    .and().resideOutsideOfPackage("..repository..")
                    .should(new CrossModuleRepositoryAccessRule())
                    .allowEmptyShould(true)
                    .because("Los repositorios de cada módulo constitucional son privados y no deben accederse desde otro módulo");

    /**
     * (4) PIIP no admite los paquetes genéricos {@code model/}, {@code client/}
     * ni {@code integration/} bajo {@code pe.gob.midagri.piip}.
     */
    @ArchTest
    static final ArchRule forbidden_generic_packages =
            noClasses()
                    .should().resideInAnyPackage(
                            "pe.gob.midagri.piip.model..",
                            "pe.gob.midagri.piip.client..",
                            "pe.gob.midagri.piip.integration..")
                    .allowEmptyShould(true)
                    .because("La Constitución 4.0.0 prohíbe directorios genéricos model/, client/ e integration/");

    /**
     * (5) Solo el adaptador de Keycloak dentro de
     * {@code pe.gob.midagri.piip.seguridad.service.impl} puede depender de
     * {@code org.keycloak}.
     */
    @ArchTest
    static final ArchRule keycloak_only_in_seguridad_service_impl =
            noClasses()
                    .that().resideOutsideOfPackage("pe.gob.midagri.piip.seguridad.service.impl..")
                    .and().resideInAPackage("pe.gob.midagri.piip..")
                    .should().dependOnClassesThat().resideInAPackage("org.keycloak..")
                    .allowEmptyShould(true)
                    .because("Únicamente el adaptador de Keycloak en seguridad/service/impl puede usar org.keycloak");

    /**
     * (6) Exigir exactamente un {@link RestControllerAdvice} por módulo
     * constitucional, acotado al paquete propio del módulo.
     * La regla verifica que no exista más de una clase con @RestControllerAdvice
     * en cada paquete de módulo constitucional.
     */
    @ArchTest
    static void exactly_one_rest_controller_advice_per_module(JavaClasses importedClasses) {
        Map<String, List<String>> adviceClassesByModule = importedClasses.stream()
                .filter(javaClass -> javaClass.isAnnotatedWith(RestControllerAdvice.class))
                .filter(javaClass -> extractModule(javaClass.getPackageName()) != null)
                .collect(Collectors.groupingBy(
                        javaClass -> extractModule(javaClass.getPackageName()),
                        LinkedHashMap::new,
                        Collectors.mapping(JavaClass::getName, Collectors.toList())));

        List<String> violations = new ArrayList<>();
        for (String module : CONSTITUTIONAL_MODULES) {
            List<String> adviceClasses = adviceClassesByModule.getOrDefault(module, List.of());
            if (adviceClasses.size() != 1) {
                violations.add(String.format(
                        "El módulo '%s' debe tener exactamente un RestControllerAdvice, pero tiene %d. Advice encontrados: %s",
                        module,
                        adviceClasses.size(),
                        String.join(", ", adviceClasses)));
            }
        }

        if (!violations.isEmpty()) {
            throw new AssertionError(String.join(System.lineSeparator(), violations));
        }
    }

    /**
     * (7) Prohibir conectores funcionales externos no autorizados.
     * Solo se permiten conectores externos en {@code seguridad/service/impl}
     * (Keycloak) y en el módulo {@code consulta} (proyecciones públicas).
     * Los paquetes {@code integration/} y {@code client/} están prohibidos
     * por la regla (4).
     */
    @ArchTest
    static final ArchRule no_unauthorized_external_functional_connectors =
            noClasses()
                    .that().resideInAPackage("pe.gob.midagri.piip..")
                    .and().resideOutsideOfPackages(ALLOWED_EXTERNAL_CONNECTOR_PACKAGES.toArray(String[]::new))
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.keycloak..",
                            "org.springframework.security.oauth2..",
                            "org.springframework.web.client..",
                            "feign..",
                            "resttemplate..",
                            "webclient..")
                    .allowEmptyShould(true)
                    .because("Los conectores funcionales externos (Keycloak, OAuth2, Feign, "
                            + "RestTemplate, WebClient) solo se permiten en seguridad/service/impl "
                            + "y consulta");

    /**
     * Regla auxiliar que verifica que cada módulo constitucional tenga
     * exactamente un {@link RestControllerAdvice} en su propio paquete.
     */
    private static String extractModule(String packageName) {
        for (String module : CONSTITUTIONAL_MODULES) {
            if (packageName.contains("." + module + ".") || packageName.endsWith("." + module)) {
                return module;
            }
        }
        return null;
    }

    private static boolean isRepositoryPackage(String packageName) {
        return packageName.contains(".repository");
    }

    private static final class PublicServiceApiMustNotExposeEntityRule extends ArchCondition<JavaMethod> {
        private PublicServiceApiMustNotExposeEntityRule() {
            super("not expose JPA entities in the public service API");
        }

        @Override
        public void check(JavaMethod method, ConditionEvents events) {
            JavaClass returnType = method.getRawReturnType();
            if (returnType.isAnnotatedWith(Entity.class) || returnType.isEquivalentTo(Entity.class)) {
                String message = String.format(
                        "El método público %s.%s no debe exponer la entidad JPA %s.",
                        method.getOwner().getName(),
                        method.getName(),
                        returnType.getName());
                events.add(SimpleConditionEvent.violated(method, message));
            }
        }
    }

    private static final class CrossModuleRepositoryAccessRule extends ArchCondition<JavaClass> {
        private CrossModuleRepositoryAccessRule() {
            super("not depend on repositories from other constitutional modules");
        }

        @Override
        public void check(JavaClass javaClass, ConditionEvents events) {
            String sourceModule = extractModule(javaClass.getPackageName());
            if (sourceModule == null || isRepositoryPackage(javaClass.getPackageName())) {
                return;
            }

            List<String> offendingRepositories = javaClass.getDirectDependenciesFromSelf().stream()
                    .map(dependency -> dependency.getTargetClass())
                    .filter(targetClass -> isRepositoryPackage(targetClass.getPackageName()))
                    .filter(targetClass -> {
                        String targetModule = extractModule(targetClass.getPackageName());
                        return targetModule != null && !sourceModule.equals(targetModule);
                    })
                    .map(JavaClass::getName)
                    .distinct()
                    .toList();

            if (!offendingRepositories.isEmpty()) {
                String message = String.format(
                        "La clase %s no debe depender de repositorios de otro módulo: %s",
                        javaClass.getName(),
                        String.join(", ", offendingRepositories));
                events.add(SimpleConditionEvent.violated(javaClass, message));
            }
        }
    }
}
