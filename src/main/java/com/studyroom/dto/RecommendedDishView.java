package com.studyroom.dto;

import java.math.BigDecimal;

public record RecommendedDishView(DishView dish, BigDecimal score, String reason) {
}
