package com.eduze.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ServiceArchitectureTest {
    private Path repositoryRoot() {
        Path candidate = Path.of("").toAbsolutePath();
        while (candidate != null && !Files.isDirectory(candidate.resolve("services"))) {
            candidate = candidate.getParent();
        }
        assertThat(candidate).as("Repository root must contain services directory").isNotNull();
        return candidate;
    }

    @Test
    void serviceBytecodeCannotDependOnAnotherServiceImplementation() throws Exception {
        Path root = repositoryRoot();
        Map<String, String> owners = new LinkedHashMap<>();
        Map<String, JavaClasses> services = new LinkedHashMap<>();
        try (var paths = Files.list(root.resolve("services"))) {
            for (Path service : paths.filter(Files::isDirectory).toList()) {
                Path classes = service.resolve("target/classes");
                assertThat(classes).as("Build service before checking its architecture").exists();
                JavaClasses imported = new ClassFileImporter().importPath(classes);
                services.put(service.getFileName().toString(), imported);
                for (JavaClass type : imported) {
                    owners.put(type.getName(), service.getFileName().toString());
                }
            }
        }
        services.forEach(
                (service, classes) ->
                        classes.forEach(
                                type ->
                                        type.getDirectDependenciesFromSelf()
                                                .forEach(
                                                        dependency -> {
                                                            String owner =
                                                                    owners.get(
                                                                            dependency
                                                                                    .getTargetClass()
                                                                                    .getName());
                                                            assertThat(
                                                                            owner == null
                                                                                    || owner.equals(
                                                                                            service))
                                                                    .as(
                                                                            "%s imports implementation from %s: %s",
                                                                            service,
                                                                            owner,
                                                                            dependency
                                                                                    .getDescription())
                                                                    .isTrue();
                                                        })));
    }

    @Test
    void controllersDelegatePersistenceToApplicationServices() throws Exception {
        Path root = repositoryRoot();
        try (var paths = Files.list(root.resolve("services"))) {
            for (Path service : paths.filter(Files::isDirectory).toList()) {
                JavaClasses imported =
                        new ClassFileImporter().importPath(service.resolve("target/classes"));
                for (JavaClass type : imported) {
                    if (!type.isAnnotatedWith(
                            "org.springframework.web.bind.annotation.RestController")) {
                        continue;
                    }
                    for (var dependency : type.getDirectDependenciesFromSelf()) {
                        String target = dependency.getTargetClass().getName();
                        assertThat(
                                        target.startsWith("org.springframework.jdbc.")
                                                || target.contains(".mapper."))
                                .as(
                                        "Controller bypasses application layer: %s",
                                        dependency.getDescription())
                                .isFalse();
                    }
                }
            }
        }
    }
}
