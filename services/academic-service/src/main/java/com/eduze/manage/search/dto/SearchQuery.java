package com.eduze.manage.search.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchQuery {

    @NotBlank
    @Size(max = 64)
    private String q;

    private List<String> types;

    @Min(1)
    @Max(50)
    private int limit = 20;
}
