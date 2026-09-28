package com.studyroom.common;

public record AuthUser(Long id, String username, Integer role, String nickname) {
    public static final String REQUEST_ATTRIBUTE = AuthUser.class.getName();

    public boolean isAdmin() {
        return role != null && role == 2;
    }
}
