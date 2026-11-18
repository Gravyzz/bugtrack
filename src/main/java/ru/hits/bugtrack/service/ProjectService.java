package ru.hits.bugtrack.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hits.bugtrack.dto.ApiDtos.*;
import ru.hits.bugtrack.model.Member;
import ru.hits.bugtrack.repository.MemberRepository;
import java.util.*;

@Service
public class ProjectService {
    private final MemberRepository members;
    public ProjectService(MemberRepository members) { this.members = members; }

    @Transactional(readOnly = true)
    public Member requireMember(UUID userId, UUID projectId) {
        return members.findByUserIdAndProjectIdAndActiveTrue(userId, projectId)
            .orElseThrow(ApiException::notFound);
    }

    @Transactional(readOnly = true)
    public List<ProjectView> listMine(UUID userId) {
        return members.findByUserIdAndActiveTrueOrderByProjectNameAsc(userId).stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public ProjectView get(UUID userId, UUID projectId) { return view(requireMember(userId, projectId)); }

    private ProjectView view(Member membership) {
        var project = membership.getProject();
        var team = members.findByProjectIdAndActiveTrueOrderByUserNameAsc(project.getId()).stream()
            .map(m -> new MemberView(m.getId(), m.getUser().getName(), m.getRoles())).toList();
        return new ProjectView(project.getId(), project.getKey(), project.getName(), project.getDescription(),
            membership.getRoles(), membership.canCreateBug(), team);
    }
}
