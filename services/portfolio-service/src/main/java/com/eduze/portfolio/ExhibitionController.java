package com.eduze.portfolio;

import static com.eduze.portfolio.PortfolioModels.*;

import com.eduze.platform.runtime.Actors;
import com.eduze.platform.runtime.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/portfolio")
public class ExhibitionController {
    private final ExhibitionService service;

    public ExhibitionController(ExhibitionService service) {
        this.service = service;
    }

    @PostMapping("/publications/{id}/consent")
    public ApiResponse<?> consent(@PathVariable String id, @Valid @RequestBody Consent input) {
        return ApiResponse.ok(service.consent(Actors.current(), id, input));
    }

    @GetMapping("/exhibitions")
    public ApiResponse<?> list() {
        return ApiResponse.ok(service.list(Actors.current()));
    }

    @PostMapping("/exhibitions")
    public ApiResponse<?> create(@Valid @RequestBody ExhibitionInput input) {
        return ApiResponse.ok(service.create(Actors.current(), input));
    }

    @PostMapping("/exhibitions/{id}/approve")
    public ApiResponse<?> approve(@PathVariable String id) {
        return ApiResponse.ok(service.approve(Actors.current(), id));
    }

    @PostMapping("/exhibitions/{id}/withdraw")
    public ApiResponse<?> withdraw(@PathVariable String id) {
        return ApiResponse.ok(service.withdraw(Actors.current(), id));
    }

    @GetMapping("/public/exhibitions")
    public ApiResponse<?> publicList() {
        return ApiResponse.ok(service.publicList());
    }

    @GetMapping("/public/exhibitions/{id}")
    public ApiResponse<?> publicDetail(@PathVariable String id) {
        return ApiResponse.ok(service.publicDetail(id));
    }

    @GetMapping("/public/exhibitions/{id}/publications/{publicationId}/media-access")
    public ApiResponse<?> publicMedia(
            @PathVariable String id,
            @PathVariable String publicationId,
            @RequestParam java.util.List<String> mediaIds) {
        return ApiResponse.ok(service.publicMedia(id, publicationId, mediaIds));
    }

    @PostMapping("/exhibitions/{id}/archive")
    public ApiResponse<?> archive(@PathVariable String id) {
        return ApiResponse.ok(service.archive(Actors.current(), id));
    }

    @GetMapping("/public/exhibitions/{id}/publications/{publicationId}/certificate")
    public ApiResponse<?> certificate(@PathVariable String id, @PathVariable String publicationId) {
        return ApiResponse.ok(service.certificate(id, publicationId));
    }

    @GetMapping("/public/exhibitions/{id}/share-card")
    public ApiResponse<?> shareCard(@PathVariable String id) {
        return ApiResponse.ok(service.shareCard(id));
    }
}
