package com.poc.pocpdf.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("POC Contratos - Template Service")
                        .version("v1")
                        .description("API para upload e renderização de templates de contratos, com versionamento e geração de PDF."));
    }
}
