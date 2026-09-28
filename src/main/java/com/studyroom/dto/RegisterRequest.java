package com.studyroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Pattern(regexp = "^[a-zA-Z0-9_]{4,32}$") String username,
        @NotBlank @Size(min = 8, max = 72) String password,
        @Size(max = 32) String nickname
) {
}
