package com.swp391.eyewear_management_backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    // BỘ LỌC ĐỂ ẨN CÁC API RÁC CỦA HATEOAS
//    @Bean
//    public GroupedOpenApi publicApi() {
//        return GroupedOpenApi.builder()
//                .group("public-apis")
//                .pathsToMatch(
//                        "/auth/**",
//                        "/checkout/**",
//                        "/ghn/**",
//                        "/api/operation-staff/orders/**",
//                        "/api/cart/**",
//                        "/orders/**",
//                        "/payments/vnpay/**",
//                        "/users/**",
//                        "/api/staff/orders/**",
//                        "/api/products/**",
//                        "/api/returns/**",
//                        "/api/return-exchanges/**",
//                        "/api/payment/**"
//                )
//                .packagesToScan("com.swp391.eyewear_management_backend.controller")
//                .build();
//    }

    @Bean
    public OpenAPI openAPI() {
        final String schemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info().title("Eyewear API").version("1.0"))
                .addServersItem(new Server().url("https://api-eyewear.purintech.id.vn").description("Production Server"))
                .addServersItem(new Server().url("http://localhost:8080").description("Local Dev (HTTP)"))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components().addSecuritySchemes(
                        schemeName,
                        new SecurityScheme()
                                .name(schemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                ));
    }
}