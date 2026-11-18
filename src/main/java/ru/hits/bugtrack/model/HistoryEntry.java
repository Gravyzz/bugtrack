package ru.hits.bugtrack.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "history_entries")
public class HistoryEntry {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bug_id")
    private Bug bug;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id")
    private Member actor;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "field_name", length = 100)
    private String field;

    @Column(name = "before_value", columnDefinition = "text")
    private String before;

    @Column(name = "after_value", columnDefinition = "text")
    private String after;

    @Column(columnDefinition = "text")
    private String comment;

    protected HistoryEntry() {}

    public HistoryEntry(UUID id, Bug bug, Member actor, Instant createdAt, String action, String field, String before, String after, String comment) {
        this.id = id;
        this.bug = bug;
        this.actor = actor;
        this.createdAt = createdAt;
        this.action = action;
        this.field = field;
        this.before = before;
        this.after = after;
        this.comment = comment;
    }

    public UUID getId() { return id; }
    public Bug getBug() { return bug; }
    public Member getActor() { return actor; }
    public Instant getCreatedAt() { return createdAt; }
    public String getAction() { return action; }
    public String getField() { return field; }
    public String getBefore() { return before; }
    public String getAfter() { return after; }
    public String getComment() { return comment; }
}
