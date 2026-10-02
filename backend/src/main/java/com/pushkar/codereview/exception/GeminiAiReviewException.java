package com.pushkar.codereview.exception;

public class GeminiAiReviewException extends RuntimeException {

    private final Integer statusCode;

    public GeminiAiReviewException(String message) {
        super(message);
        this.statusCode = null;
    }

    public GeminiAiReviewException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
    }

    public GeminiAiReviewException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public GeminiAiReviewException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}
