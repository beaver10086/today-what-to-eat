package com.studyroom.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

public record CanteenRequest(
        @NotBlank @Size(max = 50) String canteenName,
        @NotBlank @Size(max = 50) String campus,
        @Size(max = 100) String location,
        LocalTime openTime,
        LocalTime closeTime,
        @Size(max = 500) String description,
        @Size(max = 255) String coverUrl,
        Integer sortOrder,
        @Min(0) @Max(1) Integer status
) {
}
