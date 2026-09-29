package com.studyroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;

public record AssistantRequest(@NotBlank @Size(max = 500) String message, @Min(1) Integer page) {
}
