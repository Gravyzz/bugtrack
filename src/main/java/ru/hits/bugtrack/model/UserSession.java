package ru.hits.bugtrack.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "user_sessions")
public class UserSession {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false, length = 64)
    private String csrfToken;

    @Column(nullable = false)
    private Instant expiresAt;

    protected UserSession() {}

    public UserSession(UUID id, User user, String tokenHash, String csrfToken, Instant expiresAt) {
        this.id = id;
        this.user = user;
        this.tokenHash = tokenHash;
        this.csrfToken = csrfToken;
        this.expiresAt = expiresAt;
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public String getTokenHash() { return tokenHash; }
    public String getCsrfToken() { return csrfToken; }
    public Instant getExpiresAt() { return expiresAt; }
}
