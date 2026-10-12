package com.itau.insurance.patrimonial.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OpenApiConfigTest {

    @Test
    void customOpenAPI_shouldIncludeMetadataAndSecurity() {
        OpenApiConfig config = new OpenApiConfig();
        ReflectionTestUtils.setField(config, "appName", "Test API");
        ReflectionTestUtils.setField(config, "appDescription", "Test Description");
        ReflectionTestUtils.setField(config, "appVersion", "9.9.9");
        ReflectionTestUtils.setField(config, "contactName", "Test Team");
        ReflectionTestUtils.setField(config, "contactEmail", "test@example.com");
        ReflectionTestUtils.setField(config, "contactUrl", "https://example.com");
        ReflectionTestUtils.setField(config, "licenseName", "MIT");
        ReflectionTestUtils.setField(config, "licenseUrl", "https://example.com/license");

        var openApi = config.customOpenAPI();

        assertEquals("Test API", openApi.getInfo().getTitle());
        assertEquals("Test Description", openApi.getInfo().getDescription());
        assertEquals("9.9.9", openApi.getInfo().getVersion());
        assertNotNull(openApi.getComponents().getSecuritySchemes().get("Bearer Authentication"));
        assertEquals(4, openApi.getServers().size());
        assertEquals(1, openApi.getSecurity().size());
    }
}
