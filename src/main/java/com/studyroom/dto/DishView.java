package com.studyroom.dto;

import java.math.BigDecimal;

public record DishView(Long id, Long shopId, String shopName, Long canteenId,
                       String canteenName, String dishName, BigDecimal price,
                       Integer category, Integer mealType, Integer spiceLevel,
                       Integer calorie, String description, String imageUrl,
                       Integer isSignature, Integer isAvailable, BigDecimal rating) {
}
