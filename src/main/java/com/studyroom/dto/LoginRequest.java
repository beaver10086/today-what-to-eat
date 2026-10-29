package com.studyroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
        @NotBlank @Pattern(regexp = "^[a-zA-Z0-9_]{4,32}$", message = "用户名格式无效") String username,
        @NotBlank String password) {
}
