package ru.hits.bugtrack.repository;

import ru.hits.bugtrack.model.Bug;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;

public interface BugRepository extends JpaRepository<Bug, UUID>, JpaSpecificationExecutor<Bug> {
    @EntityGraph(attributePaths = {"project", "author.user", "reviewer.user"})
    Optional<Bug> findById(UUID id);
}
