package com.pushkar.codereview.github.review.fix;

import com.pushkar.codereview.github.review.dto.CodeFixResponse;

public interface AiFixEngine {

    CodeFixResponse generateFix(FixGenerationInput input);
}
