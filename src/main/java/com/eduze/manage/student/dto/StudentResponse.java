package com.eduze.manage.student.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudentResponse {

    private final Long id;
    private final Long tenantId;
    private final Long branchId;
    private final String branchName;
    private final String enrollNo;
    private final String name;
    private final Integer gender;
    private final LocalDate birthday;
    private final LocalDate enrollDate;
    private final Integer status;
    private final String allergy;
    private final String healthNote;
    private final String emergencyContact;
    private final String emergencyPhone;
    private final String avatarUrl;
    private final Long mentorTeacherId;
    private final String mentorTeacherName;
    private final Long currentStageId;
    private final String currentStageCode;
    private final String currentStageName;
    private final List<ClassGroupRef> classGroups;
    private final Integer totalRemaining;
    private final Boolean alertLow;

    @Getter
    @Builder
    public static class ClassGroupRef {
        private final Long id;
        private final String name;
    }
}
