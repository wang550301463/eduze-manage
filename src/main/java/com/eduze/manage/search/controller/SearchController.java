package com.eduze.manage.search.controller;

import com.eduze.manage.common.web.ApiResponse;
import com.eduze.manage.search.dto.SearchHit;
import com.eduze.manage.search.service.SearchService;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    @PreAuthorize("hasAuthority('search:read')")
    public ApiResponse<List<SearchHit>> search(
            @RequestParam String q,
            @RequestParam(required = false) String types,
            @RequestParam(defaultValue = "20") int limit) {
        Set<String> typeSet = null;
        if (types != null && !types.isBlank()) {
            typeSet = Arrays.stream(types.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
        }
        return ApiResponse.ok(searchService.search(q, typeSet, limit));
    }
}
