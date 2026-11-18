package ru.hits.bugtrack.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import ru.hits.bugtrack.dto.ApiDtos.*;
import ru.hits.bugtrack.service.*;
import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final SessionService sessions;
    private final boolean secure;
    public AuthController(AuthService auth, SessionService sessions, @Value("${bugtrack.cookie-secure}") boolean secure) {
        this.auth = auth; this.sessions = sessions; this.secure = secure;
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserView> login(@Valid @RequestBody LoginRequest data, HttpServletRequest request) {
        var result = auth.login(data, sessions.cookieToken(request));
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie(result.token(), Duration.ofHours(8))).body(result.user());
    }

    @GetMapping("/me")
    public UserView me(HttpServletRequest request) { return auth.view(sessions.requireSession(request)); }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        auth.logout(sessions.requireSession(request));
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO)).build();
    }

    private String cookie(String token, Duration maxAge) {
        return ResponseCookie.from(SessionService.COOKIE_NAME, token).httpOnly(true).secure(secure)
            .sameSite("Lax").path("/").maxAge(maxAge).build().toString();
    }
}
