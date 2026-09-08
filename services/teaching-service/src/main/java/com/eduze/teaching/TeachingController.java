package com.eduze.teaching;

import static com.eduze.teaching.TeachingModels.*;

import com.eduze.platform.runtime.Actors;
import com.eduze.platform.runtime.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/teaching")
public class TeachingController {
    private final TeachingService service;

    public TeachingController(TeachingService service) {
        this.service = service;
    }

    @GetMapping("/templates")
    public ApiResponse<?> templates(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.ok(service.templates(Actors.current(), q, limit));
    }

    @PostMapping("/templates")
    public ApiResponse<?> create(@Valid @RequestBody TemplateInput input) {
        return ApiResponse.ok(service.createTemplate(Actors.current(), input));
    }

    @GetMapping("/templates/{id}")
    public ApiResponse<?> template(@PathVariable String id) {
        return ApiResponse.ok(service.template(Actors.current(), id));
    }

    @PutMapping("/templates/{id}")
    public ApiResponse<?> update(@PathVariable String id, @Valid @RequestBody TemplateInput input) {
        return ApiResponse.ok(service.updateTemplate(Actors.current(), id, input));
    }

    @PostMapping("/templates/{id}/publish")
    public ApiResponse<?> publish(@PathVariable String id, @RequestBody Version input) {
        return ApiResponse.ok(service.publishTemplate(Actors.current(), id, input.version()));
    }

    @GetMapping("/templates/{id}/versions")
    public ApiResponse<?> versions(@PathVariable String id) {
        return ApiResponse.ok(service.versions(Actors.current(), id, "", 100));
    }

    @GetMapping("/template-versions")
    public ApiResponse<?> versions(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.ok(service.versions(Actors.current(), null, q, limit));
    }

    @GetMapping("/resources")
    public ApiResponse<?> resources(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.ok(service.resources(Actors.current(), q, limit));
    }

    @PostMapping("/resources")
    public ApiResponse<?> create(@Valid @RequestBody ResourceInput input) {
        return ApiResponse.ok(service.createResource(Actors.current(), input));
    }

    @PutMapping("/resources/{id}")
    public ApiResponse<?> update(@PathVariable String id, @Valid @RequestBody ResourceInput input) {
        return ApiResponse.ok(service.updateResource(Actors.current(), id, input));
    }

    @PostMapping("/resources/{id}/publish")
    public ApiResponse<?> publishResource(@PathVariable String id, @RequestBody Version input) {
        return ApiResponse.ok(service.publishResource(Actors.current(), id, input.version()));
    }

    @GetMapping("/themes")
    public ApiResponse<?> themes(
            @RequestParam(required = false) String branchId,
            @RequestParam(required = false) String groupId,
            @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.ok(service.themes(Actors.current(), branchId, groupId, limit));
    }

    @PostMapping("/themes")
    public ApiResponse<?> create(@Valid @RequestBody ThemeInput input) {
        return ApiResponse.ok(service.createTheme(Actors.current(), input));
    }

    @GetMapping("/themes/{id}")
    public ApiResponse<?> theme(@PathVariable String id) {
        return ApiResponse.ok(service.theme(Actors.current(), id));
    }

    @PutMapping("/themes/{id}")
    public ApiResponse<?> update(@PathVariable String id, @Valid @RequestBody ThemeInput input) {
        return ApiResponse.ok(service.updateTheme(Actors.current(), id, input));
    }

    @PostMapping("/themes/{id}/transition")
    public ApiResponse<?> transition(
            @PathVariable String id, @Valid @RequestBody Transition input) {
        return ApiResponse.ok(
                service.transition(
                        Actors.current(), id, input.version(), input.status(), input.reason()));
    }

    @GetMapping("/themes/{id}/roster")
    public ApiResponse<?> roster(@PathVariable String id) {
        return ApiResponse.ok(service.roster(Actors.current(), id));
    }

    @GetMapping("/plans")
    public ApiResponse<?> plans(
            @RequestParam String branchId, @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.ok(service.plans(Actors.current(), branchId, limit));
    }

    @PostMapping("/plans")
    public ApiResponse<?> createPlan(@Valid @RequestBody PlanInput input) {
        return ApiResponse.ok(service.createPlan(Actors.current(), input));
    }

    @GetMapping("/dashboard")
    public ApiResponse<?> dashboard(@RequestParam(required = false) String branchId) {
        return ApiResponse.ok(service.dashboard(Actors.current(), branchId));
    }

    public record MediaIds(java.util.List<String> mediaIds) {}

    @PostMapping("/{type:resources|templates|template-versions}/{id}/media-access")
    public ApiResponse<?> media(
            @PathVariable String type, @PathVariable String id, @RequestBody MediaIds input) {
        return ApiResponse.ok(service.mediaAccess(Actors.current(), type, id, input.mediaIds()));
    }

    @PostMapping("/template-versions/{id}/copy")
    public ApiResponse<?> copyTemplate(@PathVariable String id) {
        return ApiResponse.ok(service.copyTemplate(Actors.current(), id));
    }

    @PostMapping("/resources/{id}/copy")
    public ApiResponse<?> copyResource(@PathVariable String id) {
        return ApiResponse.ok(service.copyResource(Actors.current(), id));
    }
}
