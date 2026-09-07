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
}
