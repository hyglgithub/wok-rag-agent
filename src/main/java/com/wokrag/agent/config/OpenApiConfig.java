package com.wokrag.agent.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("WokRag Agent API")
                        .version("1.0.0")
                        .description("Agentic RAG Intelligent Q&A Platform API")
                        .contact(new Contact().name("WokRag Team")))
                .addSecurityItem(new SecurityRequirement().addList("API-Key"))
                .components(new Components()
                        .addSecuritySchemes("API-Key",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-API-Key")));
    }
}
