package com.eduze.manage.attendance.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TodayRosterResponse {

    private List<RosterItem> items;
    private int totalExpected;
    private int checkedInCount;

    @Getter
    @Builder
    public static class RosterItem {
        private Long lessonId;
        private Long studentId;
        private String studentName;
        private String classGroupName;
        private LocalDateTime lessonStartAt;
        private Long attendanceId;
        private Integer status;
        private String statusLabel;
        private LocalDateTime checkInAt;
        private LocalDateTime checkOutAt;
    }
}
