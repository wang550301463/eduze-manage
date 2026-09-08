package com.eduze.teaching;

import com.eduze.platform.runtime.Actors;
import com.eduze.platform.runtime.ApiResponse;
import org.springframework.web.bind.annotation.*;

@RestController
public class CurriculumController {
    private final CurriculumService service;

    public CurriculumController(CurriculumService service) {
        this.service = service;
    }

    @GetMapping({"/api/curriculum/stages", "/api/v1/teaching/stages"})
    public ApiResponse<?> stages() {
        Actors.current().requirePermission("course:read");
        return ApiResponse.ok(service.stages(Actors.current()));
    }

    @GetMapping({"/api/curriculum/dimensions", "/api/v1/teaching/dimensions"})
    public ApiResponse<?> dimensions(@RequestParam(required = false) String kind) {
        Actors.current().requirePermission("course:read");
        return ApiResponse.ok(service.dimensions(Actors.current(), kind));
    }
}
