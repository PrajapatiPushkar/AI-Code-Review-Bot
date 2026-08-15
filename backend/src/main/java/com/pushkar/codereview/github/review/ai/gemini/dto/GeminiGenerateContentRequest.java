package com.pushkar.codereview.github.review.ai.gemini.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class GeminiGenerateContentRequest {

    @JsonProperty("contents")
    private List<Content> contents;

    public GeminiGenerateContentRequest() {
    }

    public GeminiGenerateContentRequest(List<Content> contents) {
        this.contents = contents;
    }

    public static GeminiGenerateContentRequest fromText(String text) {
        return new GeminiGenerateContentRequest(List.of(new Content(List.of(new Part(text)))));
    }

    public List<Content> getContents() {
        return contents;
    }

    public void setContents(List<Content> contents) {
        this.contents = contents;
    }

    public static class Content {

        @JsonProperty("parts")
        private List<Part> parts;

        public Content() {
        }

        public Content(List<Part> parts) {
            this.parts = parts;
        }

        public List<Part> getParts() {
            return parts;
        }

        public void setParts(List<Part> parts) {
            this.parts = parts;
        }
    }

    public static class Part {

        @JsonProperty("text")
        private String text;

        public Part() {
        }

        public Part(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
