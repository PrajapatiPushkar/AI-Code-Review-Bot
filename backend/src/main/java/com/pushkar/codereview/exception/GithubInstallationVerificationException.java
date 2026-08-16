package com.pushkar.codereview.exception;

public class GithubInstallationVerificationException extends RuntimeException {

    public GithubInstallationVerificationException(String message) {
        super(message);
    }

    public GithubInstallationVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
