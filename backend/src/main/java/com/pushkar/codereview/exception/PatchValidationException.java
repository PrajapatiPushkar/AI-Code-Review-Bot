package com.pushkar.codereview.exception;

public class PatchValidationException extends IllegalArgumentException {

    public PatchValidationException(String message) {
        super(message);
    }

    public PatchValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
