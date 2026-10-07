package com.pushkar.codereview.github.review.dto;

import jakarta.validation.constraints.Size;

public class CodeFixRequest {

    @Size(max = 500, message = "Developer instructions must not exceed 500 characters")
    private String instructions;

    public CodeFixRequest() {
    }

    public CodeFixRequest(String instructions) {
        this.instructions = instructions;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }
}
