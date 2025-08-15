package com.brieflyai.BrieflyAI.service;

import com.brieflyai.BrieflyAI.model.dto.GeminiResponse;
import com.brieflyai.BrieflyAI.model.enums.ResearchOperation;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.brieflyai.BrieflyAI.model.dto.ResearchRequest;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class ResearchService {
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
    String prompt = buildPrompt(researchRequest);

      Map<String, Object> requestBody = Map.of(
              "contents", new Object[] {
                      Map.of("parts", new Object[] {
                              Map.of("text", prompt)
                      })
      }
              );
      
      String response = webClient.post()
              .uri(geminiUrl + geminiKey)
              .bodyValue(requestBody)
              .retrieve()
              .bodyToMono(String.class)
              .block();
      
    return extractTextFromResponse(response);
  }

    private String extractTextFromResponse(String response) {
        try {
            GeminiResponse geminiResponse = objectMapper.readValue(response, GeminiResponse.class);
            if (geminiResponse.getCandidates() != null && !geminiResponse.getCandidates().isEmpty()) {
                GeminiResponse.Candidate firstCandidate = geminiResponse.getCandidates().get(0);
                if (firstCandidate.getContent() != null && firstCandidate.getContent().getParts() != null && !firstCandidate.getContent().getParts().isEmpty()) {
                    return firstCandidate.getContent().getParts().get(0).getText();
                }
            }
            
            return "No content found";
        } catch (Exception e) {
            return "Error Parsing: " + e.getMessage();
        }
    }

    private String buildPrompt(ResearchRequest researchRequest) {
    try {
        ResearchOperation operation = ResearchOperation.fromString(researchRequest.getOperation());
        return operation.getPromptTemplate() + "\n\n" + researchRequest.getContent();
    } catch (Exception e) {
        return "Failed to build prompt" + e.getMessage();
    }
  }
}
