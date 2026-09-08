package com.eduze.teaching;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Published content is stored as immutable value snapshots, never mutable template references. */
public final class TeachingModels {
    private TeachingModels() {}

    public record Step(
            @NotBlank @Size(max = 200) String title, @NotBlank @Size(max = 10000) String content) {}

    public record TemplateInput(
            int version,
            @NotBlank @Size(max = 200) String title,
            @Min(0) @Max(99) int ageMin,
            @Min(0) @Max(99) int ageMax,
            @NotBlank @Size(max = 10000) String goals,
            @NotNull @Size(max = 10000) String materials,
            @Min(1) @Max(100) int expectedLessons,
            @NotEmpty @Size(max = 100) List<@Valid Step> steps,
            @NotNull @Size(max = 30) List<@Size(max = 60) String> tags,
            @NotNull @Size(max = 100) List<@NotBlank String> mediaIds) {}

    public record Template(
            String id, String ownerId, int version, TemplateInput content, Instant createdAt) {}

    public record TemplateVersion(
            String id, String templateId, int version, TemplateInput content, Instant createdAt) {}

    public record ResourceInput(
            int version,
            @NotBlank @Size(max = 200) String title,
            @Pattern(regexp = "IMAGE|PDF|VIDEO|PRESENTATION|AUDIO") String kind,
            @NotEmpty @Size(max = 100) List<String> mediaIds,
            @NotNull @Size(max = 30) List<String> tags,
            @NotNull @Size(max = 10000) String description) {}

    public record Resource(
            String id,
            String ownerId,
            int version,
            boolean published,
            ResourceInput content,
            Instant createdAt) {}

    public record ThemeInput(
            int version,
            @NotBlank String branchId,
            @NotBlank String groupId,
            @NotBlank String templateVersionId,
            @NotBlank @Size(max = 200) String title,
            @NotNull @Size(max = 100) List<@NotBlank String> lessonIds,
            @NotNull @Size(max = 10000) String handoffNote) {}

    public record Theme(
            String id,
            int version,
            String status,
            ThemeInput content,
            TemplateInput template,
            Instant createdAt) {}

    public record PlanInput(
            @NotBlank String branchId,
            @NotBlank @Size(max = 200) String name,
            @NotNull LocalDate startsOn,
            @NotNull LocalDate endsOn,
            @NotNull @Size(max = 100) List<String> themeIds) {}

    public record Plan(
            String id,
            String branchId,
            String name,
            LocalDate startsOn,
            LocalDate endsOn,
            List<String> themeIds,
            Instant createdAt) {}

    public record Version(int version) {}

    public record Transition(
            int version,
            @Pattern(regexp = "PLANNED|IN_PROGRESS|FINISHED") String status,
            @NotNull @Size(max = 2000) String reason) {}

    public record Ids(@NotNull @Size(max = 100) List<String> ids) {}
}
