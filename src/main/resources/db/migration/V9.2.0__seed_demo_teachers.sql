-- ============================================================================
-- V9.2.0 Dev profile 示例老师 / 学员 / 订阅
-- 仅当 Flyway placeholder ${demoSeed}='true' 时执行（dev/test profile）
-- ============================================================================

-- BCrypt for "teacher@123"，使用与 admin seed 同套 cost=10
SET @demo := '${demoSeed}';

-- 3 个示例老师用户
INSERT INTO t_user (id, tenant_id, branch_id, username, password_hash, name, status)
SELECT 1101,1,1,'teacher_zhang',
       '$2a$10$Vc9KSaM9T4nAJVhrG3kacu.930c1dN/FnftTYp7twKW2oaq.PF0iW','张老师',1
WHERE '${demoSeed}' = 'true' AND NOT EXISTS (SELECT 1 FROM t_user WHERE id=1101);

INSERT INTO t_user (id, tenant_id, branch_id, username, password_hash, name, status)
SELECT 1102,1,1,'teacher_wang',
       '$2a$10$Vc9KSaM9T4nAJVhrG3kacu.930c1dN/FnftTYp7twKW2oaq.PF0iW','王老师',1
WHERE '${demoSeed}' = 'true' AND NOT EXISTS (SELECT 1 FROM t_user WHERE id=1102);

INSERT INTO t_user (id, tenant_id, branch_id, username, password_hash, name, status)
SELECT 1103,1,1,'teacher_li',
       '$2a$10$Vc9KSaM9T4nAJVhrG3kacu.930c1dN/FnftTYp7twKW2oaq.PF0iW','李老师',1
WHERE '${demoSeed}' = 'true' AND NOT EXISTS (SELECT 1 FROM t_user WHERE id=1103);

-- 用户角色（TEACHER=4）
INSERT INTO t_user_role (id, tenant_id, user_id, role_id)
SELECT 1101,1,1101,4 WHERE '${demoSeed}' = 'true' AND NOT EXISTS (SELECT 1 FROM t_user_role WHERE id=1101);
INSERT INTO t_user_role (id, tenant_id, user_id, role_id)
SELECT 1102,1,1102,4 WHERE '${demoSeed}' = 'true' AND NOT EXISTS (SELECT 1 FROM t_user_role WHERE id=1102);
INSERT INTO t_user_role (id, tenant_id, user_id, role_id)
SELECT 1103,1,1103,4 WHERE '${demoSeed}' = 'true' AND NOT EXISTS (SELECT 1 FROM t_user_role WHERE id=1103);

-- user_branch
INSERT INTO t_user_branch (id, tenant_id, user_id, branch_id)
SELECT 1101,1,1101,1 WHERE '${demoSeed}' = 'true' AND NOT EXISTS (SELECT 1 FROM t_user_branch WHERE id=1101);
INSERT INTO t_user_branch (id, tenant_id, user_id, branch_id)
SELECT 1102,1,1102,1 WHERE '${demoSeed}' = 'true' AND NOT EXISTS (SELECT 1 FROM t_user_branch WHERE id=1102);
INSERT INTO t_user_branch (id, tenant_id, user_id, branch_id)
SELECT 1103,1,1103,1 WHERE '${demoSeed}' = 'true' AND NOT EXISTS (SELECT 1 FROM t_user_branch WHERE id=1103);

