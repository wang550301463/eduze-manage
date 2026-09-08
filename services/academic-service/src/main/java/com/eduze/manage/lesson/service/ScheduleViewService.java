package com.eduze.manage.lesson.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eduze.manage.course.domain.ClassRoom;
import com.eduze.manage.course.mapper.ClassRoomMapper;
import com.eduze.manage.directory.UserDirectory;
import com.eduze.manage.directory.UserView;
import com.eduze.manage.lesson.domain.Lesson;
import com.eduze.manage.lesson.domain.LessonStudent;
import com.eduze.manage.lesson.dto.TeacherScheduleResponse;
import com.eduze.manage.lesson.mapper.LessonMapper;
import com.eduze.manage.lesson.mapper.LessonStudentMapper;
import com.eduze.manage.teacher.domain.TeacherAvailability;
import com.eduze.manage.teacher.mapper.TeacherAvailabilityMapper;
import com.eduze.manage.tenant.BranchAccessGuard;
import com.eduze.manage.tenant.TenantContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** 周课表"按老师列"聚合查询服务。 */
@Service
@RequiredArgsConstructor
public class ScheduleViewService {

    private final LessonMapper lessonMapper;
    private final UserDirectory userDirectory;
    private final ClassRoomMapper classRoomMapper;
    private final TeacherAvailabilityMapper availabilityMapper;
    private final LessonStudentMapper lessonStudentMapper;
    private final JdbcTemplate jdbcTemplate;
    private final BranchAccessGuard branchAccessGuard;

    public TeacherScheduleResponse byTeacher(Long branchId, LocalDate weekStart) {
        if (branchId != null) {
            branchAccessGuard.requireBranchAccess(branchId);
        }
        Long tenantId = TenantContext.getTenantId();
        LocalDate ws = weekStart != null ? weekStart : currentWeekStart();
        LocalDateTime from = ws.atStartOfDay();
        LocalDateTime to = ws.plusDays(7).atStartOfDay();

        // 1. 加载本周所有 lesson
        LambdaQueryWrapper<Lesson> wrapper =
                new LambdaQueryWrapper<Lesson>()
                        .ge(Lesson::getStartAt, from)
                        .lt(Lesson::getStartAt, to);
        if (branchId != null) wrapper.eq(Lesson::getBranchId, branchId);
        List<Lesson> lessons = lessonMapper.selectList(wrapper);

        // 2. 加载老师列表（同校区所有 TEACHER 用户）
        List<UserView> teacherRows = userDirectory.teachers(branchId);

        Set<Long> teacherIds =
                teacherRows.stream()
                        .map(r -> r.getId())
                        .collect(Collectors.toCollection(HashSet::new));
        lessons.forEach(
                l -> {
                    if (l.getTeacherId() != null) teacherIds.add(l.getTeacherId());
                });

        Map<Long, UserView> userById =
                userDirectory.batch(teacherIds).stream()
                        .collect(Collectors.toMap(UserView::getId, u -> u));
        Map<Long, ClassRoom> roomById =
                lessons.stream()
                        .map(Lesson::getClassRoomId)
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .collect(
                                Collectors.toMap(
                                        id -> id,
                                        id -> classRoomMapper.selectById(id),
                                        (a, b) -> a));
        Map<Long, TeacherAvailability> availById =
                lessons.stream()
                        .map(Lesson::getTeacherAvailabilityId)
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .collect(
                                Collectors.toMap(
                                        id -> id,
                                        id -> availabilityMapper.selectById(id),
                                        (a, b) -> a));

        // 3. 学员数：按 lessonId GROUP COUNT
        Map<Long, Integer> studentCountByLesson = new HashMap<>();
        if (!lessons.isEmpty()) {
            List<Long> lessonIds = lessons.stream().map(Lesson::getId).toList();
            List<LessonStudent> roster =
                    lessonStudentMapper.selectList(
                            new LambdaQueryWrapper<LessonStudent>()
                                    .in(LessonStudent::getLessonId, lessonIds)
                                    .eq(LessonStudent::getStatus, "BOOKED"));
            for (LessonStudent r : roster) {
                studentCountByLesson.merge(r.getLessonId(), 1, Integer::sum);
            }
        }

        // 4. 按老师分组
        Map<Long, List<Lesson>> lessonsByTeacher =
                lessons.stream()
                        .filter(l -> l.getTeacherId() != null)
                        .collect(Collectors.groupingBy(Lesson::getTeacherId));

        List<TeacherScheduleResponse.TeacherColumn> columns =
                teacherRows.stream()
                        .map(
                                r ->
                                        buildColumn(
                                                r.getId(),
                                                r.getName(),
                                                r.getBranchId(),
                                                lessonsByTeacher,
                                                roomById,
                                                availById,
                                                studentCountByLesson))
                        .toList();

        return TeacherScheduleResponse.builder().weekStart(ws).columns(columns).build();
    }

