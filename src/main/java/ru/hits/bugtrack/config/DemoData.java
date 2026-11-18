package ru.hits.bugtrack.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.hits.bugtrack.model.*;
import ru.hits.bugtrack.repository.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
@ConditionalOnProperty(name = "bugtrack.demo.enabled", havingValue = "true")
public class DemoData implements ApplicationRunner {
    public static final UUID SHOP_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private final UserRepository users;
    private final ProjectRepository projects;
    private final MemberRepository members;
    private final PasswordEncoder passwords;
    private final String demoPassword;

    public DemoData(UserRepository users, ProjectRepository projects, MemberRepository members,
                    PasswordEncoder passwords, @Value("${bugtrack.demo.password}") String demoPassword) {
        this.users = users; this.projects = projects; this.members = members;
        this.passwords = passwords; this.demoPassword = demoPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (demoPassword.length() < 12 || demoPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("DEMO_PASSWORD должен содержать от 12 символов и не больше 72 байт.");
        }
        Project shop = projects.findById(SHOP_ID).orElseGet(() -> projects.save(new Project(
            SHOP_ID, "SHOP", "Учебный интернет-магазин", "Учёт ошибок учебного интернет-магазина", null, 1)));
        add(shop, "Захар", "zakhar@example.org", Set.of(Role.MANAGER, Role.TESTER));
        add(shop, "Иван", "ivan@example.org", Set.of(Role.DEVELOPER));
        add(shop, "Преподаватель", "observer@example.org", Set.of(Role.OBSERVER));
    }

    private void add(Project project, String name, String email, Set<Role> roles) {
        User user = users.findByEmail(email).orElseGet(() -> users.save(new User(
            UUID.randomUUID(), name, email, passwords.encode(demoPassword))));
        if (members.findByUserIdAndProjectIdAndActiveTrue(user.getId(), project.getId()).isEmpty()) {
            if (members.findAll().stream().noneMatch(m -> m.getUser().getId().equals(user.getId()) && m.getProject().getId().equals(project.getId()))) {
                members.save(new Member(UUID.randomUUID(), user, project, true, roles));
            }
        }
    }
}
