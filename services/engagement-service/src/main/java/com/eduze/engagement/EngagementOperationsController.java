package com.eduze.engagement;

import com.eduze.platform.runtime.*;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/engagement")
public class EngagementOperationsController {
    private final EngagementOperations operations;

    public EngagementOperationsController(EngagementOperations operations) {
        this.operations = operations;
    }

    @GetMapping("/signups")
    public ApiResponse<?> mine() {
        return ApiResponse.ok(operations.signups(Actors.current()));
    }

    @GetMapping("/activities/{id}/signups")
    public ApiResponse<?> signups(@PathVariable String id) {
        return ApiResponse.ok(operations.activitySignups(Actors.current(), id));
    }

    @PostMapping("/signups/{id}/checkin")
    public ApiResponse<?> checkin(@PathVariable String id) {
        operations.checkin(Actors.current(), id);
        return ApiResponse.ok(null);
    }

    @PutMapping("/renewals/{id}")
    public ApiResponse<?> renewal(
            @PathVariable String id,
            @jakarta.validation.Valid @RequestBody EngagementModels.RenewalInput input) {
        operations.updateRenewal(Actors.current(), id, input);
        return ApiResponse.ok(null);
    }

    @GetMapping("/dashboard")
    public ApiResponse<?> dashboard(@RequestParam String branchId) {
        return ApiResponse.ok(operations.dashboard(Actors.current(), branchId));
    }

    @GetMapping("/referrals")
    public ApiResponse<?> referrals(@RequestParam String branchId) {
        return ApiResponse.ok(operations.referrals(Actors.current(), branchId));
    }

    @PostMapping("/referrals/{enquiryId}/fulfill")
    public ApiResponse<?> fulfill(
            @PathVariable String enquiryId,
            @jakarta.validation.Valid @RequestBody Map<String, String> input) {
        operations.fulfillReferral(Actors.current(), enquiryId, input.get("note"));
        return ApiResponse.ok(null);
    }
}
