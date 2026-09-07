package com.eduze.manage.lesson.service;

import com.eduze.manage.auth.domain.User;
import com.eduze.manage.auth.mapper.UserMapper;
import com.eduze.manage.course.domain.ClassGroup;
import com.eduze.manage.course.domain.ClassRoom;
import com.eduze.manage.course.domain.Course;
import com.eduze.manage.course.mapper.ClassGroupMapper;
import com.eduze.manage.course.mapper.ClassRoomMapper;
import com.eduze.manage.course.mapper.CourseMapper;
import com.eduze.manage.lesson.domain.Lesson;
import com.eduze.manage.lesson.dto.LessonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LessonAssembler {

    private final ClassGroupMapper classGroupMapper;
    private final CourseMapper courseMapper;
    private final ClassRoomMapper classRoomMapper;
    private final UserMapper userMapper;

    public LessonResponse toResponse(Lesson lesson) {
        ClassGroup group = classGroupMapper.selectById(lesson.getClassGroupId());
        String classGroupName = group != null ? group.getName() : null;
        Long courseId = group != null ? group.getCourseId() : null;
        String courseName = null;
        if (courseId != null) {
            Course course = courseMapper.selectById(courseId);
            courseName = course != null ? course.getName() : null;
        }
        String classRoomName = null;
        if (lesson.getClassRoomId() != null) {
            ClassRoom room = classRoomMapper.selectById(lesson.getClassRoomId());
            classRoomName = room != null ? room.getName() : null;
        }
        String teacherName = null;
        if (lesson.getTeacherId() != null) {
            User teacher = userMapper.selectById(lesson.getTeacherId());
            teacherName = teacher != null ? teacher.getName() : null;
        }
        return LessonResponse.builder()
                .id(lesson.getId())
                .tenantId(lesson.getTenantId())
                .branchId(lesson.getBranchId())
                .classGroupId(lesson.getClassGroupId())
                .classGroupName(classGroupName)
                .courseId(courseId)
                .courseName(courseName)
                .classRoomId(lesson.getClassRoomId())
                .classRoomName(classRoomName)
                .teacherId(lesson.getTeacherId())
                .teacherName(teacherName)
                .startAt(lesson.getStartAt())
                .endAt(lesson.getEndAt())
                .status(lesson.getStatus())
                .note(lesson.getNote())
                .build();
    }
}
