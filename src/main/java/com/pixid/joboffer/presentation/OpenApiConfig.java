package com.pixid.joboffer.presentation;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    OpenAPI jobOfferOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Job Offer Service API")
                .version("v1")
                .description("Gestion des offres d'emploi (back-office recruteurs, job board, services internes). "
                        + "Les erreurs suivent la RFC 9457 (application/problem+json)."));
    }
}
