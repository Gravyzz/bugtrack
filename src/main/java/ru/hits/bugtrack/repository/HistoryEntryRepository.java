package ru.hits.bugtrack.repository;

import ru.hits.bugtrack.model.HistoryEntry;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;

public interface HistoryEntryRepository extends JpaRepository<HistoryEntry, UUID> {
    @EntityGraph(attributePaths = "actor.user")
    List<HistoryEntry> findByBugIdOrderByCreatedAtAsc(UUID bugId);
}
