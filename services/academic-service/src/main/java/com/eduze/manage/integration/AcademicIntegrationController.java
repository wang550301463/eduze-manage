package com.eduze.manage.integration;

import com.eduze.platform.runtime.InternalCredentials;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/academic")
@RequiredArgsConstructor
public class AcademicIntegrationController {
    private final InternalCredentials credentials;
    private final TrialReservationService trials;
    private final CommerceEntitlements entitlements;

    @PostMapping("/trial-reservations")
    public TrialReservationService.State reserve(
            @RequestBody TrialReservationService.Request body, HttpServletRequest request) {
        credentials.requireCaller(request, "engagement");
        return trials.reserve(body);
    }

    @GetMapping("/trial-reservations/{id}")
    public TrialReservationService.State trial(
            @PathVariable String id, HttpServletRequest request) {
        credentials.requireCaller(request, "engagement");
        return trials.get(id);
    }

    @PostMapping("/trial-reservations/{id}/cancel")
    public TrialReservationService.State cancel(
            @PathVariable String id, HttpServletRequest request) {
        credentials.requireCaller(request, "engagement");
        return trials.cancel(id);
    }

    @PostMapping("/trial-reservations/{id}/enroll")
    public TrialReservationService.State enroll(
            @PathVariable String id,
            @RequestBody TrialReservationService.Enrollment body,
            HttpServletRequest request) {
        credentials.requireCaller(request, "engagement");
        return trials.enroll(id, body);
    }

    @PostMapping("/commerce/refunds/freeze")
    public CommerceEntitlements.RefundState freeze(
            @RequestBody CommerceEntitlements.RefundRequest body, HttpServletRequest request) {
        credentials.requireCaller(request, "commerce");
        return entitlements.freeze(body);
    }

    @PostMapping("/commerce/refunds/complete")
    public CommerceEntitlements.RefundState complete(
            @RequestBody CommerceEntitlements.RefundRequest body, HttpServletRequest request) {
        credentials.requireCaller(request, "commerce");
        return entitlements.complete(body);
    }

    @PostMapping("/commerce/refunds/release")
    public CommerceEntitlements.RefundState release(
            @RequestBody CommerceEntitlements.RefundRequest body, HttpServletRequest request) {
        credentials.requireCaller(request, "commerce");
        return entitlements.release(body);
    }

    @GetMapping("/commerce/refunds/{id}")
    public CommerceEntitlements.RefundState refund(
            @PathVariable String id, HttpServletRequest request) {
        credentials.requireCaller(request, "commerce");
        return entitlements.get(id);
    }
}
