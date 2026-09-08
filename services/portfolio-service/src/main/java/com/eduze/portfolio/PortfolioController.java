package com.eduze.portfolio;

import static com.eduze.portfolio.PortfolioModels.*;

import com.eduze.platform.runtime.Actors;
import com.eduze.platform.runtime.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/portfolio")
public class PortfolioController {
    private final PortfolioService service;

    public PortfolioController(PortfolioService service) {
        this.service = service;
    }

    @GetMapping("/records")
    public ApiResponse<?> records(
            @RequestParam(required = false) String themeId,
            @RequestParam(required = false) String studentId,
            @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.ok(service.records(Actors.current(), themeId, studentId, limit));
    }

    @PostMapping("/records")
    public ApiResponse<?> create(
            @Valid @RequestBody DraftInput input,
            @RequestParam(required = false) String sourceRecordId) {
        return ApiResponse.ok(service.create(Actors.current(), input, sourceRecordId));
    }

    @GetMapping("/records/{id}")
    public ApiResponse<?> record(@PathVariable String id) {
        return ApiResponse.ok(service.record(Actors.current(), id));
    }

    @PutMapping("/records/{id}")
    public ApiResponse<?> update(
            @PathVariable String id,
            @Valid @RequestBody DraftInput input,
            @RequestParam(required = false) String sourceRecordId) {
        return ApiResponse.ok(service.update(Actors.current(), id, input, sourceRecordId));
    }

    @PostMapping("/records/{id}/publish")
    public ApiResponse<?> publish(@PathVariable String id, @Valid @RequestBody Publish input) {
        return ApiResponse.ok(
                service.publish(Actors.current(), id, input.version(), input.idempotencyKey()));
    }

    @PostMapping("/records/{id}/withdraw")
    public ApiResponse<?> withdraw(@PathVariable String id, @Valid @RequestBody Withdraw input) {
        return ApiResponse.ok(
                service.withdraw(Actors.current(), id, input.version(), input.reason()));
    }

    @GetMapping("/themes/{id}/roster")
    public ApiResponse<?> roster(@PathVariable String id) {
        return ApiResponse.ok(service.roster(Actors.current(), id));
    }

    @GetMapping("/dashboard")
    public ApiResponse<?> dashboard(@RequestParam(required = false) String branchId) {
        return ApiResponse.ok(service.dashboard(Actors.current(), branchId));
    }

    @PostMapping("/records/{id}/media-access")
    public ApiResponse<?> media(@PathVariable String id, @Valid @RequestBody MediaIds input) {
        return ApiResponse.ok(service.mediaAccess(Actors.current(), id, input.mediaIds()));
    }

    @GetMapping("/students/{id}/growth")
    public ApiResponse<?> growth(@PathVariable String id) {
        return ApiResponse.ok(service.growth(Actors.current(), id));
    }

    @PostMapping("/students/{id}/reports")
    public ApiResponse<?> report(@PathVariable String id, @Valid @RequestBody ReportInput input) {
        return ApiResponse.ok(service.report(Actors.current(), id, input));
    }

    @PostMapping("/publications/{id}/media-access")
    public ApiResponse<?> publicationMedia(
            @PathVariable String id, @Valid @RequestBody MediaIds input) {
        return ApiResponse.ok(service.publicationMedia(Actors.current(), id, input.mediaIds()));
    }

    @PostMapping("/reports/{id}/publications/{publicationId}/media-access")
    public ApiResponse<?> reportMedia(
            @PathVariable String id,
            @PathVariable String publicationId,
            @Valid @RequestBody MediaIds input) {
        return ApiResponse.ok(
                service.reportMedia(Actors.current(), id, publicationId, input.mediaIds()));
    }

    @GetMapping("/students/{id}/collections")
    public ApiResponse<?> collections(@PathVariable String id) {
        return ApiResponse.ok(service.collections(Actors.current(), id));
    }

    @PostMapping("/students/{id}/collections")
    public ApiResponse<?> collection(
            @PathVariable String id, @Valid @RequestBody ReportInput input) {
        return ApiResponse.ok(service.createCollection(Actors.current(), id, input));
    }

    @GetMapping("/records/{id}/entries")
    public ApiResponse<?> entries(@PathVariable String id) {
        return ApiResponse.ok(service.entries(Actors.current(), id));
    }

    @GetMapping("/students/{id}/entries")
    public ApiResponse<?> studentEntries(@PathVariable String id) {
        return ApiResponse.ok(service.studentEntries(Actors.current(), id));
    }

    @PostMapping("/records/{id}/entries")
    public ApiResponse<?> addEntry(@PathVariable String id, @Valid @RequestBody EntryInput input) {
        return ApiResponse.ok(service.addEntry(Actors.current(), id, input));
    }

    @PostMapping("/records/{id}/entries/{entryId}/publish")
    public ApiResponse<?> publishEntry(
            @PathVariable String id,
            @PathVariable String entryId,
            @Valid @RequestBody EntryPublish input) {
        return ApiResponse.ok(
                service.publishEntry(Actors.current(), id, entryId, input.idempotencyKey()));
    }

    @PostMapping("/records/{id}/entries/{entryId}/withdraw")
    public ApiResponse<?> withdrawEntry(
            @PathVariable String id,
            @PathVariable String entryId,
            @Valid @RequestBody EntryWithdraw input) {
        return ApiResponse.ok(service.withdrawEntry(Actors.current(), id, entryId, input.reason()));
    }

    @PostMapping("/records/{id}/entries/{entryId}/media-access")
    public ApiResponse<?> entryMedia(
            @PathVariable String id,
            @PathVariable String entryId,
            @Valid @RequestBody MediaIds input) {
        return ApiResponse.ok(service.entryMedia(Actors.current(), id, entryId, input.mediaIds()));
    }

    @GetMapping("/records/{id}/history")
    public ApiResponse<?> history(@PathVariable String id) {
        return ApiResponse.ok(service.history(Actors.current(), id));
    }
}
