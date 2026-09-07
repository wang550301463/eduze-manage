package com.eduze.manage.attendance.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LeaveStatus {
    PENDING(1),
    APPROVED(2),
    REJECTED(3);

    private final int code;

    public static LeaveStatus fromCode(int code) {
        for (LeaveStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown leave status: " + code);
    }
}
