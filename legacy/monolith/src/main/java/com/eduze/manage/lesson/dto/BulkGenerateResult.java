package com.eduze.manage.lesson.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BulkGenerateResult {

    private final int generated;
    private final int skipped;
    private final int rosterAdded;
    private final List<ConflictItem> conflicts;

    @Getter
    @Builder
    public static class ConflictItem {
        private final LocalDate date;
        private final Long teacherId;
        private final String reason;
    }
}
