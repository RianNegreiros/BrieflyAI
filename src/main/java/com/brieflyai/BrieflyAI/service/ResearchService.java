package com.brieflyai.BrieflyAI.service;

import com.brieflyai.BrieflyAI.model.dto.GeminiResponse;
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
    StringBuilder prompt = new StringBuilder();

    switch (researchRequest.getOperation()) {
      case "summarize":
        prompt.append("You are an expert summarizer. Summarize the following article into a clear, concise overview that captures the main points and key details without personal opinion.");
        break;
      case "suggest":
        prompt.append("""
                Based solely on the article below, suggest 3 diverse resources or actions for further exploration. Include: \s
                1. One reputable source for deeper understanding (include URL if possible). \s
                2. One thought-provoking question challenging the article's premise. \s
                3. One practical next step (e.g., tool, activity, or related concept).\s
                 \
                Avoid rewriting the entire article. Keep suggestions actionable.""");
        break;
      default:
        throw new IllegalArgumentException("Unknown operation");
    }
    prompt.append(researchRequest.getContent());
    return prompt.toString();
  }
}
