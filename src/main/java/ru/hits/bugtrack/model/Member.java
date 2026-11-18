package ru.hits.bugtrack.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "members")
public class Member {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false)
    private boolean active;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "member_roles", joinColumns = @JoinColumn(name = "member_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20)
    private Set<Role> roles;

    protected Member() {}

    public Member(UUID id, User user, Project project, boolean active, Set<Role> roles) {
        this.id = id;
        this.user = user;
        this.project = project;
        this.active = active;
        this.roles = Set.copyOf(roles);
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public Project getProject() { return project; }
    public boolean isActive() { return active; }
    public Set<Role> getRoles() { return Set.copyOf(roles); }

    public boolean canCreateBug() {
        return roles.contains(Role.MANAGER) || roles.contains(Role.TESTER) || roles.contains(Role.DEVELOPER);
    }
    public boolean canReview() { return roles.contains(Role.MANAGER) || roles.contains(Role.TESTER); }
}
