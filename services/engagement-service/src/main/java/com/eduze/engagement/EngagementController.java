package com.eduze.engagement;

import static com.eduze.engagement.EngagementModels.*;

import com.eduze.platform.runtime.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/engagement")
public class EngagementController {
    private final EngagementService service;

    public EngagementController(EngagementService service) {
        this.service = service;
    }

    @GetMapping("/public/studios")
    public ApiResponse<java.util.List<Studio>> studios() {
        return ApiResponse.ok(service.studios());
    }

    @GetMapping("/public/studios/{id}")
    public ApiResponse<Studio> studio(@PathVariable String id) {
        return ApiResponse.ok(service.studio(id));
    }

    @PostMapping("/studios")
    public ApiResponse<Studio> saveStudio(
            @jakarta.validation.Valid @RequestBody StudioInput input) {
        return ApiResponse.ok(service.saveStudio(Actors.current(), input));
    }

    @PostMapping("/public/studios/{id}/enquiries")
    public ApiResponse<Map<String, String>> enquire(
            @PathVariable String id, @jakarta.validation.Valid @RequestBody EnquiryInput input) {
        var result = service.enquire(id, input);
        return ApiResponse.ok(Map.of("id", result.id(), "status", result.status()));
    }

    @GetMapping("/enquiries")
    public ApiResponse<java.util.List<Enquiry>> enquiries(@RequestParam String branchId) {
        return ApiResponse.ok(service.enquiries(Actors.current(), branchId));
    }

    @GetMapping("/enquiries/{id}/followups")
    public ApiResponse<java.util.List<Map<String, Object>>> history(@PathVariable String id) {
        return ApiResponse.ok(service.history(Actors.current(), id));
    }

    @PostMapping("/enquiries/{id}/followups")
    public ApiResponse<Void> followup(
            @PathVariable String id, @jakarta.validation.Valid @RequestBody FollowupInput input) {
        service.followup(Actors.current(), id, input);
        return ApiResponse.ok(null);
    }

    @PostMapping("/enquiries/{id}/trial")
    public ApiResponse<Map<String, Object>> trial(
            @PathVariable String id, @jakarta.validation.Valid @RequestBody TrialInput input) {
        return ApiResponse.ok(service.reserveTrial(Actors.current(), id, input));
    }

    @PostMapping("/activities")
    public ApiResponse<Activity> activity(
            @jakarta.validation.Valid @RequestBody ActivityInput input) {
        return ApiResponse.ok(service.saveActivity(Actors.current(), input));
    }

    @GetMapping("/public/activities")
    public ApiResponse<java.util.List<Activity>> activities(@RequestParam String branchId) {
        return ApiResponse.ok(service.activities(branchId));
    }

    @PostMapping("/activities/{id}/signups")
    public ApiResponse<Signup> signup(@PathVariable String id) {
        return ApiResponse.ok(service.signup(Actors.current(), id));
    }

    @PostMapping("/signups/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable String id) {
        service.cancelSignup(Actors.current(), id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/renewals")
    public ApiResponse<java.util.List<Map<String, Object>>> renewals(
            @RequestParam String branchId) {
        return ApiResponse.ok(service.renewals(Actors.current(), branchId));
    }

    @PostMapping("/renewals")
    public ApiResponse<Map<String, String>> renewal(
            @jakarta.validation.Valid @RequestBody RenewalInput input) {
        return ApiResponse.ok(Map.of("id", service.renewal(Actors.current(), input)));
    }

    @PostMapping("/enquiries/{id}/enroll")
    public ApiResponse<Map<String, Object>> enroll(
            @PathVariable String id,
            @jakarta.validation.Valid @RequestBody Map<String, String> input) {
        return ApiResponse.ok(service.enroll(Actors.current(), id, input.get("classGroupId")));
    }
}
