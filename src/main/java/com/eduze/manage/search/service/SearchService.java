package com.eduze.manage.search.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eduze.manage.auth.security.CustomUserDetails;
import com.eduze.manage.branch.domain.Branch;
import com.eduze.manage.branch.mapper.BranchMapper;
import com.eduze.manage.course.domain.ClassGroup;
import com.eduze.manage.course.mapper.ClassGroupMapper;
import com.eduze.manage.lesson.domain.Lesson;
import com.eduze.manage.lesson.mapper.LessonMapper;
import com.eduze.manage.search.dto.SearchHit;
import com.eduze.manage.student.domain.Guardian;
import com.eduze.manage.student.domain.Student;
import com.eduze.manage.student.domain.StudentGuardianRelation;
import com.eduze.manage.student.mapper.GuardianMapper;
import com.eduze.manage.student.mapper.StudentGuardianRelationMapper;
import com.eduze.manage.student.mapper.StudentMapper;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SearchService {

    private static final int PER_TYPE_LIMIT = 10;
    private static final int TOTAL_LIMIT = 20;
    private static final DateTimeFormatter LESSON_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final StudentMapper studentMapper;
    private final GuardianMapper guardianMapper;
    private final StudentGuardianRelationMapper relationMapper;
    private final BranchMapper branchMapper;
    private final ClassGroupMapper classGroupMapper;
    private final LessonMapper lessonMapper;

    public List<SearchHit> search(String q, Set<String> types, int limit) {
        if (!StringUtils.hasText(q)) {
            return List.of();
        }
        String keyword = q.trim();
        int cap = limit > 0 ? Math.min(limit, TOTAL_LIMIT) : TOTAL_LIMIT;
        List<Long> branchScope = allowedBranchIds();

        boolean allTypes = types == null || types.isEmpty();
        boolean searchStudent = allTypes || types.contains("student");
        boolean searchGuardian = allTypes || types.contains("guardian");
        boolean searchClassGroup = allTypes || types.contains("class_group");
        boolean searchLesson = allTypes || types.contains("lesson");

        List<SearchHit> hits = new ArrayList<>();
        if (searchStudent) {
            hits.addAll(searchStudents(keyword, branchScope));
        }
        if (searchGuardian) {
            hits.addAll(searchGuardians(keyword));
        }
        if (searchClassGroup) {
            hits.addAll(searchClassGroups(keyword, branchScope));
        }
        if (searchLesson) {
            hits.addAll(searchLessons(keyword, branchScope));
        }

        hits.sort(Comparator.comparingInt(h -> score(keyword, h)));
        if (hits.size() > cap) {
            return hits.subList(0, cap);
        }
        return hits;
    }

    private List<SearchHit> searchStudents(String keyword, List<Long> branchScope) {
        var wrapper = Wrappers.<Student>lambdaQuery()
                .and(w -> w.like(Student::getName, keyword).or().like(Student::getEnrollNo, keyword))
                .orderByDesc(Student::getId)
                .last("LIMIT " + PER_TYPE_LIMIT);
        if (branchScope != null) {
            wrapper.in(Student::getBranchId, branchScope);
        }
        List<Student> students = studentMapper.selectList(wrapper);
        Map<Long, String> branchNames = loadBranchNames(
                students.stream().map(Student::getBranchId).collect(Collectors.toSet()));
        List<SearchHit> hits = new ArrayList<>();
        for (Student s : students) {
            String branch = branchNames.getOrDefault(s.getBranchId(), "");
            hits.add(SearchHit.builder()
                    .type("student")
                    .id(s.getId())
                    .title(s.getName())
                    .subtitle(s.getEnrollNo() + (branch.isEmpty() ? "" : " · " + branch))
                    .url("/students/" + s.getId())
                    .branch(branch)
                    .branchId(s.getBranchId())
                    .build());
        }
        hits.sort(Comparator.comparingInt(h -> enrollScore(keyword, h)));
        return hits;
    }

    private List<SearchHit> searchGuardians(String keyword) {
        List<Long> branchScope = allowedBranchIds();
        List<Guardian> guardians = guardianMapper.selectList(
                Wrappers.<Guardian>lambdaQuery()
                        .and(w -> w.like(Guardian::getName, keyword).or().like(Guardian::getPhone, keyword))
                        .orderByDesc(Guardian::getId)
                        .last("LIMIT " + (PER_TYPE_LIMIT * 3)));
        List<SearchHit> hits = new ArrayList<>();
        for (Guardian g : guardians) {
            List<StudentGuardianRelation> relations = relationMapper.selectList(Wrappers.<StudentGuardianRelation>lambdaQuery()
                    .eq(StudentGuardianRelation::getGuardianId, g.getId()));
            if (relations.isEmpty()) {
                // 无关联学员：仅超管（branchScope==null）可见，用于全局家长检索
                if (branchScope != null) {
                    continue;
                }
                hits.add(SearchHit.builder()
                        .type("guardian")
                        .id(g.getId())
                        .title(g.getName())
                        .subtitle(g.getPhone())
                        .url("/guardians/" + g.getId())
                        .branch("")
                        .branchId(null)
                        .build());
                if (hits.size() >= PER_TYPE_LIMIT) {
                    break;
                }
                continue;
            }
            List<Long> studentIds = relations.stream().map(StudentGuardianRelation::getStudentId).toList();
            List<Student> students = studentMapper.selectBatchIds(studentIds);
            if (branchScope != null) {
                students = students.stream().filter(s -> branchScope.contains(s.getBranchId())).toList();
                if (students.isEmpty()) {
                    continue;
                }
            }
            String studentNames = students.stream().map(Student::getName).collect(Collectors.joining(", "));
            hits.add(SearchHit.builder()
                    .type("guardian")
                    .id(g.getId())
                    .title(g.getName())
                    .subtitle(g.getPhone() + (studentNames.isEmpty() ? "" : " · 学员: " + studentNames))
                    .url("/guardians/" + g.getId())
                    .branch("")
                    .branchId(students.get(0).getBranchId())
                    .build());
            if (hits.size() >= PER_TYPE_LIMIT) {
                break;
            }
        }
        hits.sort(Comparator.comparingInt(h -> phoneScore(keyword, h)));
        return hits;
    }

    private List<SearchHit> searchClassGroups(String keyword, List<Long> branchScope) {
        var wrapper = Wrappers.<ClassGroup>lambdaQuery()
                .like(ClassGroup::getName, keyword)
                .orderByDesc(ClassGroup::getId)
                .last("LIMIT " + PER_TYPE_LIMIT);
        if (branchScope != null) {
            wrapper.in(ClassGroup::getBranchId, branchScope);
        }
        List<ClassGroup> groups = classGroupMapper.selectList(wrapper);
        Map<Long, String> branchNames = loadBranchNames(
                groups.stream().map(ClassGroup::getBranchId).collect(Collectors.toSet()));
        List<SearchHit> hits = new ArrayList<>();
        for (ClassGroup g : groups) {
            String branch = branchNames.getOrDefault(g.getBranchId(), "");
            hits.add(SearchHit.builder()
                    .type("class_group")
                    .id(g.getId())
                    .title(g.getName())
                    .subtitle(branch)
                    .url("/class-groups/" + g.getId())
                    .branch(branch)
                    .branchId(g.getBranchId())
                    .build());
        }
        return hits;
    }

    private List<SearchHit> searchLessons(String keyword, List<Long> branchScope) {
        var groupWrapper = Wrappers.<ClassGroup>lambdaQuery().like(ClassGroup::getName, keyword);
        if (branchScope != null) {
            groupWrapper.in(ClassGroup::getBranchId, branchScope);
        }
        List<ClassGroup> groups = classGroupMapper.selectList(groupWrapper.last("LIMIT " + PER_TYPE_LIMIT));
        if (groups.isEmpty()) {
            return List.of();
        }
        Map<Long, ClassGroup> groupById =
                groups.stream().collect(Collectors.toMap(ClassGroup::getId, g -> g, (a, b) -> a));
        Map<Long, String> branchNames = loadBranchNames(
                groups.stream().map(ClassGroup::getBranchId).collect(Collectors.toSet()));

        List<SearchHit> hits = new ArrayList<>();
        for (ClassGroup group : groups) {
            List<Lesson> lessons = lessonMapper.selectList(Wrappers.<Lesson>lambdaQuery()
                    .eq(Lesson::getClassGroupId, group.getId())
                    .orderByAsc(Lesson::getStartAt)
                    .last("LIMIT 3"));
            String branch = branchNames.getOrDefault(group.getBranchId(), "");
            for (Lesson lesson : lessons) {
                String time = lesson.getStartAt() != null ? lesson.getStartAt().format(LESSON_TIME_FMT) : "";
                hits.add(SearchHit.builder()
                        .type("lesson")
                        .id(lesson.getId())
                        .title(group.getName())
                        .subtitle(time)
                        .url("/lessons/" + lesson.getId())
                        .branch(branch)
                        .branchId(group.getBranchId())
                        .build());
            }
        }
        return hits;
    }

    private String relatedStudentNames(Long guardianId) {
        List<StudentGuardianRelation> relations = relationMapper.selectList(
                Wrappers.<StudentGuardianRelation>lambdaQuery().eq(StudentGuardianRelation::getGuardianId, guardianId));
        if (relations.isEmpty()) {
            return "";
        }
        List<Long> studentIds =
                relations.stream().map(StudentGuardianRelation::getStudentId).limit(3).toList();
        return studentMapper.selectBatchIds(studentIds).stream()
                .map(Student::getName)
                .collect(Collectors.joining("、"));
    }

    private Map<Long, String> loadBranchNames(Set<Long> branchIds) {
        if (branchIds.isEmpty()) {
            return Map.of();
        }
        return branchMapper.selectBatchIds(branchIds).stream()
                .collect(Collectors.toMap(Branch::getId, Branch::getName, (a, b) -> a));
    }

    private List<Long> allowedBranchIds() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails user) {
            if (!user.isSuperAdmin() && !user.getBranchIds().isEmpty()) {
                return user.getBranchIds();
            }
        }
        return null;
    }

    private int score(String keyword, SearchHit hit) {
        return switch (hit.getType()) {
            case "student" -> enrollScore(keyword, hit);
            case "guardian" -> phoneScore(keyword, hit);
            case "class_group", "lesson" -> titleScore(keyword, hit);
            default -> 10;
        };
    }

    private int enrollScore(String keyword, SearchHit hit) {
        if (hit.getSubtitle() != null && hit.getSubtitle().startsWith(keyword)) {
            return 0;
        }
        if (hit.getTitle() != null && hit.getTitle().equals(keyword)) {
            return 1;
        }
        return 10;
    }

    private int phoneScore(String keyword, SearchHit hit) {
        if (hit.getSubtitle() != null && hit.getSubtitle().startsWith(keyword)) {
            return 0;
        }
        return 10;
    }

    private int titleScore(String keyword, SearchHit hit) {
        if (hit.getTitle() != null && hit.getTitle().contains(keyword)) {
            return 2;
        }
        return 10;
    }
}
