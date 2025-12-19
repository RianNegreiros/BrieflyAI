package com.brieflyai.BrieflyAI.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResearchRequest(
        @NotBlank(message = "Content is required") @Size(min = 50, max = 50000, message = "Content must be between 50 and 50000 characters") String content,

        @NotBlank(message = "Operation is required") @Pattern(regexp = "summarize|suggest", message = "Operation must be 'summarize' or 'suggest'") String operation,

        @NotBlank(message = "API key is required") @Size(min = 20, message = "Invalid API key format") String apiKey) {
}
