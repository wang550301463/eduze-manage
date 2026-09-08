package com.eduze.manage.family;

import com.eduze.platform.runtime.ApiResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/academic/family")
@RequiredArgsConstructor
public class FamilyController {
    private final FamilyService families;

    public record InviteRequest(String studentId) {}

    public record ClaimRequest(String code) {}

    public record LeaveRequest(String lessonId, String reason) {}

    @PostMapping("/invites")
    public ApiResponse<FamilyService.Invite> invite(@RequestBody InviteRequest request) {
        return ApiResponse.ok(families.invite(request.studentId()));
    }

    @PostMapping("/bindings")
    public ApiResponse<FamilyService.Binding> claim(@RequestBody ClaimRequest request) {
        return ApiResponse.ok(families.claim(request.code()));
    }

    @GetMapping("/bindings")
    public ApiResponse<List<FamilyService.Binding>> bindings(@RequestParam String studentId) {
        return ApiResponse.ok(families.bindings(studentId));
    }

    @PostMapping("/bindings/{id}/approve")
    public ApiResponse<FamilyService.Binding> approve(@PathVariable String id) {
        return ApiResponse.ok(families.approve(id));
    }

    @DeleteMapping("/bindings/{id}")
    public ApiResponse<FamilyService.Binding> revoke(@PathVariable String id) {
        return ApiResponse.ok(families.revoke(id));
    }

    @GetMapping("/children")
    public ApiResponse<List<AcademicAccess.StudentView>> children() {
        return ApiResponse.ok(families.children());
    }

    @GetMapping("/children/{id}/schedule")
    public ApiResponse<List<FamilyService.Schedule>> schedule(
            @PathVariable String id, @RequestParam LocalDate from, @RequestParam LocalDate to) {
        return ApiResponse.ok(families.schedule(id, from, to));
    }

    @GetMapping("/children/{id}/balance")
    public ApiResponse<Map<String, Object>> balance(@PathVariable String id) {
        return ApiResponse.ok(families.balance(id));
    }

    @GetMapping("/children/{id}/leaves")
    public ApiResponse<List<FamilyService.Leave>> leaves(@PathVariable String id) {
        return ApiResponse.ok(families.leaves(id));
    }

    @PostMapping("/children/{id}/leaves")
    public ApiResponse<FamilyService.Leave> leave(
            @PathVariable String id,
            @RequestBody LeaveRequest request,
            @RequestHeader("Idempotency-Key") String key) {
        return ApiResponse.ok(families.leave(id, request.lessonId(), request.reason(), key));
    }
}
