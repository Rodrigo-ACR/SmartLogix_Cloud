package com.smartlogix.envios.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

        @Bean
        public OpenAPI customOpenAPI() {

                return new OpenAPI()

                                .info(new Info()

                                                .title("SmartLogix - Envios API")

                                                .version("1.0")

                                                .description(
                                                                "Microservicio de logística y envíos")

                                                .contact(new Contact()
                                                                .name("SmartLogix Team")
                                                                .email("smartlogix@demo.cl")));
        }
}