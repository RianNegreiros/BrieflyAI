package com.brieflyai.BrieflyAI.service;

import com.brieflyai.BrieflyAI.exception.ResearchServiceException;
import com.brieflyai.BrieflyAI.model.enums.ResearchOperation;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.beans.factory.annotation.Value;
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

    private final Client client;
    private final GenerateContentConfig config;

    public ResearchService(Client client, GenerateContentConfig config) {
        this.client = client;
        this.config = config;
    }

    public String processContent(ResearchRequest researchRequest) {
        try {
            logger.info("Starting request processing: operation={}, contentLength={}", researchRequest.getOperation(),
                    researchRequest.getContent().length());

            Instant startTime = Instant.now();

            validateRequest(researchRequest);
            String prompt = buildPrompt(researchRequest);
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

        if (!StringUtils.hasText(researchRequest.getOperation())) {
            throw new IllegalArgumentException("Operation is required");
        }

        if (!StringUtils.hasText(researchRequest.getContent())) {
            throw new IllegalArgumentException("Content is required");
        }

        logger.debug("Request validation passed");
    }

    private String buildPrompt(ResearchRequest researchRequest) {
        try {
            ResearchOperation operation = ResearchOperation.fromString(researchRequest.getOperation());
            String prompt = operation.getPromptTemplate() + "\n\n" + researchRequest.getContent();
            logger.debug("Built prompt for operation '{}', total length: {}",
                    researchRequest.getOperation(), prompt.length());
            return prompt;
        } catch (IllegalArgumentException e) {
            logger.error("Invalid operation '{}': {}", researchRequest.getOperation(), e.getMessage());
            throw new ResearchServiceException("Unsupported operation: " + researchRequest.getOperation(), e);
        }
    }

    private void logCompressionForSummarizeOperation(ResearchRequest request, String response) {
        if (ResearchOperation.SUMMARIZE.getOperation().equalsIgnoreCase(request.getOperation())) {
            int originalLength = request.getContent().length();
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