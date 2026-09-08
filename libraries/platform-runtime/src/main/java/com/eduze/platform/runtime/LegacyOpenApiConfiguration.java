package com.eduze.platform.runtime;

import io.swagger.v3.oas.models.media.Schema;
import java.util.Set;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Matches the existing identity/academic Jackson Long-as-string compatibility serializer. */
@Configuration
public class LegacyOpenApiConfiguration {
    @Bean
    public OpenApiCustomizer legacyLongSchemas(ApplicationContext context) {
        return api -> {
            if (!context.containsBean("longAsString")) {
                return;
            }
            if (api.getComponents() != null && api.getComponents().getSchemas() != null) {
                api.getComponents().getSchemas().values().forEach(this::strings);
            }
            if (api.getPaths() != null) {
                api.getPaths()
                        .values()
                        .forEach(
                                path ->
                                        path.readOperations()
                                                .forEach(
                                                        operation -> {
                                                            if (operation.getParameters() != null) {
                                                                operation
                                                                        .getParameters()
                                                                        .forEach(
                                                                                parameter ->
                                                                                        strings(
                                                                                                parameter
                                                                                                        .getSchema()));
                                                            }
                                                        }));
            }
        };
    }

    private void strings(Schema<?> schema) {
        if (schema == null) {
            return;
        }
        if (("integer".equals(schema.getType())
                        || (schema.getTypes() != null && schema.getTypes().contains("integer")))
                && "int64".equals(schema.getFormat())) {
            schema.setType("string");
            if (schema.getTypes() != null) {
                schema.setTypes(Set.of("string"));
            }
            schema.setFormat(null);
        }
        if (schema.getProperties() != null) {
            schema.getProperties().values().forEach(this::strings);
        }
        strings(schema.getItems());
        if (schema.getAdditionalProperties() instanceof Schema<?> additional) {
            strings(additional);
        }
    }
}
