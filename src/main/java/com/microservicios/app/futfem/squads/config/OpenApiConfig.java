package com.microservicios.app.futfem.squads.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI squadsTempOpenApi(
        @Value("${api.docs.server-url:/}") String serverUrl
    ) {
        return new OpenAPI()
            .info(new Info()
                .title("Futfem Squads Temp API")
                .version("v0.1.0"))
            .servers(List.of(new Server().url(serverUrl)));
    }
}