    public TeacherScheduleResponse myWeek(Long teacherId, LocalDate weekStart) {
        Long tenantId = TenantContext.getTenantId();
        LocalDate ws = weekStart != null ? weekStart : currentWeekStart();
        LocalDateTime from = ws.atStartOfDay();
        LocalDateTime to = ws.plusDays(7).atStartOfDay();

        List<Lesson> lessons =
                lessonMapper.selectList(
                        new LambdaQueryWrapper<Lesson>()
                                .eq(Lesson::getTeacherId, teacherId)
                                .ge(Lesson::getStartAt, from)
                                .lt(Lesson::getStartAt, to));

        UserView teacher = userDirectory.get(teacherId);
        if (teacher == null) {
            return TeacherScheduleResponse.builder().weekStart(ws).columns(List.of()).build();
        }
        Map<Long, ClassRoom> roomById =
                lessons.stream()
                        .map(Lesson::getClassRoomId)
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .collect(
                                Collectors.toMap(
                                        id -> id,
                                        id -> classRoomMapper.selectById(id),
                                        (a, b) -> a));
        Map<Long, TeacherAvailability> availById =
                lessons.stream()
                        .map(Lesson::getTeacherAvailabilityId)
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .collect(
                                Collectors.toMap(
                                        id -> id,
                                        id -> availabilityMapper.selectById(id),
                                        (a, b) -> a));

        Map<Long, Integer> studentCountByLesson = new HashMap<>();
        if (!lessons.isEmpty()) {
            List<Long> lessonIds = lessons.stream().map(Lesson::getId).toList();
            List<LessonStudent> roster =
                    lessonStudentMapper.selectList(
                            new LambdaQueryWrapper<LessonStudent>()
                                    .in(LessonStudent::getLessonId, lessonIds)
                                    .eq(LessonStudent::getStatus, "BOOKED"));
            for (LessonStudent r : roster) {
                studentCountByLesson.merge(r.getLessonId(), 1, Integer::sum);
            }
        }

        Map<Long, List<Lesson>> grouped = new HashMap<>();
        grouped.put(teacherId, lessons);
        TeacherScheduleResponse.TeacherColumn col =
                buildColumn(
                        teacherId,
                        teacher.getName(),
                        teacher.getBranchId(),
                        grouped,
                        roomById,
                        availById,
                        studentCountByLesson);
        return TeacherScheduleResponse.builder().weekStart(ws).columns(List.of(col)).build();
    }

    private TeacherScheduleResponse.TeacherColumn buildColumn(
            Long teacherId,
            String teacherName,
            Long branchId,
            Map<Long, List<Lesson>> lessonsByTeacher,
            Map<Long, ClassRoom> roomById,
            Map<Long, TeacherAvailability> availById,
            Map<Long, Integer> studentCountByLesson) {
        List<Lesson> lessons = lessonsByTeacher.getOrDefault(teacherId, List.of());
        List<TeacherScheduleResponse.LessonCell> cells =
                lessons.stream()
                        .sorted(Comparator.comparing(Lesson::getStartAt))
                        .map(l -> toCell(l, roomById, availById, studentCountByLesson))
                        .toList();
        return TeacherScheduleResponse.TeacherColumn.builder()
                .teacherId(teacherId)
                .teacherName(teacherName)
                .branchId(branchId)
                .lessons(cells)
                .build();
    }

    private TeacherScheduleResponse.LessonCell toCell(
            Lesson l,
            Map<Long, ClassRoom> roomById,
            Map<Long, TeacherAvailability> availById,
            Map<Long, Integer> studentCountByLesson) {
        ClassRoom room = l.getClassRoomId() == null ? null : roomById.get(l.getClassRoomId());
        TeacherAvailability avail =
                l.getTeacherAvailabilityId() == null
                        ? null
                        : availById.get(l.getTeacherAvailabilityId());
        int dayOfWeek = l.getStartAt().getDayOfWeek().getValue();
        int startMinute = l.getStartAt().getHour() * 60 + l.getStartAt().getMinute();
        int endMinute = l.getEndAt().getHour() * 60 + l.getEndAt().getMinute();
        return TeacherScheduleResponse.LessonCell.builder()
                .lessonId(l.getId())
                .classRoomId(l.getClassRoomId())
                .classRoomName(room != null ? room.getName() : null)
                .startAt(l.getStartAt())
                .endAt(l.getEndAt())
                .dayOfWeek(dayOfWeek)
                .startMinute(startMinute)
                .endMinute(endMinute)
                .capacity(avail != null ? avail.getCapacity() : null)
                .studentCount(studentCountByLesson.getOrDefault(l.getId(), 0))
                .source(l.getSource())
                .teacherAvailabilityId(l.getTeacherAvailabilityId())
                .status(l.getStatus())
                .build();
    }

    private LocalDate currentWeekStart() {
        LocalDate today = LocalDate.now();
        int dow = today.getDayOfWeek().getValue();
        return today.minusDays(dow - 1);
    }
}
