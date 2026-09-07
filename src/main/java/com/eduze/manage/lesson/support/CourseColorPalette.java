package com.eduze.manage.lesson.support;

public final class CourseColorPalette {

    private static final String[] COLORS = {
        "#E85D4C", "#F4A261", "#E9C46A", "#2A9D8F", "#264653",
        "#8B5CF6", "#EC4899", "#06B6D4", "#84CC16", "#F97316",
        "#6366F1", "#14B8A6"
    };

    private CourseColorPalette() {}

    public static String colorForCourse(Long courseId) {
        if (courseId == null) {
            return COLORS[0];
        }
        int index = (int) (Math.floorMod(courseId, COLORS.length));
        return COLORS[index];
    }
}
