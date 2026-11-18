package ru.hits.bugtrack.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.hits.bugtrack.dto.ApiDtos.ApiError;
import ru.hits.bugtrack.service.*;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Component
public class ApiAccessFilter extends OncePerRequestFilter {
    private final SessionService sessions;
    private final ObjectMapper json;
    public ApiAccessFilter(SessionService sessions, ObjectMapper json) { this.sessions = sessions; this.json = json; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "same-origin");
        response.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; font-src 'self'; img-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'");
        if (!request.getRequestURI().startsWith("/api/")) { chain.doFilter(request, response); return; }
        response.setHeader("Cache-Control", "no-store");
        try {
            boolean mutation = !Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod());
            if (mutation && !sameOrigin(request)) throw ApiException.forbidden();
            boolean login = request.getRequestURI().equals("/api/auth/login") && request.getMethod().equals("POST");
            if (!login) {
                var session = sessions.resolve(sessions.cookieToken(request));
                request.setAttribute(SessionService.REQUEST_SESSION, session);
                if (mutation) {
                    String token = request.getHeader("X-CSRF-Token");
                    if (token == null || !MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                        session.getCsrfToken().getBytes(StandardCharsets.UTF_8))) throw ApiException.forbidden();
                }
            }
            chain.doFilter(request, response);
        } catch (ApiException e) {
            response.setStatus(e.getStatus().value());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            json.writeValue(response.getWriter(), new ApiError(e.getCode(), e.getMessage(), Map.of()));
        }
    }

    private boolean sameOrigin(HttpServletRequest request) {
        if ("cross-site".equals(request.getHeader("Sec-Fetch-Site"))) return false;
        String origin = request.getHeader("Origin");
        if (origin == null) return true;
        try {
            URI uri = URI.create(origin);
            return uri.getScheme().equalsIgnoreCase(request.getScheme())
                && uri.getRawAuthority().equalsIgnoreCase(request.getHeader("Host"));
        } catch (RuntimeException e) { return false; }
    }
}
