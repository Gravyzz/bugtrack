package ru.hits.bugtrack.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hits.bugtrack.model.UserSession;
import ru.hits.bugtrack.repository.UserSessionRepository;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;

@Service
public class SessionService {
    public static final String COOKIE_NAME = "BUGTRACK_SESSION";
    public static final String REQUEST_SESSION = "bugtrack.session";
    private final UserSessionRepository sessions;
    private final SecureRandom random = new SecureRandom();

    public SessionService(UserSessionRepository sessions) { this.sessions = sessions; }

    @Transactional(readOnly = true)
    public UserSession resolve(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw ApiException.unauthorized();
        return sessions.findByTokenHashAndExpiresAtAfter(hash(token), Instant.now())
            .orElseThrow(ApiException::unauthorized);
    }

    public UserSession requireSession(HttpServletRequest request) {
        Object session = request.getAttribute(REQUEST_SESSION);
        if (session instanceof UserSession current) return current;
        throw ApiException.unauthorized();
    }

    public UUID requireUser(HttpServletRequest request) { return requireSession(request).getUser().getId(); }

    public String cookieToken(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    public String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
