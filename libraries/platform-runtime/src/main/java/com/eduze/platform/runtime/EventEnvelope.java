package com.eduze.platform.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record EventEnvelope(
        @NotBlank String eventId,
        @NotBlank String type,
        @Min(1) int version,
        @NotBlank String tenantId,
        String branchId,
        @NotBlank String aggregateId,
        @NotNull Instant occurredAt,
        String traceId,
        @NotNull JsonNode payload) {}
