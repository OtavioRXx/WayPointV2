package com.waypoint.project_tcc_app.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI waypointOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("WayPoint API")
                        .version("v1")
                        .description("Documentacao dos endpoints do backend do sistema WayPoint."));
    }
}
