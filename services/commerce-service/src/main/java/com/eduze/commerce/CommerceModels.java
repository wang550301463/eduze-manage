package com.eduze.commerce;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public final class CommerceModels {
    private CommerceModels() {}

    public record ProductInput(
            String id,
            @NotBlank String branchId,
            @NotBlank String type,
            @NotBlank String title,
            @NotBlank String description,
            long priceMinor,
            int stock,
            String courseId,
            int lessonUnits,
            boolean published,
            int version) {
        public ProductInput(
                String id,
                String branchId,
                String type,
                String title,
                String description,
                long priceMinor,
                int stock,
                String courseId,
                int lessonUnits,
                boolean published) {
            this(
                    id,
                    branchId,
                    type,
                    title,
                    description,
                    priceMinor,
                    stock,
                    courseId,
                    lessonUnits,
                    published,
                    0);
        }
    }

    public record Product(
            String id,
            String branchId,
            String type,
            String title,
            String description,
            long priceMinor,
            int stock,
            String courseId,
            int lessonUnits,
            boolean published,
            int version) {}

    public record OrderInput(
            @NotBlank String branchId,
            @NotBlank String productId,
            String studentId,
            int quantity,
            @NotBlank String idempotencyKey,
            String fulfillmentMethod,
            String recipientName,
            String recipientPhone,
            String address) {
        public OrderInput(
                String branchId,
                String productId,
                String studentId,
                int quantity,
                String idempotencyKey) {
            this(
                    branchId,
                    productId,
                    studentId,
                    quantity,
                    idempotencyKey,
                    "PICKUP",
                    null,
                    null,
                    null);
        }
    }

    public record Order(
            String id,
            String branchId,
            String userId,
            String productId,
            String studentId,
            String courseId,
            String productType,
            String productTitle,
            long unitMinor,
            int quantity,
            long totalMinor,
            int lessonUnits,
            String paymentStatus,
            String fulfillmentStatus,
            String trackingNumber,
            Instant createdAt,
            String fulfillmentMethod,
            String recipientName,
            String recipientPhone,
            String address) {}

    public record RefundInput(@NotBlank String idempotencyKey, @NotBlank String reason) {}

    public record Refund(
            String id, String orderId, long amountMinor, String status, String reason) {}

    public record FulfillmentInput(@NotBlank String status, String trackingNumber) {}

    public record AftersaleInput(@NotBlank String reason) {}

    public record AftersaleResolution(@NotBlank String status, @NotBlank String response) {}
}
