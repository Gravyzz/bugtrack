package ru.hits.bugtrack.service;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hits.bugtrack.dto.ApiDtos.*;
import ru.hits.bugtrack.model.*;
import ru.hits.bugtrack.repository.*;
import java.time.Instant;
import java.util.*;

@Service
public class BugService {
    private final BugRepository bugs;
    private final ProjectRepository projects;
    private final HistoryEntryRepository history;
    private final ProjectService access;

    public BugService(BugRepository bugs, ProjectRepository projects,
                      HistoryEntryRepository history, ProjectService access) {
        this.bugs = bugs;
        this.projects = projects;
        this.history = history;
        this.access = access;
    }

    @Transactional
    public BugView create(UUID actorId, UUID projectId, CreateBugRequest data) {
        Project project = projects.lockById(projectId).orElseThrow(ApiException::notFound);
        Member actor = access.requireMember(actorId, projectId);
        if (!actor.canCreateBug()) throw ApiException.forbidden();
        Instant now = Instant.now();
        Bug bug = new Bug(UUID.randomUUID(), project, project.allocateBugNumber(), actor,
            actor.canReview() ? actor : null, data.title().strip(), data.description().strip(),
            data.steps().strip(), data.expected().strip(), data.actual().strip(),
            data.environment() == null ? null : data.environment().strip(),
            data.severity(), data.priority(), BugStatus.NEW, 0, now);
        bugs.save(bug);
        history.save(new HistoryEntry(UUID.randomUUID(), bug, actor, now, "CREATED", "status", null, "NEW", null));
        return view(bug);
    }

    @Transactional(readOnly = true)
    public BugView get(UUID actorId, UUID bugId) { return view(requireBug(actorId, bugId)); }

    @Transactional(readOnly = true)
    public List<HistoryView> history(UUID actorId, UUID bugId) {
        requireBug(actorId, bugId);
        return history.findByBugIdOrderByCreatedAtAsc(bugId).stream().map(h -> new HistoryView(
            h.getId(), h.getActor().getUser().getName(), h.getCreatedAt(), h.getAction(),
            h.getField(), h.getBefore(), h.getAfter(), h.getComment())).toList();
    }

    @Transactional(readOnly = true)
    public BugPage list(UUID actorId, UUID projectId, String query, BugStatus status, Priority priority, int page) {
        access.requireMember(actorId, projectId);
        if (page < 0 || page > 100000) throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "VALIDATION", "Некорректная страница.");
        Specification<Bug> spec = (root, q, cb) -> cb.equal(root.get("project").get("id"), projectId);
        if (status != null) spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        if (priority != null) spec = spec.and((root, q, cb) -> cb.equal(root.get("priority"), priority));
        if (query != null && !query.isBlank()) {
            if (query.length() > 200) throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "VALIDATION", "Поисковая строка слишком длинная.");
            String text = query.strip().toLowerCase(Locale.ROOT);
            String escaped = text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            spec = spec.and((root, q, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), "%" + escaped + "%", '\\'),
                cb.equal(cb.lower(cb.concat(cb.concat(root.get("project").get("key"), "-"), root.get("number").as(String.class))), text)));
        }
        var result = bugs.findAll(spec, PageRequest.of(page, 25, Sort.by(Sort.Direction.DESC, "number")));
        return new BugPage(result.getContent().stream().map(this::view).toList(), result.getTotalElements(), page, result.getTotalPages());
    }

    private Bug requireBug(UUID actorId, UUID bugId) {
        Bug bug = bugs.findById(bugId).orElseThrow(ApiException::notFound);
        access.requireMember(actorId, bug.getProject().getId());
        return bug;
    }

    private BugView view(Bug b) {
        return new BugView(b.getId(), b.getProject().getId(), b.getProject().getKey() + "-" + b.getNumber(),
            b.getTitle(), b.getDescription(), b.getSteps(), b.getExpected(), b.getActual(), b.getEnvironment(),
            b.getSeverity(), b.getPriority(), b.getStatus(), b.getAuthor().getUser().getName(),
            b.getReviewer() == null ? null : b.getReviewer().getUser().getName(), b.getCreatedAt(), b.getRevision());
    }
}
