package com.studyroom.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record PreferenceRequest(
        @NotNull @Min(0) @Max(3) Integer maxSpiceLevel,
        @DecimalMin("0.00") BigDecimal budgetMin,
        @DecimalMin("0.00") BigDecimal budgetMax,
        @NotNull @Size(max = 40) List<@NotNull @Min(1) Long> likeTagIds,
        @NotNull @Size(max = 40) List<@NotNull @Min(1) Long> dislikeTagIds,
        @NotNull @Min(0) @Max(3) Integer dietType
) {
}
