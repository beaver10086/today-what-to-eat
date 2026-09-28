package com.studyroom.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record FavoriteRequest(@NotNull @Min(1) @Max(2) Integer targetType,
                              @NotNull @Min(1) Long targetId) {
}
