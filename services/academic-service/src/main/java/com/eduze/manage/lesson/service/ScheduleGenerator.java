package com.eduze.manage.lesson.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eduze.manage.common.exception.BizException;
import com.eduze.manage.common.exception.ErrorCode;
import com.eduze.manage.course.domain.ClassGroup;
import com.eduze.manage.course.mapper.ClassGroupMapper;
import com.eduze.manage.lesson.domain.Lesson;
import com.eduze.manage.lesson.domain.LessonStudent;
import com.eduze.manage.lesson.domain.LessonSubscription;
import com.eduze.manage.lesson.dto.BulkGenerateRequest;
import com.eduze.manage.lesson.dto.BulkGenerateResult;
import com.eduze.manage.lesson.dto.ConflictReport;
import com.eduze.manage.lesson.mapper.LessonMapper;
import com.eduze.manage.lesson.mapper.LessonStudentMapper;
import com.eduze.manage.lesson.mapper.LessonSubscriptionMapper;
import com.eduze.manage.lesson.service.ConflictService.LessonDraft;
import com.eduze.manage.teacher.domain.TeacherAvailability;
import com.eduze.manage.teacher.mapper.TeacherAvailabilityMapper;
import com.eduze.manage.tenant.TenantContext;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 基于 TeacherAvailability 模板批量生成未来 N 周课次的核心算法。 输出：每个老师每个 availability 对应未来每周一节课次，并按订阅自动入名单。 仅处理已绑定
 * ClassGroup 的可用时段（D2）。
 */
@Service
@RequiredArgsConstructor
public class ScheduleGenerator {

    private final TeacherAvailabilityMapper availabilityMapper;
    private final ClassGroupMapper classGroupMapper;
    private final LessonMapper lessonMapper;
    private final com.eduze.manage.events.AcademicEvents events;
    private final LessonSubscriptionMapper subscriptionMapper;
    private final LessonStudentMapper lessonStudentMapper;
    private final ConflictService conflictService;

