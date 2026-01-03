package com.brieflyai.BrieflyAI.model.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResearchOperationTest {

  @Test
  void testFromStringWithValidSummarize() {
    ResearchOperation operation = ResearchOperation.fromString("summarize");
    assertEquals(ResearchOperation.SUMMARIZE, operation);
  }

  @Test
  void testFromStringWithValidSuggest() {
    ResearchOperation operation = ResearchOperation.fromString("suggest");
    assertEquals(ResearchOperation.SUGGEST, operation);
  }

  @Test
  void testFromStringWithCaseInsensitive() {
    ResearchOperation operation1 = ResearchOperation.fromString("SUMMARIZE");
    ResearchOperation operation2 = ResearchOperation.fromString("SuGgEsT");

    assertEquals(ResearchOperation.SUMMARIZE, operation1);
    assertEquals(ResearchOperation.SUGGEST, operation2);
  }

  @Test
  void testFromStringWithInvalidOperation() {
    assertThrows(IllegalArgumentException.class, () -> {
      ResearchOperation.fromString("invalid");
    });
  }

  @Test
  void testGetOperation() {
    assertEquals("summarize", ResearchOperation.SUMMARIZE.getOperation());
    assertEquals("suggest", ResearchOperation.SUGGEST.getOperation());
  }

  @Test
  void testGetPromptTemplate() {
    assertNotNull(ResearchOperation.SUMMARIZE.getPromptTemplate());
    assertNotNull(ResearchOperation.SUGGEST.getPromptTemplate());
    assertTrue(ResearchOperation.SUMMARIZE.getPromptTemplate().length() > 0);
    assertTrue(ResearchOperation.SUGGEST.getPromptTemplate().length() > 0);
  }
}
