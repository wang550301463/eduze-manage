package com.eduze.manage.attendance.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eduze.manage.attendance.domain.Attendance;
import com.eduze.manage.attendance.domain.PickupRecord;
import com.eduze.manage.attendance.dto.PickupRecordRequest;
import com.eduze.manage.attendance.dto.PickupRecordResponse;
import com.eduze.manage.attendance.mapper.AttendanceMapper;
import com.eduze.manage.attendance.mapper.PickupRecordMapper;
import com.eduze.manage.common.exception.BizException;
import com.eduze.manage.common.exception.ErrorCode;
import com.eduze.manage.student.domain.Guardian;
import com.eduze.manage.student.domain.Student;
import com.eduze.manage.student.mapper.GuardianMapper;
import com.eduze.manage.student.mapper.StudentMapper;
import com.eduze.manage.tenant.TenantContext;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PickupService {

    private final PickupRecordMapper pickupRecordMapper;
    private final AttendanceMapper attendanceMapper;
    private final StudentMapper studentMapper;
    private final GuardianMapper guardianMapper;
    private final AttendanceSupport attendanceSupport;

    public List<PickupRecordResponse> list(Long lessonId, Long studentId, Integer isAbnormal) {
        List<Long> attendanceIds = null;
        if (lessonId != null) {
            attendanceIds = attendanceMapper.selectList(Wrappers.<Attendance>lambdaQuery()
                            .eq(Attendance::getTenantId, TenantContext.getTenantId())
                            .eq(Attendance::getLessonId, lessonId))
                    .stream()
                    .map(Attendance::getId)
                    .toList();
            if (attendanceIds.isEmpty()) {
                return List.of();
            }
        }
        final List<Long> filterIds = attendanceIds;
        return pickupRecordMapper.selectList(Wrappers.<PickupRecord>lambdaQuery()
                        .eq(PickupRecord::getTenantId, TenantContext.getTenantId())
                        .in(filterIds != null, PickupRecord::getAttendanceId, filterIds)
                        .eq(isAbnormal != null, PickupRecord::getIsAbnormal, isAbnormal)
                        .orderByDesc(PickupRecord::getEventTime))
                .stream()
                .filter(r -> studentId == null || matchesStudent(r, studentId))
                .map(this::toResponse)
                .toList();
    }

    public List<PickupRecordResponse> listAbnormal(LocalDateTime from, LocalDateTime to) {
        LocalDateTime start = from != null ? from : LocalDateTime.now().minusDays(7);
        LocalDateTime end = to != null ? to : LocalDateTime.now();
        return pickupRecordMapper.selectList(Wrappers.<PickupRecord>lambdaQuery()
                        .eq(PickupRecord::getTenantId, TenantContext.getTenantId())
                        .eq(PickupRecord::getIsAbnormal, 1)
                        .ge(PickupRecord::getEventTime, start)
                        .le(PickupRecord::getEventTime, end)
                        .orderByDesc(PickupRecord::getEventTime))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PickupRecordResponse create(PickupRecordRequest request) {
        Attendance attendance = requireAttendance(request.getAttendanceId());
        boolean abnormal = request.getIsAbnormal() != null && request.getIsAbnormal();
        if (!abnormal && request.getGuardianId() != null
                && !attendanceSupport.isAuthorizedPickup(attendance.getStudentId(), request.getGuardianId())) {
            abnormal = true;
        }
        LocalDateTime eventTime =
                request.getEventTime() != null ? request.getEventTime() : LocalDateTime.now();
        PickupRecord record = recordPickupInternal(
                attendance,
                request.getEventType(),
                request.getGuardianId(),
                abnormal,
                request.getAbnormalNote(),
                eventTime);
        return toResponse(record);
    }

    PickupRecord recordPickupInternal(
            Attendance attendance,
            String eventType,
            Long guardianId,
            Boolean isAbnormal,
            String abnormalNote,
            LocalDateTime eventTime) {
        boolean abnormal = isAbnormal != null && isAbnormal;
        if (!abnormal && guardianId != null
                && !attendanceSupport.isAuthorizedPickup(attendance.getStudentId(), guardianId)) {
            abnormal = true;
        }
        PickupRecord record = new PickupRecord();
        record.setTenantId(TenantContext.getTenantId());
        record.setBranchId(attendance.getBranchId());
        record.setAttendanceId(attendance.getId());
        record.setEventType(eventType);
        record.setGuardianId(guardianId);
        record.setIsAbnormal(abnormal ? 1 : 0);
        record.setAbnormalNote(abnormalNote);
        record.setEventTime(eventTime);
        pickupRecordMapper.insert(record);
        return record;
    }

    private boolean matchesStudent(PickupRecord record, Long studentId) {
        Attendance attendance = attendanceMapper.selectById(record.getAttendanceId());
        return attendance != null && studentId.equals(attendance.getStudentId());
    }

    private Attendance requireAttendance(Long id) {
        Attendance attendance = attendanceMapper.selectById(id);
        if (attendance == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "出勤记录不存在");
        }
        return attendance;
    }

    private PickupRecordResponse toResponse(PickupRecord record) {
        Attendance attendance = attendanceMapper.selectById(record.getAttendanceId());
        Student student = attendance != null ? studentMapper.selectById(attendance.getStudentId()) : null;
        Guardian guardian =
                record.getGuardianId() != null ? guardianMapper.selectById(record.getGuardianId()) : null;
        return PickupRecordResponse.builder()
                .id(record.getId())
                .attendanceId(record.getAttendanceId())
                .lessonId(attendance != null ? attendance.getLessonId() : null)
                .studentId(attendance != null ? attendance.getStudentId() : null)
                .studentName(student != null ? student.getName() : null)
                .eventType(record.getEventType())
                .guardianId(record.getGuardianId())
                .guardianName(guardian != null ? guardian.getName() : null)
                .isAbnormal(record.getIsAbnormal())
                .abnormalNote(record.getAbnormalNote())
                .eventTime(record.getEventTime())
                .build();
    }
}
