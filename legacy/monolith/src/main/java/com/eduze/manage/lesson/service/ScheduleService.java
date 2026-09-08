package com.eduze.manage.lesson.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eduze.manage.auth.domain.User;
import com.eduze.manage.auth.mapper.UserMapper;
import com.eduze.manage.course.domain.ClassGroup;
import com.eduze.manage.course.domain.ClassRoom;
import com.eduze.manage.course.domain.Course;
import com.eduze.manage.course.mapper.ClassGroupMapper;
import com.eduze.manage.course.mapper.ClassRoomMapper;
import com.eduze.manage.course.mapper.CourseMapper;
import com.eduze.manage.lesson.domain.Lesson;
import com.eduze.manage.lesson.dto.ScheduleResponse;
import com.eduze.manage.lesson.mapper.LessonMapper;
import com.eduze.manage.lesson.support.CourseColorPalette;
import com.eduze.manage.tenant.BranchAccessGuard;
import com.eduze.manage.tenant.TenantContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final LessonMapper lessonMapper;
    private final ClassGroupMapper classGroupMapper;
    private final CourseMapper courseMapper;
    private final ClassRoomMapper classRoomMapper;
    private final UserMapper userMapper;
    private final BranchAccessGuard branchAccessGuard;

    public ScheduleResponse weekSchedule(Long branchId, LocalDate weekStart) {
        if (branchId != null) {
            branchAccessGuard.requireBranchAccess(branchId);
        }
        LocalDate start = weekStart != null ? weekStart : LocalDate.now();
        LocalDate end = start.plusDays(7);
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.atStartOfDay();

        List<Lesson> lessons =
                lessonMapper.selectList(
                        Wrappers.<Lesson>lambdaQuery()
                                .eq(Lesson::getTenantId, TenantContext.getTenantId())
                                .eq(branchId != null, Lesson::getBranchId, branchId)
                                .ge(Lesson::getStartAt, from)
                                .lt(Lesson::getStartAt, to)
                                .orderByAsc(Lesson::getStartAt));

        Map<LocalDate, List<Lesson>> byDay =
                lessons.stream().collect(Collectors.groupingBy(l -> l.getStartAt().toLocalDate()));

        List<ScheduleResponse.DaySchedule> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = start.plusDays(i);
            List<Lesson> dayLessons = byDay.getOrDefault(day, List.of());
            dayLessons =
                    dayLessons.stream().sorted(Comparator.comparing(Lesson::getStartAt)).toList();
            days.add(
                    ScheduleResponse.DaySchedule.builder()
                            .date(day)
                            .lessons(dayLessons.stream().map(this::toItem).toList())
                            .build());
        }

        return ScheduleResponse.builder()
                .weekStart(start)
                .weekEnd(end.minusDays(1))
                .days(days)
                .build();
    }

    private ScheduleResponse.ScheduleLessonItem toItem(Lesson lesson) {
        ClassGroup group = classGroupMapper.selectById(lesson.getClassGroupId());
        String classGroupName = group != null ? group.getName() : "";
        Long courseId = group != null ? group.getCourseId() : null;
        String courseName = "";
        if (courseId != null) {
            Course course = courseMapper.selectById(courseId);
            courseName = course != null ? course.getName() : "";
        }
        String teacherShort = "";
        if (lesson.getTeacherId() != null) {
            User teacher = userMapper.selectById(lesson.getTeacherId());
            if (teacher != null && teacher.getName() != null && !teacher.getName().isEmpty()) {
                teacherShort = teacher.getName().substring(0, 1);
            }
        }
        String roomShort = "";
        if (lesson.getClassRoomId() != null) {
            ClassRoom room = classRoomMapper.selectById(lesson.getClassRoomId());
            if (room != null && room.getName() != null) {
                roomShort = abbreviate(room.getName(), 4);
            }
        }
        return ScheduleResponse.ScheduleLessonItem.builder()
                .id(lesson.getId())
                .classGroupId(lesson.getClassGroupId())
                .classGroupName(classGroupName)
                .courseId(courseId)
                .courseName(courseName)
                .teacherId(lesson.getTeacherId())
                .teacherShortName(teacherShort)
                .classRoomId(lesson.getClassRoomId())
                .classRoomShortName(roomShort)
                .startAt(lesson.getStartAt().toString())
                .endAt(lesson.getEndAt().toString())
                .status(lesson.getStatus())
                .color(CourseColorPalette.colorForCourse(courseId))
                .build();
    }

    private static String abbreviate(String text, int maxLen) {
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen);
    }
}
