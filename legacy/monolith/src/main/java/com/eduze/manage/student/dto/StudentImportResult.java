package com.eduze.manage.student.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudentImportResult {

    private final int successCount;
    private final List<ImportFailure> failures;

    @Getter
    @Builder
    public static class ImportFailure {
        private final int rowIndex;
        private final List<FieldError> errors;
    }

    @Getter
    @Builder
    public static class FieldError {
        private final String column;
        private final String message;
    }
}
