package com.eduze.teaching;

import com.eduze.platform.runtime.Actors;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/teaching")
public class TeachingInternalController {
    private final TeachingService service;
    private final CurriculumService curriculum;

    public TeachingInternalController(TeachingService service, CurriculumService curriculum) {
        this.service = service;
        this.curriculum = curriculum;
    }

    @GetMapping("/themes/{id}")
    public TeachingModels.Theme theme(@PathVariable String id) {
        return service.theme(Actors.current(), id);
    }

    @GetMapping("/stages/{id}")
    public Object stage(@PathVariable String id) {
        return curriculum.stage(Actors.current(), id);
    }

    @PostMapping("/stages/batch")
    public Object stages(@RequestBody TeachingModels.Ids ids) {
        return curriculum.stages(Actors.current()).stream()
                .filter(s -> ids.ids().contains(String.valueOf(s.get("id"))))
                .toList();
    }
}
