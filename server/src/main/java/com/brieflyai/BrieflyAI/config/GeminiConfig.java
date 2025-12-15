package com.brieflyai.BrieflyAI.config;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.ThinkingConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {

    @Value("${gemini.api.key}")
    private String geminiKey;

    @Bean
    public Client geminiClient() {
        return Client.builder().apiKey(geminiKey).build();
    }

    @Bean
    public GenerateContentConfig generateContentConfig() {
        // Disable thiking for faster response
        return GenerateContentConfig.builder()
                .thinkingConfig(ThinkingConfig.builder().thinkingBudget(0).build())
                .build();
    }
}
