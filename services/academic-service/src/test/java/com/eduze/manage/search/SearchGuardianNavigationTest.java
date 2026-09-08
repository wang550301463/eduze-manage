package com.eduze.manage.search;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.eduze.manage.auth.security.CustomUserDetails;
import com.eduze.manage.course.mapper.ClassGroupMapper;
import com.eduze.manage.directory.BranchDirectory;
import com.eduze.manage.lesson.mapper.LessonMapper;
import com.eduze.manage.search.service.SearchService;
import com.eduze.manage.student.domain.Guardian;
import com.eduze.manage.student.domain.Student;
import com.eduze.manage.student.domain.StudentGuardianRelation;
import com.eduze.manage.student.mapper.GuardianMapper;
import com.eduze.manage.student.mapper.StudentGuardianRelationMapper;
import com.eduze.manage.student.mapper.StudentMapper;
import com.eduze.manage.tenant.BranchAccessGuard;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class SearchGuardianNavigationTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void linkedGuardianOpensAnAuthorizedStudentWithoutRoundingIds() {
        var user =
                new CustomUserDetails(
                        1L,
                        1L,
                        "teacher",
                        "老师",
                        "",
                        List.of(22L),
                        Set.of("TEACHER"),
                        List.of("student:read"),
                        1,
                        true);
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));

        var guardian = new Guardian();
        guardian.setId(9007199254740993123L);
        guardian.setName("林家长");
        guardian.setPhone("13800000000");
        var outsideScope = new Student();
        outsideScope.setId(80L);
        outsideScope.setBranchId(23L);
        outsideScope.setName("其他校区学员");
        var insideScope = new Student();
        insideScope.setId(9007199254740993124L);
        insideScope.setBranchId(22L);
        insideScope.setName("可见学员");
        var relation = new StudentGuardianRelation();
        relation.setStudentId(insideScope.getId());
        var students =
                stub(
                        StudentMapper.class,
                        Map.of("selectBatchIds", List.of(outsideScope, insideScope)));
        var guardians = stub(GuardianMapper.class, Map.of("selectList", List.of(guardian)));
        var relations =
                stub(StudentGuardianRelationMapper.class, Map.of("selectList", List.of(relation)));
        var service =
                new SearchService(
                        students,
                        guardians,
                        relations,
                        org.mockito.Mockito.mock(BranchDirectory.class),
                        stub(ClassGroupMapper.class, Map.of()),
                        stub(LessonMapper.class, Map.of()),
                        new BranchAccessGuard());

        var hits = service.search("林", Set.of("guardian"), 20);
        assertEquals(1, hits.size());
        assertEquals(
                "/students?openId=9007199254740993124&openGuardianId=9007199254740993123",
                hits.get(0).getUrl());
        assertEquals("13800000000 · 学员: 可见学员", hits.get(0).getSubtitle());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void usersWithoutBranchesCannotReadLinkedOrUnlinkedGuardianContacts(boolean linked) {
        var user =
                new CustomUserDetails(
                        1L,
                        1L,
                        "teacher",
                        "老师",
                        "",
                        List.of(),
                        Set.of("TEACHER"),
                        List.of("search:read"),
                        1,
                        true);
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        var guardian = new Guardian();
        guardian.setId(91L);
        guardian.setName("林家长");
        guardian.setPhone("13800000000");
        var student = new Student();
        student.setId(92L);
        student.setBranchId(22L);
        student.setName("不可见学员");
        var relation = new StudentGuardianRelation();
        relation.setStudentId(student.getId());
        var service =
                new SearchService(
                        stub(StudentMapper.class, Map.of("selectBatchIds", List.of(student))),
                        stub(GuardianMapper.class, Map.of("selectList", List.of(guardian))),
                        stub(
                                StudentGuardianRelationMapper.class,
                                Map.of("selectList", linked ? List.of(relation) : List.of())),
                        org.mockito.Mockito.mock(BranchDirectory.class),
                        stub(ClassGroupMapper.class, Map.of()),
                        stub(LessonMapper.class, Map.of()),
                        new BranchAccessGuard());

        assertEquals(List.of(), service.search("林", Set.of("guardian"), 20));
    }

    private static <T> T stub(Class<T> mapper, Map<String, Object> responses) {
        return mapper.cast(
                Proxy.newProxyInstance(
                        mapper.getClassLoader(),
                        new Class<?>[] {mapper},
                        (proxy, method, args) -> {
                            if (!responses.containsKey(method.getName())) {
                                throw new AssertionError(
                                        "Unexpected mapper call: " + method.getName());
                            }
                            return responses.get(method.getName());
                        }));
    }
}