-- 每人 2 条 availability
INSERT INTO t_teacher_availability (id,tenant_id,branch_id,teacher_id,day_of_week,start_minute,end_minute,capacity,valid_from,status)
SELECT 230001,1,1,1101,6, 540, 630, 8, '2026-05-01', 1 WHERE '${demoSeed}' = 'true' AND NOT EXISTS (SELECT 1 FROM t_teacher_availability WHERE id=230001);
INSERT INTO t_teacher_availability (id,tenant_id,branch_id,teacher_id,day_of_week,start_minute,end_minute,capacity,valid_from,status)
SELECT 230002,1,1,1101,7, 540, 630, 8, '2026-05-01', 1 WHERE '${demoSeed}' = 'true' AND NOT EXISTS (SELECT 1 FROM t_teacher_availability WHERE id=230002);
INSERT INTO t_teacher_availability (id,tenant_id,branch_id,teacher_id,day_of_week,start_minute,end_minute,capacity,valid_from,status)
SELECT 230003,1,1,1102,6, 840, 960, 6, '2026-05-01', 1 WHERE '${demoSeed}' = 'true' AND NOT EXISTS (SELECT 1 FROM t_teacher_availability WHERE id=230003);
INSERT INTO t_teacher_availability (id,tenant_id,branch_id,teacher_id,day_of_week,start_minute,end_minute,capacity,valid_from,status)
SELECT 230004,1,1,1102,7, 840, 960, 6, '2026-05-01', 1 WHERE '${demoSeed}' = 'true' AND NOT EXISTS (SELECT 1 FROM t_teacher_availability WHERE id=230004);
INSERT INTO t_teacher_availability (id,tenant_id,branch_id,teacher_id,day_of_week,start_minute,end_minute,capacity,valid_from,status)
SELECT 230005,1,1,1103,5,1080,1200, 4, '2026-05-01', 1 WHERE '${demoSeed}' = 'true' AND NOT EXISTS (SELECT 1 FROM t_teacher_availability WHERE id=230005);
INSERT INTO t_teacher_availability (id,tenant_id,branch_id,teacher_id,day_of_week,start_minute,end_minute,capacity,valid_from,status)
SELECT 230006,1,1,1103,6,1080,1200, 4, '2026-05-01', 1 WHERE '${demoSeed}' = 'true' AND NOT EXISTS (SELECT 1 FROM t_teacher_availability WHERE id=230006);

-- 5 个示例学员（每阶段 1）
INSERT INTO t_student (id,tenant_id,branch_id,enroll_no,name,gender,birthday,enroll_date,status,mentor_teacher_id,current_stage_id)
SELECT 240001,1,1,'DEMO0001','小启', 1,'2021-06-01','2026-05-01',1, 1101, 200001
WHERE '${demoSeed}' = 'true' AND NOT EXISTS (SELECT 1 FROM t_student WHERE id=240001);
INSERT INTO t_student (id,tenant_id,branch_id,enroll_no,name,gender,birthday,enroll_date,status,mentor_teacher_id,current_stage_id)
SELECT 240002,1,1,'DEMO0002','小探', 2,'2018-06-01','2026-05-01',1, 1101, 200002
WHERE '${demoSeed}' = 'true' AND NOT EXISTS (SELECT 1 FROM t_student WHERE id=240002);
INSERT INTO t_student (id,tenant_id,branch_id,enroll_no,name,gender,birthday,enroll_date,status,mentor_teacher_id,current_stage_id)
SELECT 240003,1,1,'DEMO0003','小成', 1,'2016-06-01','2026-05-01',1, 1102, 200003
WHERE '${demoSeed}' = 'true' AND NOT EXISTS (SELECT 1 FROM t_student WHERE id=240003);
INSERT INTO t_student (id,tenant_id,branch_id,enroll_no,name,gender,birthday,enroll_date,status,mentor_teacher_id,current_stage_id)
SELECT 240004,1,1,'DEMO0004','小进', 2,'2014-06-01','2026-05-01',1, 1102, 200004
WHERE '${demoSeed}' = 'true' AND NOT EXISTS (SELECT 1 FROM t_student WHERE id=240004);
INSERT INTO t_student (id,tenant_id,branch_id,enroll_no,name,gender,birthday,enroll_date,status,mentor_teacher_id,current_stage_id)
SELECT 240005,1,1,'DEMO0005','小辨', 1,'2012-06-01','2026-05-01',1, 1103, 200005
WHERE '${demoSeed}' = 'true' AND NOT EXISTS (SELECT 1 FROM t_student WHERE id=240005);

