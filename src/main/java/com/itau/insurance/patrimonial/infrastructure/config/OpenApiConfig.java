package com.itau.insurance.patrimonial.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Slf4j
@Configuration
public class OpenApiConfig {

    @Value("${app.name:Insurance Patrimonial API}")
    private String appName;

    @Value("${app.description:Insurance Patrimonial Management System}")
    private String appDescription;

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @Value("${app.contact.name:Insurance Team}")
    private String contactName;

    @Value("${app.contact.email:insurance@itau.com.br}")
    private String contactEmail;

    @Value("${app.contact.url:https://www.itau.com.br}")
    private String contactUrl;

    @Value("${app.license.name:Apache 2.0}")
    private String licenseName;

    @Value("${app.license.url:https://www.apache.org/licenses/LICENSE-2.0.html}")
    private String licenseUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        log.info("Configuring OpenAPI documentation for: {}", appName);

        return new OpenAPI()
                .info(buildApiInfo())
                .components(buildSecurityComponents())
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .servers(Arrays.asList(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api-dev.itau.com.br")
                                .description("Development Server"),
                        new Server()
                                .url("https://api-staging.itau.com.br")
                                .description("Staging Server"),
                        new Server()
                                .url("https://api-prod.itau.com.br")
                                .description("Production Server")
                ));
    }

    private Info buildApiInfo() {
        Contact contact = new Contact()
                .name(contactName)
                .email(contactEmail)
                .url(contactUrl);

        License license = new License()
                .name(licenseName)
                .url(licenseUrl);

        return new Info()
                .title(appName)
                .description(appDescription)
                .version(appVersion)
                .contact(contact)
                .license(license)
                .termsOfService("https://www.itau.com.br/terms");
    }

    private Components buildSecurityComponents() {
        return new Components()
                .addSecuritySchemes("Bearer Authentication", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT Authentication token")
                        .name("Authorization"));
    }
}
