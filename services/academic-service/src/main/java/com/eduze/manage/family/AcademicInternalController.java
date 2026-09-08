package com.eduze.manage.family;

import com.eduze.manage.family.AcademicInternalService.LessonValidation;
import com.eduze.platform.runtime.PlatformException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/academic")
@RequiredArgsConstructor
public class AcademicInternalController {
    private final AcademicAccess access;
    private final AcademicInternalService service;
    private final com.eduze.platform.runtime.InternalCredentials credentials;

    public record Batch(List<String> ids) {}

    @GetMapping("/students/{id}/access")
    public AcademicAccess.Access student(@PathVariable String id) {
        var s = access.student(id);
        return new AcademicAccess.Access(true, s.branchId());
    }

    @PostMapping("/students/batch")
    public List<AcademicAccess.StudentView> students(@RequestBody Batch batch) {
        if (batch.ids() == null || batch.ids().size() > 200)
            throw new PlatformException(400, "每批最多 200 人");
        return batch.ids().stream().distinct().map(access::student).toList();
    }

    public record AccessItem(String id, boolean allowed, String branchId) {}

    @PostMapping("/students/batch-access")
    public List<AccessItem> studentAccessBatch(@RequestBody Batch batch) {
        validateBatch(batch);
        return batch.ids().stream()
                .distinct()
                .map(
                        id -> {
                            try {
                                var s = access.student(id);
                                return new AccessItem(id, true, s.branchId());
                            } catch (PlatformException ex) {
                                if (ex.getStatus() == 403 || ex.getStatus() == 404)
                                    return new AccessItem(id, false, null);
                                throw ex;
                            }
                        })
                .toList();
    }

    @PostMapping("/groups/batch-access")
    public List<AccessItem> groupAccessBatch(@RequestBody Batch batch) {
        validateBatch(batch);
        return batch.ids().stream()
                .distinct()
                .map(
                        id -> {
                            try {
                                var s = access.group(id);
                                return new AccessItem(id, true, s.branchId());
                            } catch (PlatformException ex) {
                                if (ex.getStatus() == 403 || ex.getStatus() == 404)
                                    return new AccessItem(id, false, null);
                                throw ex;
                            }
                        })
                .toList();
    }

    private void validateBatch(Batch batch) {
        if (batch.ids() == null || batch.ids().size() > 200)
            throw new PlatformException(400, "每批最多 200 个 ID");
    }

    @GetMapping("/groups/{id}/access")
    public AcademicAccess.Access group(@PathVariable String id) {
        return access.group(id);
    }

    @GetMapping("/groups/{id}/students")
    public List<AcademicAccess.StudentView> roster(@PathVariable String id) {
        return access.roster(id);
    }

    @PostMapping("/lessons/validate")
    public Map<String, Boolean> validate(@RequestBody LessonValidation request) {
        return service.validate(request);
    }

    @GetMapping("/students/{id}/families")
    public List<Map<String, Object>> families(@PathVariable String id) {
        return service.families(id);
    }

    @GetMapping("/lessons/{lessonId}/students/{studentId}/notification-access")
    public Map<String, Object> notificationAccess(
            @PathVariable String lessonId,
            @PathVariable String studentId,
            HttpServletRequest request) {
        credentials.requireCaller(request, "notification");
        return service.notificationAccess(lessonId, studentId);
    }

    @GetMapping("/students/{id}/recipients")
    public Map<String, Object> recipients(@PathVariable String id, HttpServletRequest request) {
        String caller = credentials.require(request);
        if (!java.util.Set.of("notification", "portfolio").contains(caller))
            throw new PlatformException(403, "无权读取收件人");
        return service.recipients(id);
    }
}
