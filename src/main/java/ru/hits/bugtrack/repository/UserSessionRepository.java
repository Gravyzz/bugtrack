package ru.hits.bugtrack.repository;

import ru.hits.bugtrack.model.UserSession;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    @EntityGraph(attributePaths = "user")
    Optional<UserSession> findByTokenHashAndExpiresAtAfter(String tokenHash, Instant now);
    void deleteByTokenHash(String tokenHash);
    void deleteByExpiresAtBefore(Instant now);
}
