package com.studyroom.dto;

import java.math.BigDecimal;

public record SearchIntent(String rawQuery, BigDecimal budgetMax, Integer maxSpiceLevel,
                           String canteenKeyword, String shopKeyword, Integer mealType,
                           Integer category, String keyword, boolean nearby) {
}
