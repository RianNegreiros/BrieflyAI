package com.brieflyai.BrieflyAI.service;

import com.brieflyai.BrieflyAI.exception.ResearchServiceException;
import com.brieflyai.BrieflyAI.model.dto.ResearchRequest;
import com.brieflyai.BrieflyAI.model.enums.ResearchOperation;
import com.google.genai.types.GenerateContentConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ResearchServiceTest {

    @Mock
    private GenerateContentConfig config;

    @InjectMocks
    private ResearchService researchService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testBuildPromptWithSummarize() {
        ResearchRequest request = new ResearchRequest(
                "Test content",
                "summarize",
                "test-api-key-12345678901234567890");

        String prompt = researchService.buildPrompt(request);
        assertTrue(prompt.contains(ResearchOperation.SUMMARIZE.getPromptTemplate()));
        assertTrue(prompt.contains("Test content"));
    }

    @Test
    void testBuildPromptWithSuggest() {
        ResearchRequest request = new ResearchRequest(
                "Test content",
                "suggest",
                "test-api-key-12345678901234567890");

        String prompt = researchService.buildPrompt(request);
        assertTrue(prompt.contains(ResearchOperation.SUGGEST.getPromptTemplate()));
        assertTrue(prompt.contains("Test content"));
    }

    @Test
    void testBuildPromptWithInvalidOperation() {
        ResearchRequest request = new ResearchRequest(
                "Test content",
                "invalid",
                "test-api-key-12345678901234567890");

        assertThrows(ResearchServiceException.class, () -> {
            researchService.buildPrompt(request);
        });
    }

    @Test
    void testProcessContentWithInvalidOperation() {
        ResearchRequest invalidRequest = new ResearchRequest(
                "Test content",
                "invalid",
                "test-api-key-12345678901234567890");

        assertThrows(ResearchServiceException.class, () -> {
            researchService.processContent(invalidRequest);
        });
    }
}
