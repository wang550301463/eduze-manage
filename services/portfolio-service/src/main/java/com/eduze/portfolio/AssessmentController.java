package com.eduze.portfolio;

import com.eduze.platform.runtime.Actors;
import com.eduze.platform.runtime.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
public class AssessmentController {
    private final PortfolioService service;

    public AssessmentController(PortfolioService service) {
        this.service = service;
    }

    @GetMapping({
        "/api/students/{id}/stage-assessments",
        "/api/v1/portfolio/students/{id}/assessments"
    })
    public ApiResponse<?> list(@PathVariable String id) {
        return ApiResponse.ok(service.assessments(Actors.current(), id));
    }

    @PostMapping({
        "/api/students/{id}/stage-assessments",
        "/api/v1/portfolio/students/{id}/assessments"
    })
    public ApiResponse<?> create(
            @PathVariable String id, @Valid @RequestBody PortfolioModels.AssessmentInput input) {
        return ApiResponse.ok(service.assess(Actors.current(), id, input));
    }
}
