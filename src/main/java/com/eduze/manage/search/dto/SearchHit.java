package com.eduze.manage.search.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SearchHit {

    private final String type;
    private final Long id;
    private final String title;
    private final String subtitle;
    private final String url;
    private final String branch;
    private final Long branchId;
}
