package com.brieflyai.BrieflyAI.service;

import com.brieflyai.BrieflyAI.exception.ResearchServiceException;
import com.brieflyai.BrieflyAI.model.dto.ResearchRequest;
import com.brieflyai.BrieflyAI.model.enums.ResearchOperation;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class ResearchService {

    private static final Logger logger = LoggerFactory.getLogger(ResearchService.class);

    @Value("${gemini.api.model}")
    private String geminiModel;

    private final GenerateContentConfig config;

    public ResearchService(GenerateContentConfig config) {
        this.config = config;
    }

    @Cacheable(value = "research", key = "#researchRequest.operation + ':' + #researchRequest.content.hashCode()")
    public String processContent(ResearchRequest researchRequest) {
        Instant startTime = Instant.now();

        logger.info("Processing request - operation: {}, contentLength: {}",
                researchRequest.operation(),
                researchRequest.content().length());

        try {
            String prompt = buildPrompt(researchRequest);

            Client client = Client.builder()
                    .apiKey(researchRequest.apiKey())
                    .build();

            GenerateContentResponse response = client.models.generateContent(
                    geminiModel,
                    prompt,
                    config);

            Duration processingTime = Duration.between(startTime, Instant.now());

            logger.info("Request completed - operation: {}, processingTime: {}ms, responseLength: {}",
                    researchRequest.operation(),
                    processingTime.toMillis(),
                    response.text().length());

            logCompressionMetrics(researchRequest, response.text());

            return response.text();

        } catch (Exception e) {
            logger.error("Error processing request - operation: {}, error: {}",
                    researchRequest.operation(),
                    e.getMessage(),
                    e);
            throw new ResearchServiceException("Failed to process content", e);
        }
    }

    private String buildPrompt(ResearchRequest researchRequest) {
        try {
            ResearchOperation operation = ResearchOperation.fromString(
                    researchRequest.operation());
            return operation.getPromptTemplate() + "\n\n" + researchRequest.content();
        } catch (IllegalArgumentException e) {
            logger.error("Invalid operation: {}", researchRequest.operation());
            throw new ResearchServiceException("Unsupported operation: " + researchRequest.operation(), e);
        }
    }

    private void logCompressionMetrics(ResearchRequest request, String response) {
        if ("summarize".equalsIgnoreCase(request.operation())) {
            int originalLength = request.content().length();
            int summaryLength = response.length();
            double compressionRatio = (double) summaryLength / originalLength;
            double reductionPercentage = (1 - compressionRatio) * 100;

            logger.info("Compression metrics - originalLength: {}, summaryLength: {}, " +
                    "compressionRatio: {:.2f}, reduction: {:.2f}%",
                    originalLength,
                    summaryLength,
                    compressionRatio,
                    reductionPercentage);
        }
    }
}
