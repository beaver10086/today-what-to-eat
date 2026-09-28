package com.studyroom.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FeedbackRequest(@NotNull @Min(1) Long dishId,
                              @NotNull @Min(1) @Max(4) Integer feedbackType,
                              @Size(max = 50) String reasonTag,
                              @Size(max = 255) String comment) {
}
