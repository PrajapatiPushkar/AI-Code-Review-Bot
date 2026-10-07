package com.pushkar.codereview.github.review.dto;

import com.pushkar.codereview.github.review.fix.CodeFixProposalStatus;
import jakarta.validation.constraints.NotNull;

public class CodeFixProposalStatusRequest {

    @NotNull(message = "Status is required")
    private CodeFixProposalStatus status;

    public CodeFixProposalStatusRequest() {
    }

    public CodeFixProposalStatusRequest(CodeFixProposalStatus status) {
        this.status = status;
    }

    public CodeFixProposalStatus getStatus() {
        return status;
    }

    public void setStatus(CodeFixProposalStatus status) {
        this.status = status;
    }
}
