package com.studyroom.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TagRequest(
        @NotBlank @Size(max = 30) String tagName,
        @NotNull @Min(1) @Max(5) Integer tagType,
        Integer sortOrder
) {
}
