package com.brieflyai.BrieflyAI.service;

import com.brieflyai.BrieflyAI.exception.ResearchServiceException;
import com.brieflyai.BrieflyAI.model.dto.GeminiResponse;
import com.brieflyai.BrieflyAI.model.enums.ResearchOperation;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.brieflyai.BrieflyAI.model.dto.ResearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;
import java.util.Optional;

@Service
public class ResearchService {

    private static final Logger logger = LoggerFactory.getLogger(ResearchService.class);
    private static final String NO_CONTENT_FOUND = "No content found in response";
    
    @Value("${gemini.api.url}")
    private String geminiUrl;
    
    @Value("${gemini.api.key}")
    private String geminiKey;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public ResearchService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public String processContent(ResearchRequest researchRequest) {
            logger.info("Processing research request: operation={}, contentLength={}",
                    researchRequest != null ? researchRequest.getOperation() : "null",
                    researchRequest != null && researchRequest.getContent() != null ? researchRequest.getContent().length() : 0);
            validateRequest(researchRequest);
            try {
                String prompt = buildPrompt(researchRequest);
                logger.debug("Built prompt: {}", prompt);
                Map<String, Object> requestBody = createRequestBody(prompt);
                logger.debug("Request body for Gemini API: {}", requestBody);
                String response = callGeminiApi(requestBody);
                logger.info("Received response from Gemini API, length={}", response != null ? response.length() : 0);
                return extractTextFromResponse(response);
            } catch (ResearchServiceException e) {
                throw e;
            } catch (Exception e) {
                logger.error("Error processing research request", e);
                throw new ResearchServiceException("Failed to process research request", e);
            }
    }

        private void validateRequest(ResearchRequest researchRequest) {
                if (researchRequest == null) {
                    logger.warn("Research request is null");
                    throw new IllegalArgumentException("Research request cannot be null");
                }
                if (!StringUtils.hasText(researchRequest.getOperation())) {
                    logger.warn("Operation is null or empty");
                    throw new IllegalArgumentException("Operation cannot be null or empty");
                }
                if (!StringUtils.hasText(researchRequest.getContent())) {
                    logger.warn("Content is null or empty");
                    throw new IllegalArgumentException("Content cannot be null or empty");
                }
    }

  private Map<String, Object> createRequestBody(String prompt) {
    return Map.of(
              "contents", new Object[] {
                      Map.of("parts", new Object[] {
                              Map.of("text", prompt)
                      })
                }
              );
  }

  private String callGeminiApi(Map<String, Object> requestBody) {
        try {
            logger.info("Calling Gemini API at {}", geminiUrl);
            return webClient.post()
                    .uri(geminiUrl + geminiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .onErrorMap(WebClientResponseException.class, ex -> {
                        String userMessage;
                        int status = ex.getStatusCode().value();
                        if (status >= 400 && status < 500) {
                            userMessage = "The request to the external service was invalid. Please check your input.";
                        } else if (status >= 500) {
                            userMessage = "The external service is currently unavailable. Please try again later.";
                        } else {
                            userMessage = "An unexpected error occurred while communicating with the external service.";
                        }
                        logger.error("Gemini API call failed: status={}, body={}, message={}", status, ex.getResponseBodyAsString(), ex.getMessage());
                        return new ResearchServiceException(userMessage, ex);
                    })
                    .onErrorMap(Exception.class, ex -> {
                        logger.error("Unexpected error during Gemini API call: {}", ex.getMessage(), ex);
                        return new ResearchServiceException("Unexpected error during Gemini API call. Please try again later.", ex);
                    })
                    .block();
        } catch (WebClientResponseException ex) {
            String userMessage;
            int status = ex.getStatusCode().value();
            if (status >= 400 && status < 500) {
                userMessage = "The request to the external service was invalid. Please check your input.";
            } else if (status >= 500) {
                userMessage = "The external service is currently unavailable. Please try again later.";
            } else {
                userMessage = "An unexpected error occurred while communicating with the external service.";
            }
            logger.error("Gemini API call failed: status={}, body={}, message={}", status, ex.getResponseBodyAsString(), ex.getMessage());
            throw new ResearchServiceException(userMessage, ex);
        } catch (Exception e) {
            logger.error("Exception during Gemini API call", e);
            throw new ResearchServiceException("Unexpected error during Gemini API call. Please try again later.", e);
        }
    }

    private String extractTextFromResponse(String response) {
            if (!StringUtils.hasText(response)) {
                logger.warn("No content found in Gemini API response");
                return NO_CONTENT_FOUND;
            }
            try {
                GeminiResponse geminiResponse = objectMapper.readValue(response, GeminiResponse.class);
                String extracted = Optional.ofNullable(geminiResponse)
                        .map(GeminiResponse::getCandidates)
                        .filter(candidates -> !candidates.isEmpty())
                        .map(candidates -> candidates.get(0))
                        .map(GeminiResponse.Candidate::getContent)
                        .map(GeminiResponse.Content::getParts)
                        .filter(parts -> !parts.isEmpty())
                        .map(parts -> parts.get(0))
                        .map(GeminiResponse.Part::getText)
                        .filter(StringUtils::hasText)
                        .orElse(NO_CONTENT_FOUND);
                logger.debug("Extracted text from response: {}", extracted);
                return extracted;
            } catch (Exception e) {
                logger.error("Failed to parse response from Gemini API", e);
                throw new ResearchServiceException("Failed to parse response from Gemini API", e);
            }
    }

    private String buildPrompt(ResearchRequest researchRequest) {
        try {
            ResearchOperation operation = ResearchOperation.fromString(researchRequest.getOperation());
            String prompt = operation.getPromptTemplate() + "\n\n" + researchRequest.getContent();
            logger.debug("Prompt built for operation {}: {}", researchRequest.getOperation(), prompt);
            return prompt;
        } catch (IllegalArgumentException e) {
            logger.error("Invalid operation: {}", researchRequest.getOperation());
            throw new ResearchServiceException("Invalid operation: " + researchRequest.getOperation());
        }
  }
}
