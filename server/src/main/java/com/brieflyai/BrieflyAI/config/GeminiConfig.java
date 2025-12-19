package com.brieflyai.BrieflyAI.config;

import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.ThinkingConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {

    @Bean
    GenerateContentConfig generateContentConfig() {
        // Disable thiking for faster response
        return GenerateContentConfig.builder()
                .thinkingConfig(ThinkingConfig.builder().thinkingBudget(0).build())
                .build();
    }
}
