package com.brieflyai.BrieflyAI.model.dto;

public record ResearchRequest(
        String content,
        String operation,
        String apiKey) {
}