    @Transactional
    public BulkGenerateResult generate(BulkGenerateRequest req) {
        Long tenantId = TenantContext.getTenantId();
        Set<LocalDate> holidays =
                req.getHolidays() == null ? Set.of() : new HashSet<>(req.getHolidays());

        LambdaQueryWrapper<TeacherAvailability> wrapper =
                new LambdaQueryWrapper<TeacherAvailability>().eq(TeacherAvailability::getStatus, 1);
        if (req.getBranchId() != null) {
            wrapper.eq(TeacherAvailability::getBranchId, req.getBranchId());
        }
        if (req.getTeacherIds() != null && !req.getTeacherIds().isEmpty()) {
            wrapper.in(TeacherAvailability::getTeacherId, req.getTeacherIds());
        }
        List<TeacherAvailability> avails = availabilityMapper.selectList(wrapper);

        Set<Long> boundAvailIds = loadBoundAvailabilityIds(avails);

        List<BulkGenerateResult.ConflictItem> conflicts = new ArrayList<>();
        int generated = 0;
        int skipped = 0;
        int rosterAdded = 0;

        for (TeacherAvailability avail : avails) {
            if (!boundAvailIds.contains(avail.getId())) {
                continue;
            }
            ClassGroup boundGroup =
                    classGroupMapper.selectOne(
                            new LambdaQueryWrapper<ClassGroup>()
                                    .eq(ClassGroup::getTeacherAvailabilityId, avail.getId())
                                    .eq(ClassGroup::getStatus, 1)
                                    .last("LIMIT 1"));
            if (boundGroup == null) {
                continue;
            }
            List<LessonSubscription> subs =
                    subscriptionMapper.selectList(
                            new LambdaQueryWrapper<LessonSubscription>()
                                    .eq(LessonSubscription::getTeacherAvailabilityId, avail.getId())
                                    .eq(LessonSubscription::getStatus, 1));

            for (int weekOffset = 0; weekOffset < req.getWeeks(); weekOffset++) {
                LocalDate weekStart = req.getFromDate().plusWeeks(weekOffset);
                LocalDate lessonDate = nextDayOfWeek(weekStart, avail.getDayOfWeek());

                if (avail.getValidFrom() != null && lessonDate.isBefore(avail.getValidFrom())) {
                    continue;
                }
                if (avail.getValidTo() != null && lessonDate.isAfter(avail.getValidTo())) {
                    continue;
                }
                if (holidays.contains(lessonDate)) {
                    continue;
                }

                LocalDateTime startAt =
                        lessonDate.atStartOfDay().plusMinutes(avail.getStartMinute());
                LocalDateTime endAt = lessonDate.atStartOfDay().plusMinutes(avail.getEndMinute());

                Long existing =
                        lessonMapper.selectCount(
                                new LambdaQueryWrapper<Lesson>()
                                        .eq(Lesson::getTeacherId, avail.getTeacherId())
                                        .eq(Lesson::getStartAt, startAt));
                if (existing != null && existing > 0) {
                    skipped++;
                    continue;
                }

                ConflictReport report =
                        conflictService.check(
                                new LessonDraft(
                                        null,
                                        avail.getBranchId(),
                                        null,
                                        avail.getDefaultClassRoomId(),
                                        avail.getTeacherId(),
                                        startAt,
                                        endAt));
                if (report.isHasConflict()) {
                    throw new BizException(ErrorCode.CONFLICT, conflictReason(report));
                }

                Lesson lesson = new Lesson();
                lesson.setTenantId(tenantId);
                lesson.setBranchId(avail.getBranchId());
                lesson.setClassGroupId(boundGroup.getId());
                lesson.setClassRoomId(avail.getDefaultClassRoomId());
                lesson.setTeacherId(avail.getTeacherId());
                lesson.setStartAt(startAt);
                lesson.setEndAt(endAt);
                lesson.setStatus("SCHEDULED");
                lesson.setTeacherAvailabilityId(avail.getId());
                lesson.setSource(1);
                lessonMapper.insert(lesson);
                generated++;

                for (LessonSubscription sub : subs) {
                    if (sub.getValidFrom() != null && lessonDate.isBefore(sub.getValidFrom()))
                        continue;
                    if (sub.getValidTo() != null && lessonDate.isAfter(sub.getValidTo())) continue;
                    LessonStudent ls = new LessonStudent();
                    ls.setTenantId(tenantId);
                    ls.setBranchId(avail.getBranchId());
                    ls.setLessonId(lesson.getId());
                    ls.setStudentId(sub.getStudentId());
                    ls.setSubscriptionId(sub.getId());
                    ls.setSource("SUBSCRIPTION");
                    ls.setStatus("BOOKED");
                    try {
                        lessonStudentMapper.insert(ls);
                        rosterAdded++;
                    } catch (RuntimeException ex) {
                        conflicts.add(
                                BulkGenerateResult.ConflictItem.builder()
                                        .date(lessonDate)
                                        .teacherId(avail.getTeacherId())
                                        .reason("名单冲突：学员" + sub.getStudentId())
                                        .build());
                    }
                }
                events.lessonChanged(lesson);
            }
        }

        return BulkGenerateResult.builder()
                .generated(generated)
                .skipped(skipped)
                .rosterAdded(rosterAdded)
                .conflicts(conflicts.isEmpty() ? Collections.emptyList() : conflicts)
                .build();
    }

    private Set<Long> loadBoundAvailabilityIds(List<TeacherAvailability> avails) {
        if (avails.isEmpty()) {
            return Set.of();
        }
        Set<Long> ids =
                avails.stream()
                        .map(TeacherAvailability::getId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Set.of();
        }
        return classGroupMapper
                .selectList(
                        new LambdaQueryWrapper<ClassGroup>()
                                .eq(ClassGroup::getTenantId, TenantContext.getTenantId())
                                .in(ClassGroup::getTeacherAvailabilityId, ids))
                .stream()
                .map(ClassGroup::getTeacherAvailabilityId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private static String conflictReason(ConflictReport report) {
        if (report.getTeacher() != null) {
            return "教师时间冲突";
        }
        if (report.getClassRoom() != null) {
            return "画室时间冲突";
        }
        if (report.getClassGroup() != null) {
            return "班级时间冲突";
        }
        if (report.getBoundAvailability() != null) {
            return "与已绑分组正常时段冲突";
        }
        return "时间冲突";
    }

    private LocalDate nextDayOfWeek(LocalDate weekStart, int dayOfWeek) {
        int wsDow = weekStart.getDayOfWeek().getValue();
        int diff = dayOfWeek - wsDow;
        if (diff < 0) diff += 7;
        return weekStart.plusDays(diff);
    }

    public LocalDate resolveDate(LocalDate weekStart, DayOfWeek dow) {
        return nextDayOfWeek(weekStart, dow.getValue());
    }
}
