package com.pushkar.codereview.github.review.fix;

import com.pushkar.codereview.exception.PatchValidationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class PatchValidator {

    private static final Set<String> FORBIDDEN_FILE_PATTERNS = Set.of(
            ".env", "application.properties", "application.yml", "application.yaml",
            "id_rsa", "id_ed25519", "credentials", "secret", "passwd", "shadow", "master.key"
    );

    private static final List<String> DANGEROUS_COMMANDS = List.of(
            "rm -rf", "rm -r", "del /f", "format ", "drop database", "drop table",
            "git push", "git commit", "curl ", "wget ", "powershell", "/bin/sh", "/bin/bash"
    );

    public void validate(String expectedFilePath, String candidateFilePath, String unifiedDiff) {
        validateFilePath(expectedFilePath, candidateFilePath);
        validateDiffContent(unifiedDiff);
    }

    public void validateFilePath(String expectedFilePath, String candidateFilePath) {
        if (candidateFilePath == null || candidateFilePath.isBlank()) {
            throw new PatchValidationException("Patch candidate file path must not be empty");
        }

        String rawPath = candidateFilePath.trim();

        // 1. Path traversal checks
        if (rawPath.contains("../") || rawPath.contains("/..") || rawPath.contains("..\\") || rawPath.contains("\\..") || rawPath.equals("..")) {
            throw new PatchValidationException("Path traversal detected in patch candidate file path: " + candidateFilePath);
        }

        // 2. Absolute filesystem path checks (Unix / Linux root, Windows drive letter, UNC path)
        if (rawPath.startsWith("/") || rawPath.startsWith("\\") || rawPath.matches("^[a-zA-Z]:.*") || rawPath.startsWith("\\\\")) {
            throw new PatchValidationException("Absolute file path is not allowed in patch candidate: " + candidateFilePath);
        }

        // 3. Prohibited sensitive files & configuration checks
        String lower = rawPath.replace('\\', '/').toLowerCase();
        for (String forbidden : FORBIDDEN_FILE_PATTERNS) {
            if (lower.contains(forbidden)) {
                throw new PatchValidationException("Patch targets prohibited sensitive configuration or credential file: " + forbidden);
            }
        }

        // 4. Verification that candidate path matches the expected finding file path
        if (expectedFilePath != null && !expectedFilePath.isBlank()) {
            String normExpected = normalizePath(expectedFilePath);
            String normCandidate = normalizePath(candidateFilePath);
            if (!normExpected.equalsIgnoreCase(normCandidate)) {
                throw new PatchValidationException(String.format(
                        "Patch candidate file path '%s' does not match the target review finding file path '%s'",
                        candidateFilePath, expectedFilePath));
            }
        }
    }

    public void validateDiffContent(String unifiedDiff) {
        if (unifiedDiff == null || unifiedDiff.isBlank()) {
            throw new PatchValidationException("Generated patch diff must not be empty");
        }

        String lower = unifiedDiff.toLowerCase();
        for (String dangerous : DANGEROUS_COMMANDS) {
            if (lower.contains(dangerous)) {
                throw new PatchValidationException("Patch contains potentially malicious or destructive instruction: " + dangerous);
            }
        }

        // Must contain basic diff structure markers (@@ or +/- lines)
        boolean hasDiffMarkers = unifiedDiff.contains("@@") || unifiedDiff.contains("---") ||
                unifiedDiff.contains("+++") || unifiedDiff.contains("\n+") || unifiedDiff.contains("\n-") ||
                unifiedDiff.startsWith("+") || unifiedDiff.startsWith("-");
        if (!hasDiffMarkers) {
            throw new PatchValidationException("Generated patch does not contain valid unified diff syntax or markers");
        }
    }

    private String normalizePath(String path) {
        return path.replace('\\', '/').trim().replaceFirst("^/+", "");
    }
}
