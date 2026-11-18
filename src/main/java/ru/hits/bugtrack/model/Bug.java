package ru.hits.bugtrack.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "bugs")
public class Bug {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false)
    private int number;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private Member author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private Member reviewer;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(nullable = false, columnDefinition = "text")
    private String steps;

    @Column(nullable = false, columnDefinition = "text")
    private String expected;

    @Column(nullable = false, columnDefinition = "text")
    private String actual;

    @Column(columnDefinition = "text")
    private String environment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BugStatus status;

    @Version
    private long revision;

    @Column(nullable = false)
    private Instant createdAt;

    protected Bug() {}

    public Bug(UUID id, Project project, int number, Member author, Member reviewer, String title, String description, String steps, String expected, String actual, String environment, Severity severity, Priority priority, BugStatus status, long revision, Instant createdAt) {
        this.id = id;
        this.project = project;
        this.number = number;
        this.author = author;
        this.reviewer = reviewer;
        this.title = title;
        this.description = description;
        this.steps = steps;
        this.expected = expected;
        this.actual = actual;
        this.environment = environment;
        this.severity = severity;
        this.priority = priority;
        this.status = status;
        this.revision = revision;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public Project getProject() { return project; }
    public int getNumber() { return number; }
    public Member getAuthor() { return author; }
    public Member getReviewer() { return reviewer; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getSteps() { return steps; }
    public String getExpected() { return expected; }
    public String getActual() { return actual; }
    public String getEnvironment() { return environment; }
    public Severity getSeverity() { return severity; }
    public Priority getPriority() { return priority; }
    public BugStatus getStatus() { return status; }
    public long getRevision() { return revision; }
    public Instant getCreatedAt() { return createdAt; }
}
