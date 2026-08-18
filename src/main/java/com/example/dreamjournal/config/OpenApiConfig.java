package com.example.dreamjournal.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI dreamJournalOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Dream Journal API")
                        .version("v1")
                        .description("REST APIs for sleep day logs and dreams backed by Google Firestore."));
    }
}
