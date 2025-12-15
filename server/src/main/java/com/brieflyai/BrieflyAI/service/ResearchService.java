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
import java.util.UUID;

@Service
public class ResearchService {

    private static final Logger logger = LoggerFactory.getLogger(ResearchService.class);
    private static final int MAX_CONTENT_LENGTH = 10000;
    private static final int MAX_RESPONSE_LENGTH = 50000;

    @Value("${gemini.api.model}")
    private String geminiModel;

    private final Client client;
    private final GenerateContentConfig config;

    public ResearchService(Client client, GenerateContentConfig config) {
        this.client = client;
        this.config = config;
    }

    public String processContent(ResearchRequest researchRequest) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("requestId", requestId);
        
        try {
            logger.info("Starting request processing: operation={}, contentLength={}", 
                    getOperationSafely(researchRequest), getContentLengthSafely(researchRequest));
            
            Instant startTime = Instant.now();
            
            validateRequest(researchRequest);
            String prompt = buildPrompt(researchRequest);
            String response = callGeminiApi(prompt, requestId);
            
            Duration processingTime = Duration.between(startTime, Instant.now());
            logger.info("Request completed successfully in {}ms, responseLength={}", 
                    processingTime.toMillis(), response.length());
            
            return response;
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request: {}", e.getMessage());
            throw e;
        } catch (ResearchServiceException e) {
            logger.error("Service error for request {}: {}", requestId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error processing request {}: {}", requestId, e.getMessage(), e);
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
        
        if (researchRequest.getContent().length() > MAX_CONTENT_LENGTH) {
            logger.warn("Content length {} exceeds maximum {}", 
                    researchRequest.getContent().length(), MAX_CONTENT_LENGTH);
            throw new IllegalArgumentException("Content exceeds maximum length of " + MAX_CONTENT_LENGTH);
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
    
    private String callGeminiApi(String prompt, String requestId) {
        try {
            logger.debug("Calling Gemini API for request {}", requestId);
            Instant apiStartTime = Instant.now();
            
            GenerateContentResponse response = client.models.generateContent(geminiModel, prompt, config);
            
            Duration apiTime = Duration.between(apiStartTime, Instant.now());
            logger.debug("Gemini API call completed in {}ms for request {}", 
                    apiTime.toMillis(), requestId);
            
            if (response == null) {
                logger.error("Gemini API returned null response for request {}", requestId);
                throw new ResearchServiceException("No response received from AI service");
            }
            
            String responseText = response.text();
            if (!StringUtils.hasText(responseText)) {
                logger.error("Gemini API returned empty response for request {}", requestId);
                throw new ResearchServiceException("Empty response received from AI service");
            }
            
            if (responseText.length() > MAX_RESPONSE_LENGTH) {
                logger.warn("Response length {} exceeds maximum {} for request {}", 
                        responseText.length(), MAX_RESPONSE_LENGTH, requestId);
                responseText = responseText.substring(0, MAX_RESPONSE_LENGTH) + "... [truncated]";
            }
            
            return responseText;
            
        } catch (Exception e) {
            logger.error("Gemini API call failed for request {}: {}", requestId, e.getMessage(), e);
            throw new ResearchServiceException("AI service unavailable: " + e.getMessage(), e);
        }
    }
    
    private String getOperationSafely(ResearchRequest request) {
        return request != null ? request.getOperation() : "null";
    }
    
    private int getContentLengthSafely(ResearchRequest request) {
        return request != null && request.getContent() != null ? request.getContent().length() : 0;
    }
}