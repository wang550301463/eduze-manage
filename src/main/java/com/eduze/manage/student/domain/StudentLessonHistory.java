package com.eduze.manage.student.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduze.manage.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 学员历史课程快照（审计用，只追加）。 */
@Getter
@Setter
@TableName("t_student_lesson_history")
public class StudentLessonHistory extends BaseEntity {

    private Long studentId;
    private String studentName;
    private Long lessonId;
    private Long attendanceId;
    private Long courseId;
    private String courseName;
    private Long classGroupId;
    private String classGroupName;
    private Long teacherId;
    private String teacherName;
    private Long classRoomId;
    private String classRoomName;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer source;
    private Integer attendanceStatus;
    private Integer minutes;
    private String snapshotJson;
    private LocalDateTime occurredAt;
}
