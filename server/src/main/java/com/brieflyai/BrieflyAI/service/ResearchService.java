package com.brieflyai.BrieflyAI.service;

import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import com.brieflyai.BrieflyAI.exception.ApiKeyException;
import com.brieflyai.BrieflyAI.exception.ResearchServiceException;
import com.brieflyai.BrieflyAI.model.dto.ResearchRequest;
import com.brieflyai.BrieflyAI.model.enums.ResearchOperation;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.ThinkingConfig;

@Service
public class ResearchService {

    private static final Logger logger = LoggerFactory.getLogger(ResearchService.class);

    @Value("${gemini.api.model}")
    private String geminiModel;

    public ResearchService() {}

    @Cacheable(value = "research",
            key = "#researchRequest.operation + ':' + #researchRequest.content.hashCode()")
    public String processContent(ResearchRequest researchRequest) {
        Instant startTime = Instant.now();

        logger.info("Processing request - operation: {}, contentLength: {}",
                researchRequest.operation(), researchRequest.content().length());

        try {
            ResearchOperation operation = ResearchOperation.fromString(researchRequest.operation());

            GenerateContentConfig operationConfig = GenerateContentConfig.builder()
                    .thinkingConfig(ThinkingConfig.builder().thinkingBudget(0).build())
                    .systemInstruction(
                            Content.fromParts(Part.fromText(operation.getPromptTemplate())))
                    .build();

            Client client = Client.builder().apiKey(researchRequest.apiKey()).build();

            GenerateContentResponse response = client.models.generateContent(geminiModel,
                    researchRequest.content(), operationConfig);

            if (response == null || response.text() == null || response.text().isEmpty()) {
                throw new ResearchServiceException("Empty response from AI service");
            }

            Duration processingTime = Duration.between(startTime, Instant.now());
            logger.info(
                    "Request completed - operation: {}, processingTime: {}ms, responseLength: {}",
                    researchRequest.operation(), processingTime.toMillis(),
                    response.text().length());

            logCompressionMetrics(researchRequest, response.text());

            return response.text();

        } catch (SecurityException e) {
            logger.error("API key authentication failed: {}", e.getMessage());
            throw new ApiKeyException("Invalid or expired API key", e);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request parameters: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error processing request - operation: {}, error: {}",
                    researchRequest.operation(), e.getMessage(), e);
            throw new ResearchServiceException("Failed to process content: " + e.getMessage(), e);
        }
    }

    private void logCompressionMetrics(ResearchRequest request, String response) {
        if ("summarize".equalsIgnoreCase(request.operation())) {
            int originalLength = request.content().length();
            int summaryLength = response.length();
            double compressionRatio = (double) summaryLength / originalLength;
            double reductionPercentage = (1 - compressionRatio) * 100;

            logger.info(
                    "Compression metrics - originalLength: {}, summaryLength: {}, "
                            + "compressionRatio: {}, reduction: {}%",
                    originalLength, summaryLength, String.format("%.2f", compressionRatio),
                    String.format("%.2f", reductionPercentage));
        }
    }
}
