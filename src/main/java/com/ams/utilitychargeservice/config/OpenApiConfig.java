package com.ams.utilitychargeservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI utilityChargeServiceOpenAPI() {
        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT token issued by Group 1 identity-access-service");

        return new OpenAPI()
                .info(new Info()
                        .title("Utility Charge Service API")
                        .description("AMS Group 3 — Utility charge recording and calculation. " +
                                "Records water, electricity, gas, and parking usage. " +
                                "Feeds into billing-payment-service during invoice generation.")
                        .version("1.0.0")
                        .contact(new Contact().name("AMS-G3 Backend Developer 2")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components().addSecuritySchemes("bearerAuth", bearerScheme));
    }

    @Configuration
    public class AppConfig {

        /**
         * RestTemplate bean used by RealBillingServiceClient for service-to-service HTTP calls.
         * Spring does not auto-create this — it must be declared as a @Bean.
         */
        @Bean
        public RestTemplate restTemplate() {
            return new RestTemplate();
        }
    }
}