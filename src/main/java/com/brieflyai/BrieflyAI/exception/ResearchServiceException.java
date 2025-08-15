package com.brieflyai.BrieflyAI.exception;

public class ResearchServiceException extends RuntimeException {
    public ResearchServiceException(String message) {
        super(message);
    }
    
    public ResearchServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
