package com.pushkar.codereview.github.review.dto;

/**
 * Encapsulates exported source file or patch content and its safe filename.
 */
public record FileExportContent(String filename, String content) {
}
