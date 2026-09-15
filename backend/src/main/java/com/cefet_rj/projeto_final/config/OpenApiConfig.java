package com.cefet_rj.projeto_final.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME =
            "bearerAuth";

    @Bean
    public OpenAPI condominiumManagementOpenAPI() {

        SecurityScheme securityScheme =
                new SecurityScheme()
                        .name(SECURITY_SCHEME_NAME)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT");

        SecurityRequirement securityRequirement =
                new SecurityRequirement()
                        .addList(SECURITY_SCHEME_NAME);

        return new OpenAPI()
                .info(
                        new Info()
                                .title("Condominium Management API")
                                .description(
                                        """
                                        API para gerenciamento de condomínios.

                                        Módulos principais:

                                        - autenticação e usuários
                                        - condomínios
                                        - memberships
                                        - setores e unidades
                                        - encomendas
                                        - notificações
                                        - billing e features
                                        - comunicados
                                        - reservas
                                        - relatórios
                                        """
                                )
                                .version("1.0.0")
                                .contact(
                                        new Contact()
                                                .name("Projeto Final - CEFET/RJ")
                                )
                )
                .addSecurityItem(securityRequirement)
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        SECURITY_SCHEME_NAME,
                                        securityScheme
                                )
                );
    }
}