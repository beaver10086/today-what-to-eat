package com.studyroom.config;

import com.studyroom.common.AuthUser;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class AuthTokenService {
    private static final long TOKEN_LIFETIME_SECONDS = 12 * 60 * 60;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public String issue(AuthUser user) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.put(token, new Session(user, Instant.now().plusSeconds(TOKEN_LIFETIME_SECONDS)));
        return token;
    }

    public AuthUser resolve(String token) {
        Session session = sessions.get(token);
        if (session == null) {
            return null;
        }
        if (session.expiresAt().isBefore(Instant.now())) {
            sessions.remove(token);
            return null;
        }
        return session.user();
    }

    public void revoke(String token) {
        sessions.remove(token);
    }

    private record Session(AuthUser user, Instant expiresAt) {
    }
}
