package com.eduze.manage.lesson.controller;

import com.eduze.manage.auth.security.CustomUserDetails;
import com.eduze.manage.common.exception.BizException;
import com.eduze.manage.common.exception.ErrorCode;
import com.eduze.manage.common.web.ApiResponse;
import com.eduze.manage.lesson.dto.ScheduleResponse;
import com.eduze.manage.lesson.dto.TeacherScheduleResponse;
import com.eduze.manage.lesson.service.ScheduleService;
import com.eduze.manage.lesson.service.ScheduleViewService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final ScheduleViewService scheduleViewService;

    @GetMapping("/week")
    @PreAuthorize("hasAuthority('lesson:read')")
    public ApiResponse<ScheduleResponse> week(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate weekStart) {
        return ApiResponse.ok(scheduleService.weekSchedule(branchId, weekStart));
    }

    @GetMapping("/by-teacher")
    @PreAuthorize("hasAuthority('lesson:read')")
    public ApiResponse<TeacherScheduleResponse> byTeacher(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate weekStart) {
        return ApiResponse.ok(scheduleViewService.byTeacher(branchId, weekStart));
    }

    @GetMapping("/my-week")
    @PreAuthorize("hasAuthority('lesson:read')")
    public ApiResponse<TeacherScheduleResponse> myWeek(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate weekStart) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails user)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        return ApiResponse.ok(scheduleViewService.myWeek(user.getUserId(), weekStart));
    }
}
