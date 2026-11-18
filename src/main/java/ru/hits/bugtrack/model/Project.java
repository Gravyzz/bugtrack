package ru.hits.bugtrack.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "projects")
public class Project {
    @Id
    private UUID id;

    @Column(name = "project_key", nullable = false, unique = true, length = 10)
    private String key;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(length = 2048)
    private String repository;

    @Column(nullable = false)
    private int nextBugNumber;

    protected Project() {}

    public Project(UUID id, String key, String name, String description, String repository, int nextBugNumber) {
        this.id = id;
        this.key = key;
        this.name = name;
        this.description = description;
        this.repository = repository;
        this.nextBugNumber = nextBugNumber;
    }

    public UUID getId() { return id; }
    public String getKey() { return key; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getRepository() { return repository; }
    public int getNextBugNumber() { return nextBugNumber; }

    public int allocateBugNumber() { return nextBugNumber++; }
}
