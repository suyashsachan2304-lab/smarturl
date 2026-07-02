package com.suyash.smarturl.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI smartUrlOpenAPI() {

        return new OpenAPI()

                .info(new Info()

                        .title("Smart URL API")

                        .description("""
                                Production-ready URL Shortener built with
                                Spring Boot, PostgreSQL, Redis and Docker.
                                """)

                        .version("1.0.0")

                        .contact(new Contact()
                                .name("Suyash Sachan")
                                .email("suyash@example.com"))

                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT"))
                );
    }

}