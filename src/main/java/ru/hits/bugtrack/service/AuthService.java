package ru.hits.bugtrack.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hits.bugtrack.dto.ApiDtos.*;
import ru.hits.bugtrack.model.*;
import ru.hits.bugtrack.repository.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

@Service
public class AuthService {
    private final UserRepository users;
    private final UserSessionRepository sessions;
    private final SessionService sessionService;
    private final PasswordEncoder passwords;
    private final String dummyHash;

    public AuthService(UserRepository users, UserSessionRepository sessions,
                       SessionService sessionService, PasswordEncoder passwords) {
        this.users = users;
        this.sessions = sessions;
        this.sessionService = sessionService;
        this.passwords = passwords;
        this.dummyHash = passwords.encode(UUID.randomUUID().toString());
    }

    public record LoginResult(String token, UserView user) {}

    @Transactional
    public LoginResult login(LoginRequest data, String previousToken) {
        String email = data.email().strip().toLowerCase(Locale.ROOT);
        User user = users.findByEmail(email).orElse(null);
        boolean validLength = data.password().getBytes(StandardCharsets.UTF_8).length <= 72;
        boolean matches = passwords.matches(validLength ? data.password() : "", user == null ? dummyHash : user.getPasswordHash());
        if (user == null || !validLength || !matches) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Неверная почта или пароль.");
        }
        if (previousToken != null) sessions.deleteByTokenHash(sessionService.hash(previousToken));
        sessions.deleteByExpiresAtBefore(Instant.now());
        String token = sessionService.newToken();
        UserSession session = new UserSession(UUID.randomUUID(), user, sessionService.hash(token),
            sessionService.newToken(), Instant.now().plus(Duration.ofHours(8)));
        sessions.save(session);
        return new LoginResult(token, view(session));
    }

    @Transactional
    public void logout(UserSession session) { sessions.deleteById(session.getId()); }

    public UserView view(UserSession session) {
        return new UserView(session.getUser().getId(), session.getUser().getName(),
            session.getUser().getEmail(), session.getCsrfToken());
    }
}
