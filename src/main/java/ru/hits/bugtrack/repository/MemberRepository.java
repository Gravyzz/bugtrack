package ru.hits.bugtrack.repository;

import ru.hits.bugtrack.model.Member;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;

public interface MemberRepository extends JpaRepository<Member, UUID> {
    @EntityGraph(attributePaths = {"user", "project", "roles"})
    Optional<Member> findByUserIdAndProjectIdAndActiveTrue(UUID userId, UUID projectId);

    @EntityGraph(attributePaths = {"user", "project", "roles"})
    List<Member> findByUserIdAndActiveTrueOrderByProjectNameAsc(UUID userId);

    @EntityGraph(attributePaths = {"user", "roles"})
    List<Member> findByProjectIdAndActiveTrueOrderByUserNameAsc(UUID projectId);
}
