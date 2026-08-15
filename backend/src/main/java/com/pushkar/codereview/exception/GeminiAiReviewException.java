package com.pushkar.codereview.exception;

public class GeminiAiReviewException extends RuntimeException {

    public GeminiAiReviewException(String message) {
        super(message);
    }

    public GeminiAiReviewException(String message, Throwable cause) {
        super(message, cause);
    }
}
