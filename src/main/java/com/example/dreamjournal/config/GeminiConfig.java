package com.example.dreamjournal.config;

import com.google.genai.Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {

    @Bean
    public Client geminiClient() {
        return Client.builder()
                .vertexAI(true)
                .build();
    }
}