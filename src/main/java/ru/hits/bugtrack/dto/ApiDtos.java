package ru.hits.bugtrack.dto;

import jakarta.validation.constraints.*;
import ru.hits.bugtrack.model.*;
import java.time.Instant;
import java.util.*;

public final class ApiDtos {
    private ApiDtos() {}

    public record LoginRequest(
        @NotBlank(message = "Введите email") @Email(message = "Проверьте email") @Size(max = 254) String email,
        @NotBlank(message = "Введите пароль") @Size(max = 128) String password) {}

    public record CreateBugRequest(
        @NotBlank(message = "Введите заголовок бага") @Size(max = 200, message = "Не больше 200 символов") String title,
        @NotBlank(message = "Введите описание") @Size(max = 10000, message = "Не больше 10000 символов") String description,
        @NotBlank(message = "Укажите шаги воспроизведения") @Size(max = 10000, message = "Не больше 10000 символов") String steps,
        @NotBlank(message = "Укажите ожидаемый результат") @Size(max = 5000, message = "Не больше 5000 символов") String expected,
        @NotBlank(message = "Укажите фактический результат") @Size(max = 5000, message = "Не больше 5000 символов") String actual,
        @Size(max = 2000, message = "Не больше 2000 символов") String environment,
        @NotNull(message = "Выберите серьёзность") Severity severity,
        @NotNull(message = "Выберите приоритет") Priority priority) {}

    public record UserView(UUID id, String name, String email, String csrfToken) {}
    public record MemberView(UUID id, String name, Set<Role> roles) {}
    public record ProjectView(UUID id, String key, String name, String description,
                              Set<Role> myRoles, boolean canCreateBug, List<MemberView> members) {}
    public record BugView(UUID id, UUID projectId, String code, String title, String description,
                          String steps, String expected, String actual, String environment,
                          Severity severity, Priority priority, BugStatus status,
                          String author, String reviewer, Instant createdAt, long revision) {}
    public record BugPage(List<BugView> items, long total, int page, int totalPages) {}
    public record HistoryView(UUID id, String actor, Instant at, String action,
                              String field, String before, String after, String comment) {}
    public record ApiError(String code, String message, Map<String, String> fields) {}
}
