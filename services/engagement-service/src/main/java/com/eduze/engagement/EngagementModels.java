package com.eduze.engagement;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public final class EngagementModels {
    private EngagementModels() {}

    public record StudioInput(
            @NotBlank String branchId,
            @NotBlank String name,
            @NotBlank String description,
            @NotBlank String address,
            @NotBlank String phone,
            boolean published) {}

    public record Studio(
            String branchId, String name, String description, String address, String phone) {}

    public record EnquiryInput(
            @NotBlank String studentName,
            @NotBlank String phone,
            int age,
            String source,
            String sourceId,
            String referrerId,
            String website) {}

    public record Enquiry(
            String id,
            String branchId,
            String studentName,
            String phone,
            int age,
            String source,
            String sourceId,
            String status,
            String assigneeId,
            String reservationId) {}

    public record FollowupInput(
            @NotBlank String status, String assigneeId, @NotBlank String note, Instant nextAt) {}

    public record TrialInput(@NotBlank String sessionId) {}

    public record ActivityInput(
            @NotBlank String branchId,
            @NotBlank String title,
            @NotBlank String description,
            @NotBlank String startsAt,
            int capacity,
            boolean published) {}

    public record Activity(
            String id,
            String branchId,
            String title,
            String description,
            Instant startsAt,
            int capacity,
            int reserved,
            boolean published) {}

    public record Signup(String id, String activityId, String userId, String status) {}

    public record RenewalInput(
            @NotBlank String branchId,
            @NotBlank String studentId,
            @NotBlank String assigneeId,
            @NotBlank String note,
            Instant nextAt,
            @NotBlank String status) {}
}
