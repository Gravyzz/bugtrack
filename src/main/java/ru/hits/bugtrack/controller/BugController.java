package ru.hits.bugtrack.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import ru.hits.bugtrack.dto.ApiDtos.*;
import ru.hits.bugtrack.model.*;
import ru.hits.bugtrack.service.*;
import java.net.URI;
import java.util.*;

@RestController
@RequestMapping("/api")
public class BugController {
    private final BugService bugs;
    private final SessionService sessions;
    public BugController(BugService bugs, SessionService sessions) { this.bugs = bugs; this.sessions = sessions; }

    @PostMapping("/projects/{projectId}/bugs")
    public ResponseEntity<BugView> create(@PathVariable UUID projectId, @Valid @RequestBody CreateBugRequest data, HttpServletRequest request) {
        var bug = bugs.create(sessions.requireUser(request), projectId, data);
        return ResponseEntity.created(URI.create("/api/bugs/" + bug.id())).body(bug);
    }

    @GetMapping("/projects/{projectId}/bugs")
    public BugPage list(@PathVariable UUID projectId, @RequestParam(defaultValue = "") String query,
                        @RequestParam(required = false) BugStatus status, @RequestParam(required = false) Priority priority,
                        @RequestParam(defaultValue = "0") int page, HttpServletRequest request) {
        return bugs.list(sessions.requireUser(request), projectId, query, status, priority, page);
    }

    @GetMapping("/bugs/{bugId}")
    public BugView get(@PathVariable UUID bugId, HttpServletRequest request) { return bugs.get(sessions.requireUser(request), bugId); }

    @GetMapping("/bugs/{bugId}/history")
    public List<HistoryView> history(@PathVariable UUID bugId, HttpServletRequest request) {
        return bugs.history(sessions.requireUser(request), bugId);
    }
}
