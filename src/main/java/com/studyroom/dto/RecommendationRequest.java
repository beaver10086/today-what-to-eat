package com.studyroom.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RecommendationRequest(@NotNull @Min(1) @Max(4) Integer mealType,
                                    @Min(1) @Max(20) Integer limit,
                                    @Min(1) Long excludeRecommendationId,
                                    BigDecimal budgetMax) {
}
