package com.eduze.platform.notification;

import com.eduze.platform.runtime.ApiResponse;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<NotificationService.Message>> list() {
        return ApiResponse.ok(service.list());
    }

    @PostMapping("/{id}/read")
    public ApiResponse<NotificationService.Message> read(@PathVariable String id) {
        return ApiResponse.ok(service.mark(id, false));
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<NotificationService.Message> confirm(@PathVariable String id) {
        return ApiResponse.ok(service.mark(id, true));
    }

    @GetMapping("/subscriptions")
    public ApiResponse<List<NotificationService.Subscription>> subscriptions() {
        return ApiResponse.ok(service.subscriptions());
    }

    @PutMapping("/subscriptions")
    public ApiResponse<NotificationService.Subscription> subscribe(
            @RequestBody NotificationService.Subscription body) {
        return ApiResponse.ok(service.subscribe(body));
    }

    @GetMapping("/jobs")
    public ApiResponse<List<Map<String, Object>>> jobs() {
        return ApiResponse.ok(service.jobs());
    }

    @PostMapping("/jobs/{id}/retry")
    public ApiResponse<Map<String, Boolean>> retry(@PathVariable String id) {
        service.retry(id);
        return ApiResponse.ok(Map.of("queued", true));
    }
}
