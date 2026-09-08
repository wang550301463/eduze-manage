package com.eduze.platform.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

class LegacyOpenApiConfigurationTest {
    @Test
    void discoversCompatibilitySerializerAtDocumentGenerationTime() {
        ApplicationContext context = mock(ApplicationContext.class);
        var customizer = new LegacyOpenApiConfiguration().legacyLongSchemas(context);
        // Bean discovery must happen after application configuration registration, not during it.
        when(context.containsBean("longAsString")).thenReturn(true);
        var id = new IntegerSchema().format("int64");
        var api =
                new OpenAPI()
                        .components(
                                new Components()
                                        .addSchemas(
                                                "User", new ObjectSchema().addProperty("id", id)));
        customizer.customise(api);
        assertEquals("string", id.getType());
    }

    @Test
    void handlesOpenApi31SchemaTypes() {
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.containsBean("longAsString")).thenReturn(true);
        Schema<Object> id = new Schema<>();
        id.setTypes(Set.of("integer"));
        id.setFormat("int64");
        var api = new OpenAPI().components(new Components().addSchemas("Id", id));
        new LegacyOpenApiConfiguration().legacyLongSchemas(context).customise(api);
        assertEquals(Set.of("string"), id.getTypes());
    }

    @Test
    void preservesNumericCountersWithoutCompatibilitySerializer() {
        ApplicationContext context = mock(ApplicationContext.class);
        var counter = new IntegerSchema().format("int64");
        var api = new OpenAPI().components(new Components().addSchemas("Counter", counter));
        new LegacyOpenApiConfiguration().legacyLongSchemas(context).customise(api);
        assertEquals("integer", counter.getType());
    }
}
