package com.studyroom.dto;

public record AuthView(String token, Long userId, String username, String nickname, Integer role) {
}
