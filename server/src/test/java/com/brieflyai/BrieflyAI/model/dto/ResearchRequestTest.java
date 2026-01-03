package com.brieflyai.BrieflyAI.model.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ResearchRequestTest {

  private Validator validator;

  @BeforeEach
  void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  void testValidResearchRequest() {
    ResearchRequest request = new ResearchRequest(
        "This is a valid content with more than 50 characters to meet the validation requirement.",
        "summarize",
        "test-api-key-12345678901234567890");

    Set<ConstraintViolation<ResearchRequest>> violations = validator.validate(request);
    assertTrue(violations.isEmpty());
  }

  @Test
  void testInvalidContentTooShort() {
    ResearchRequest request = new ResearchRequest(
        "Short",
        "summarize",
        "test-api-key-12345678901234567890");

    Set<ConstraintViolation<ResearchRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
    assertTrue(
        violations.stream().anyMatch(v -> v.getMessage().contains("Content must be between 50 and 50000 characters")));
  }

  @Test
  void testInvalidContentTooLong() {
    String longContent = "a".repeat(50001);
    ResearchRequest request = new ResearchRequest(
        longContent,
        "summarize",
        "test-api-key-12345678901234567890");

    Set<ConstraintViolation<ResearchRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
    assertTrue(
        violations.stream().anyMatch(v -> v.getMessage().contains("Content must be between 50 and 50000 characters")));
  }

  @Test
  void testInvalidOperation() {
    ResearchRequest request = new ResearchRequest(
        "This is a valid content with more than 50 characters to meet the validation requirement.",
        "invalid",
        "test-api-key-12345678901234567890");

    Set<ConstraintViolation<ResearchRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
    assertTrue(
        violations.stream().anyMatch(v -> v.getMessage().contains("Operation must be 'summarize' or 'suggest'")));
  }

  @Test
  void testInvalidApiKeyTooShort() {
    ResearchRequest request = new ResearchRequest(
        "This is a valid content with more than 50 characters to meet the validation requirement.",
        "summarize",
        "short-key");

    Set<ConstraintViolation<ResearchRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Invalid API key format")));
  }

  @Test
  void testNullContent() {
    ResearchRequest request = new ResearchRequest(
        null,
        "summarize",
        "test-api-key-12345678901234567890");

    Set<ConstraintViolation<ResearchRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Content is required")));
  }

  @Test
  void testNullOperation() {
    ResearchRequest request = new ResearchRequest(
        "This is a valid content with more than 50 characters to meet the validation requirement.",
        null,
        "test-api-key-12345678901234567890");

    Set<ConstraintViolation<ResearchRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Operation is required")));
  }

  @Test
  void testNullApiKey() {
    ResearchRequest request = new ResearchRequest(
        "This is a valid content with more than 50 characters to meet the validation requirement.",
        "summarize",
        null);

    Set<ConstraintViolation<ResearchRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("API key is required")));
  }
}
