package com.brieflyai.BrieflyAI.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import com.brieflyai.BrieflyAI.model.dto.ResearchRequest;
import com.brieflyai.BrieflyAI.model.enums.ResearchOperation;

@ExtendWith(MockitoExtension.class)
class ResearchServiceTest {

    @InjectMocks
    private ResearchService researchService;

    @Test
    void testResearchOperationSummarizeHasPromptTemplate() {
        ResearchOperation op = ResearchOperation.fromString("summarize");
        assertNotNull(op.getPromptTemplate());
        assertFalse(op.getPromptTemplate().isBlank());
    }

    @Test
    void testResearchOperationSuggestHasPromptTemplate() {
        ResearchOperation op = ResearchOperation.fromString("suggest");
        assertNotNull(op.getPromptTemplate());
        assertFalse(op.getPromptTemplate().isBlank());
    }

    @Test
    void testResearchOperationFromStringIsCaseInsensitive() {
        assertEquals(ResearchOperation.SUMMARIZE, ResearchOperation.fromString("SUMMARIZE"));
        assertEquals(ResearchOperation.SUGGEST, ResearchOperation.fromString("Suggest"));
    }

    @Test
    void testResearchOperationFromStringThrowsOnInvalid() {
        assertThrows(IllegalArgumentException.class, () -> ResearchOperation.fromString("invalid"));
    }

    @Test
    void testProcessContentWithInvalidOperation() {
        ResearchRequest invalidRequest =
                new ResearchRequest("Test content", "invalid", "test-api-key-12345678901234567890");

        assertThrows(IllegalArgumentException.class,
                () -> researchService.processContent(invalidRequest));
    }
}