-- 每个学员订阅其主带的第 1 条可用时段
INSERT INTO t_lesson_subscription (id,tenant_id,branch_id,student_id,teacher_id,teacher_availability_id,valid_from,status,source)
SELECT 250001,1,1,240001,1101,230001,'2026-05-01',1,'NORMAL' WHERE '${demoSeed}' = 'true' AND NOT EXISTS (SELECT 1 FROM t_lesson_subscription WHERE id=250001);
INSERT INTO t_lesson_subscription (id,tenant_id,branch_id,student_id,teacher_id,teacher_availability_id,valid_from,status,source)
SELECT 250002,1,1,240002,1101,230002,'2026-05-01',1,'NORMAL' WHERE '${demoSeed}' = 'true' AND NOT EXISTS (SELECT 1 FROM t_lesson_subscription WHERE id=250002);
INSERT INTO t_lesson_subscription (id,tenant_id,branch_id,student_id,teacher_id,teacher_availability_id,valid_from,status,source)
SELECT 250003,1,1,240003,1102,230003,'2026-05-01',1,'NORMAL' WHERE '${demoSeed}' = 'true' AND NOT EXISTS (SELECT 1 FROM t_lesson_subscription WHERE id=250003);
INSERT INTO t_lesson_subscription (id,tenant_id,branch_id,student_id,teacher_id,teacher_availability_id,valid_from,status,source)
SELECT 250004,1,1,240004,1102,230004,'2026-05-01',1,'NORMAL' WHERE '${demoSeed}' = 'true' AND NOT EXISTS (SELECT 1 FROM t_lesson_subscription WHERE id=250004);
INSERT INTO t_lesson_subscription (id,tenant_id,branch_id,student_id,teacher_id,teacher_availability_id,valid_from,status,source)
SELECT 250005,1,1,240005,1103,230005,'2026-05-01',1,'NORMAL' WHERE '${demoSeed}' = 'true' AND NOT EXISTS (SELECT 1 FROM t_lesson_subscription WHERE id=250005);

-- 主带历史（首次绑定）
INSERT INTO t_student_mentor_history (id,tenant_id,branch_id,student_id,from_teacher_id,to_teacher_id,reason)
SELECT 260001,1,1,240001,NULL,1101,'首次绑定' WHERE '${demoSeed}' = 'true' AND NOT EXISTS (SELECT 1 FROM t_student_mentor_history WHERE id=260001);
INSERT INTO t_student_mentor_history (id,tenant_id,branch_id,student_id,from_teacher_id,to_teacher_id,reason)
SELECT 260002,1,1,240002,NULL,1101,'首次绑定' WHERE '${demoSeed}' = 'true' AND NOT EXISTS (SELECT 1 FROM t_student_mentor_history WHERE id=260002);
INSERT INTO t_student_mentor_history (id,tenant_id,branch_id,student_id,from_teacher_id,to_teacher_id,reason)
SELECT 260003,1,1,240003,NULL,1102,'首次绑定' WHERE '${demoSeed}' = 'true' AND NOT EXISTS (SELECT 1 FROM t_student_mentor_history WHERE id=260003);
INSERT INTO t_student_mentor_history (id,tenant_id,branch_id,student_id,from_teacher_id,to_teacher_id,reason)
SELECT 260004,1,1,240004,NULL,1102,'首次绑定' WHERE '${demoSeed}' = 'true' AND NOT EXISTS (SELECT 1 FROM t_student_mentor_history WHERE id=260004);
INSERT INTO t_student_mentor_history (id,tenant_id,branch_id,student_id,from_teacher_id,to_teacher_id,reason)
SELECT 260005,1,1,240005,NULL,1103,'首次绑定' WHERE '${demoSeed}' = 'true' AND NOT EXISTS (SELECT 1 FROM t_student_mentor_history WHERE id=260005);
