package com.eduze.platform.media;

import com.eduze.platform.runtime.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
public class MediaController {
    private final MediaService service;
    private final InternalCredentials credentials;
    private final MediaCleanupReview cleanupReview;

    public MediaController(
            MediaService service,
            InternalCredentials credentials,
            MediaCleanupReview cleanupReview) {
        this.service = service;
        this.credentials = credentials;
        this.cleanupReview = cleanupReview;
    }

    @PostMapping("/api/v1/media/uploads")
    public ApiResponse<MediaService.UploadResult> upload(
            @Valid @RequestBody MediaService.UploadRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @PostMapping("/api/v1/media/uploads/{id}/complete")
    public ApiResponse<Map<String, String>> complete(@PathVariable String id) {
        return ApiResponse.ok(service.complete(id));
    }

    @GetMapping("/internal/media/{id}/usable")
    public MediaService.Usable usable(@PathVariable String id) {
        return service.usable(id);
    }

    @PostMapping("/internal/media/references")
    public Map<String, Boolean> references(
            @Valid @RequestBody MediaService.References body, HttpServletRequest request) {
        service.references(credentials.require(request), body);
        return Map.of("saved", true);
    }

    @PostMapping("/internal/media/access")
    public MediaService.Links access(
            @Valid @RequestBody MediaService.Access body, HttpServletRequest request) {
        return service.access(credentials.require(request), body);
    }

    @PostMapping("/internal/media/public-access")
    public MediaService.Links publicAccess(
            @Valid @RequestBody MediaService.PublicAccess body, HttpServletRequest request) {
        return service.publicAccess(credentials.require(request), body);
    }

    @GetMapping("/api/v1/media/cleanup/candidates")
    public ApiResponse<java.util.List<MediaCleanupReview.Candidate>> candidates() {
        return ApiResponse.ok(cleanupReview.candidates());
    }

    @PostMapping("/api/v1/media/cleanup/{id}/review")
    public ApiResponse<MediaCleanupReview.Result> review(
            @PathVariable String id, @Valid @RequestBody MediaCleanupReview.Review body) {
        return ApiResponse.ok(cleanupReview.review(id, body));
    }
}
