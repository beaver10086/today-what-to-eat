package com.studyroom.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record DishRequest(
        @NotNull @Min(1) Long shopId,
        @NotBlank @Size(max = 60) String dishName,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        @NotNull @Min(1) @Max(7) Integer category,
        @Min(1) @Max(15) Integer mealType,
        @Min(0) @Max(3) Integer spiceLevel,
        @Min(0) Integer calorie,
        @Size(max = 300) String description,
        @Size(max = 255) String imageUrl,
        @Min(0) @Max(1) Integer isSignature,
        @Min(0) @Max(1) Integer isAvailable
) {
}
