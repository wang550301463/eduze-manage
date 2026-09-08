package com.eduze.portfolio;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class PortfolioModels {
    private PortfolioModels() {}

    public record Artwork(
            @NotBlank String id,
            @NotBlank @Size(max = 200) String title,
            @NotNull @Pattern(regexp = "PROCESS|FINAL") String kind,
            @NotEmpty @Size(max = 50) List<@NotBlank String> mediaIds,
            @NotEmpty @Size(max = 100) List<@NotBlank String> participantIds,
            @NotNull @Size(max = 10000) String story) {}

    public record DraftInput(
            int version,
            @NotBlank String themeId,
            @NotBlank String studentId,
            @NotNull @Pattern(regexp = "NOT_STARTED|IN_PROGRESS|MAKEUP_PENDING|COMPLETED")
                    String progress,
            @NotNull @Size(max = 10000) String classroomNote,
            @NotNull @Size(max = 10000) String comment,
            @NotNull @Size(max = 100) List<@Valid Artwork> artworks,
            @NotNull @Size(max = 30) List<@NotBlank String> audioMediaIds) {}

    public record StudentRecord(
            String id,
            int version,
            String branchId,
            String status,
            DraftInput content,
            Instant createdAt,
            boolean needsPublishing) {
        public StudentRecord(
                String id,
                int version,
                String branchId,
                String status,
                DraftInput content,
                Instant createdAt) {
            this(id, version, branchId, status, content, createdAt, false);
        }
    }

    public record EntryInput(
            @NotBlank String lessonId,
            @NotNull Instant occurredAt,
            @NotBlank @Size(max = 10000) String notes,
            @NotNull @Size(max = 100) List<String> mediaIds,
            @NotBlank @Size(max = 100) String idempotencyKey) {}

    public record Entry(
            String id,
            String recordId,
            String lessonId,
            Instant occurredAt,
            String notes,
            List<String> mediaIds,
            String status,
            Instant createdAt) {}

    public record EntryPublish(@NotBlank @Size(max = 100) String idempotencyKey) {}

    public record EntryWithdraw(@NotBlank @Size(max = 2000) String reason) {}

    public record Publication(
            String id, String recordId, int version, DraftInput content, Instant createdAt) {}

    public record Publish(int version, @NotBlank @Size(max = 100) String idempotencyKey) {}

    public record Withdraw(int version, @NotBlank @Size(max = 2000) String reason) {}

    public record ReportInput(
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 10000) String summary,
            @NotEmpty @Size(max = 100) List<String> publicationIds) {}

    public record Report(
            String id,
            String studentId,
            String title,
            String summary,
            List<Publication> publications,
            Instant createdAt) {}

    public record AssessmentInput(
            @NotBlank String stageId,
            @NotNull LocalDate assessedAt,
            @NotNull @Size(max = 30) Map<String, @Min(1) @Max(5) Integer> scores,
            @Size(max = 1024) String comment) {}

    public record Assessment(
            String id,
            String studentId,
            String stageId,
            String stageCode,
            String stageName,
            LocalDate assessedAt,
            String assessedBy,
            Map<String, Integer> scores,
            String comment,
            Instant createdAt) {}

    public record Consent(boolean allowed, @NotBlank @Size(max = 100) String displayName) {}

    public record ExhibitionInput(
            @NotBlank @Size(max = 200) String title,
            @NotNull @Size(max = 10000) String introduction,
            @NotEmpty @Size(max = 100) List<String> publicationIds,
            Instant startsAt,
            Instant endsAt) {
        public ExhibitionInput(String title, String introduction, List<String> publicationIds) {
            this(title, introduction, publicationIds, null, null);
        }
    }

    public record Exhibition(
            String id,
            String title,
            String introduction,
            String status,
            Instant createdAt,
            Instant startsAt,
            Instant endsAt) {
        public Exhibition(
                String id, String title, String introduction, String status, Instant createdAt) {
            this(id, title, introduction, status, createdAt, null, null);
        }
    }

    public record MediaIds(@NotNull @Size(max = 100) List<String> mediaIds) {}
}
