package com.eduze.manage.lesson.domain;

/** Lesson.source 约定值。 */
public final class LessonSource {

    public static final int TEMPLATE = 1;
    public static final int MANUAL = 2;
    public static final int MAKEUP = 3;
    public static final int TRIAL = 4;
    public static final int EXAM = 5;
    public static final int CONTEST = 6;

    private LessonSource() {}

    public static boolean isSpecial(Integer source) {
        return source != null && (source == MAKEUP || source == EXAM || source == CONTEST);
    }

    /** 课次来源中文标签：1模板 2手动→正常，3补课 4试听 5考级 6比赛。 */
    public static String label(Integer source) {
        if (source == null) {
            return "";
        }
        return switch (source) {
            case TEMPLATE, MANUAL -> "正常";
            case MAKEUP -> "补课";
            case TRIAL -> "试听";
            case EXAM -> "考级";
            case CONTEST -> "比赛";
            default -> String.valueOf(source);
        };
    }
}
