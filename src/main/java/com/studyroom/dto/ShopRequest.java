package com.studyroom.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalTime;

public record ShopRequest(
        @NotNull @Min(1) Long canteenId,
        @NotBlank @Size(max = 50) String shopName,
        @Size(max = 100) String locationDesc,
        LocalTime openTime,
        LocalTime closeTime,
        @Size(max = 30) String cuisine,
        @DecimalMin("0.00") BigDecimal avgPrice,
        @Min(0) @Max(3) Integer queueHeat,
        @Size(max = 255) String coverUrl,
        @Size(max = 500) String description,
        @Min(0) @Max(1) Integer status,
        Integer sortOrder
) {
}
