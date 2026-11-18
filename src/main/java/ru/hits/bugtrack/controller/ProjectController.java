package ru.hits.bugtrack.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import ru.hits.bugtrack.dto.ApiDtos.*;
import ru.hits.bugtrack.service.*;
import java.util.*;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService projects;
    private final SessionService sessions;
    public ProjectController(ProjectService projects, SessionService sessions) { this.projects = projects; this.sessions = sessions; }

    @GetMapping
    public List<ProjectView> list(HttpServletRequest request) { return projects.listMine(sessions.requireUser(request)); }

    @GetMapping("/{projectId}")
    public ProjectView get(@PathVariable UUID projectId, HttpServletRequest request) {
        return projects.get(sessions.requireUser(request), projectId);
    }
}
