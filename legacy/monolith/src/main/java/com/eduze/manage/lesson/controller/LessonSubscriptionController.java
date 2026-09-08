package com.eduze.manage.lesson.controller;

import com.eduze.manage.common.web.ApiResponse;
import com.eduze.manage.lesson.dto.SubscriptionRequest;
import com.eduze.manage.lesson.dto.SubscriptionResponse;
import com.eduze.manage.lesson.service.LessonSubscriptionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class LessonSubscriptionController {

    private final LessonSubscriptionService service;

    @GetMapping
    @PreAuthorize("hasAuthority('subscription:read')")
    public ApiResponse<List<SubscriptionResponse>> list(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long teacherId) {
        return ApiResponse.ok(service.list(studentId, teacherId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('subscription:write')")
    public ApiResponse<SubscriptionResponse> create(@Valid @RequestBody SubscriptionRequest req) {
        return ApiResponse.ok(service.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('subscription:write')")
    public ApiResponse<SubscriptionResponse> update(
            @PathVariable Long id, @Valid @RequestBody SubscriptionRequest req) {
        return ApiResponse.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('subscription:write')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }
}
