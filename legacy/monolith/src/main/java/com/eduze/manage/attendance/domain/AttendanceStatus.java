package com.eduze.manage.attendance.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AttendanceStatus {
    CHECKED_IN(2, "已入园"),
    CHECKED_OUT(3, "已离园"),
    ABSENT(4, "缺勤"),
    LEAVE(5, "请假");

    private final int code;
    private final String label;

    public static AttendanceStatus fromCode(int code) {
        for (AttendanceStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown attendance status: " + code);
    }
}
