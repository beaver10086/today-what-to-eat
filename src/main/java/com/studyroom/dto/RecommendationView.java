package com.studyroom.dto;

import java.math.BigDecimal;
import java.util.List;

public record RecommendationView(Long id, String requestNo, Integer mealType, BigDecimal budgetMax,
                                 Integer source, Integer isFallback, Integer costMs,
                                 List<RecommendedDishView> items) {
}
