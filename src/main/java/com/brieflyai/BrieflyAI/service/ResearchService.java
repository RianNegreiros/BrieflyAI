package com.brieflyai.BrieflyAI.service;

import com.brieflyai.BrieflyAI.exception.ResearchServiceException;
import com.brieflyai.BrieflyAI.model.dto.GeminiResponse;
import com.brieflyai.BrieflyAI.model.enums.ResearchOperation;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.brieflyai.BrieflyAI.model.dto.ResearchRequest;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;
import java.util.Optional;

@Service
public class ResearchService {

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
        validateRequest(researchRequest);
        
        try {
            String prompt = buildPrompt(researchRequest);
            Map<String, Object> requestBody = createRequestBody(prompt);
            
            String response = callGeminiApi(requestBody);
            return extractTextFromResponse(response);
            
        } catch (Exception e) {
            throw new ResearchServiceException("Failed to process research request", e);
        }
    }

        private void validateRequest(ResearchRequest researchRequest) {
        if (researchRequest == null) {
            throw new IllegalArgumentException("Research request cannot be null");
        }
        if (!StringUtils.hasText(researchRequest.getOperation())) {
            throw new IllegalArgumentException("Operation cannot be null or empty");
        }
        if (!StringUtils.hasText(researchRequest.getContent())) {
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
    return webClient.post()
    .uri(geminiUrl + geminiKey)
              .bodyValue(requestBody)
              .retrieve()
              .bodyToMono(String.class)
              .onErrorMap(WebClientResponseException.class, ex ->
              new ResearchServiceException("Gemini API call failed: " + ex.getMessage()))
              .block();
  }

    private String extractTextFromResponse(String response) {
        if (!StringUtils.hasText(response)) {
            return NO_CONTENT_FOUND;
        }

        try {
            GeminiResponse geminiResponse = objectMapper.readValue(response, GeminiResponse.class);

            return Optional.ofNullable(geminiResponse)
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

        } catch (Exception e) {
            throw new ResearchServiceException("Failed to parse response from Gemini API", e);
        }
    }

    private String buildPrompt(ResearchRequest researchRequest) {
    try {
        ResearchOperation operation = ResearchOperation.fromString(researchRequest.getOperation());
        return operation.getPromptTemplate() + "\n\n" + researchRequest.getContent();
    } catch (IllegalArgumentException e) {
        throw new ResearchServiceException("Invalid operation: " + researchRequest.getOperation());
    }
  }
}
