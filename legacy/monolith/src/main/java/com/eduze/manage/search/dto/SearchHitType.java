package com.eduze.manage.search.dto;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SearchHitType {
    STUDENT("student"),
    GUARDIAN("guardian"),
    CLASS_GROUP("class_group"),
    LESSON("lesson");

    private final String code;

    SearchHitType(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public static SearchHitType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (SearchHitType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}
