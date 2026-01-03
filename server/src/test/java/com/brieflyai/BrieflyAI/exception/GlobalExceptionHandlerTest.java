package com.brieflyai.BrieflyAI.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.brieflyai.BrieflyAI.model.dto.ErrorResponse;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler exceptionHandler;

    @Test
    void testHandleServiceException() {
        ResearchServiceException ex = new ResearchServiceException("Test service exception");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleServiceException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getBody().status());
        assertTrue(response.getBody().message().contains("Test service exception"));
    }

    @Test
    void testHandleIllegalArgumentException() {
        IllegalArgumentException ex = new IllegalArgumentException("Test illegal argument");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgumentException(ex);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().status());
        assertTrue(response.getBody().message().contains("Test illegal argument"));
    }

    @Test
    void testHandleGenericException() {
        Exception ex = new Exception("Test generic exception");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getBody().status());
        assertTrue(response.getBody().message().contains("An unexpected error occurred"));
    }

    @Test
    void testHandleApiKeyException() {
        ApiKeyException ex = new ApiKeyException("Invalid API key");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleApiKeyException(ex);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getBody().status());
        assertTrue(response.getBody().message().contains("Invalid API key"));
    }
}
