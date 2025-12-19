package com.brieflyai.BrieflyAI.service;

import com.brieflyai.BrieflyAI.exception.ResearchServiceException;
import com.brieflyai.BrieflyAI.model.enums.ResearchOperation;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.brieflyai.BrieflyAI.model.dto.ResearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

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
        try {
            logger.info("Starting request processing: operation={}, contentLength={}", researchRequest.operation(),
                    researchRequest.content().length());

            Instant startTime = Instant.now();

            validateRequest(researchRequest);
            String prompt = buildPrompt(researchRequest);

            Client client = Client.builder().apiKey(researchRequest.apiKey()).build();
            GenerateContentResponse response = client.models.generateContent(geminiModel, prompt, config);

            Duration processingTime = Duration.between(startTime, Instant.now());

            logCompressionForSummarizeOperation(researchRequest, response.text());

            logger.info("Request completed successfully in {}ms, responseLength={}",
                    processingTime.toMillis(), response.text().length());

            return response.text();

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request: {}", e.getMessage());
            throw e;
        } catch (ResearchServiceException e) {
            logger.error("Service error for request {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error processing request {}", e.getMessage(), e);
            throw new ResearchServiceException("Internal service error occurred", e);
        } finally {
            MDC.clear();
        }
    }

    private void validateRequest(ResearchRequest researchRequest) {
        if (researchRequest == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        if (!StringUtils.hasText(researchRequest.operation())) {
            throw new IllegalArgumentException("Operation is required");
        }

        if (!StringUtils.hasText(researchRequest.content())) {
            throw new IllegalArgumentException("Content is required");
        }

        if (!StringUtils.hasText(researchRequest.apiKey())) {
            throw new IllegalArgumentException("API key is required");
        }

        logger.debug("Request validation passed");
    }

    private String buildPrompt(ResearchRequest researchRequest) {
        try {
            ResearchOperation operation = ResearchOperation.fromString(researchRequest.operation());
            String prompt = operation.getPromptTemplate() + "\n\n" + researchRequest.content();
            logger.debug("Built prompt for operation '{}', total length: {}",
                    researchRequest.operation(), prompt.length());
            return prompt;
        } catch (IllegalArgumentException e) {
            logger.error("Invalid operation '{}': {}", researchRequest.operation(), e.getMessage());
            throw new ResearchServiceException("Unsupported operation: " + researchRequest.operation(), e);
        }
    }

    private void logCompressionForSummarizeOperation(ResearchRequest request, String response) {
        if (ResearchOperation.SUMMARIZE.getOperation().equalsIgnoreCase(request.operation())) {
            int originalLength = request.content().length();
            int summaryLength = response.length();
            double compressionRatio = (double) summaryLength / originalLength;
            double reductionPercentage = (1 - compressionRatio) * 100;
            logger.info(
                    "Compression: originalLength={}, summaryLength={}, compressionRatio={}, reductionPercentage={}%",
                    originalLength, summaryLength, String.format("%.2f", compressionRatio),
                    String.format("%.2f", reductionPercentage));
        }
    }
}