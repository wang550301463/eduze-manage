-- 投产/Compose 回归测试数据（可重复执行，幂等）
-- 密码 teacher@123 的 BCrypt（与 V9.2.0 一致）
-- 覆盖：老师、可用时段、学员、订阅、教室、分组、家长、本周课次与名单

-- ========== 老师 ==========
INSERT INTO t_user (id, tenant_id, branch_id, username, password_hash, name, status)
SELECT 1101,1,1,'teacher_zhang',
       '$2a$10$Vc9KSaM9T4nAJVhrG3kacu.930c1dN/FnftTYp7twKW2oaq.PF0iW','张老师',1
WHERE NOT EXISTS (SELECT 1 FROM t_user WHERE id=1101);
INSERT INTO t_user (id, tenant_id, branch_id, username, password_hash, name, status)
SELECT 1102,1,1,'teacher_wang',
       '$2a$10$Vc9KSaM9T4nAJVhrG3kacu.930c1dN/FnftTYp7twKW2oaq.PF0iW','王老师',1
WHERE NOT EXISTS (SELECT 1 FROM t_user WHERE id=1102);
INSERT INTO t_user (id, tenant_id, branch_id, username, password_hash, name, status)
SELECT 1103,1,1,'teacher_li',
       '$2a$10$Vc9KSaM9T4nAJVhrG3kacu.930c1dN/FnftTYp7twKW2oaq.PF0iW','李老师',1
WHERE NOT EXISTS (SELECT 1 FROM t_user WHERE id=1103);

INSERT INTO t_user_role (id, tenant_id, user_id, role_id)
SELECT 1101,1,1101,4 WHERE NOT EXISTS (SELECT 1 FROM t_user_role WHERE id=1101);
INSERT INTO t_user_role (id, tenant_id, user_id, role_id)
SELECT 1102,1,1102,4 WHERE NOT EXISTS (SELECT 1 FROM t_user_role WHERE id=1102);
INSERT INTO t_user_role (id, tenant_id, user_id, role_id)
SELECT 1103,1,1103,4 WHERE NOT EXISTS (SELECT 1 FROM t_user_role WHERE id=1103);

INSERT INTO t_user_branch (id, tenant_id, user_id, branch_id)
SELECT 1101,1,1101,1 WHERE NOT EXISTS (SELECT 1 FROM t_user_branch WHERE id=1101);
INSERT INTO t_user_branch (id, tenant_id, user_id, branch_id)
SELECT 1102,1,1102,1 WHERE NOT EXISTS (SELECT 1 FROM t_user_branch WHERE id=1102);
INSERT INTO t_user_branch (id, tenant_id, user_id, branch_id)
SELECT 1103,1,1103,1 WHERE NOT EXISTS (SELECT 1 FROM t_user_branch WHERE id=1103);

-- ========== 可用时段（含周一便于当日签到回归） ==========
INSERT INTO t_teacher_availability (id,tenant_id,branch_id,teacher_id,day_of_week,start_minute,end_minute,capacity,valid_from,status)
SELECT 230001,1,1,1101,6, 540, 630, 8, '2026-05-01', 1 WHERE NOT EXISTS (SELECT 1 FROM t_teacher_availability WHERE id=230001);
INSERT INTO t_teacher_availability (id,tenant_id,branch_id,teacher_id,day_of_week,start_minute,end_minute,capacity,valid_from,status)
SELECT 230002,1,1,1101,7, 540, 630, 8, '2026-05-01', 1 WHERE NOT EXISTS (SELECT 1 FROM t_teacher_availability WHERE id=230002);
INSERT INTO t_teacher_availability (id,tenant_id,branch_id,teacher_id,day_of_week,start_minute,end_minute,capacity,valid_from,status)
SELECT 230003,1,1,1102,6, 840, 960, 6, '2026-05-01', 1 WHERE NOT EXISTS (SELECT 1 FROM t_teacher_availability WHERE id=230003);
INSERT INTO t_teacher_availability (id,tenant_id,branch_id,teacher_id,day_of_week,start_minute,end_minute,capacity,valid_from,status)
SELECT 230004,1,1,1102,7, 840, 960, 6, '2026-05-01', 1 WHERE NOT EXISTS (SELECT 1 FROM t_teacher_availability WHERE id=230004);
INSERT INTO t_teacher_availability (id,tenant_id,branch_id,teacher_id,day_of_week,start_minute,end_minute,capacity,valid_from,status)
SELECT 230005,1,1,1103,5,1080,1200, 4, '2026-05-01', 1 WHERE NOT EXISTS (SELECT 1 FROM t_teacher_availability WHERE id=230005);
INSERT INTO t_teacher_availability (id,tenant_id,branch_id,teacher_id,day_of_week,start_minute,end_minute,capacity,valid_from,status)
SELECT 230006,1,1,1103,6,1080,1200, 4, '2026-05-01', 1 WHERE NOT EXISTS (SELECT 1 FROM t_teacher_availability WHERE id=230006);

-- ========== 教室（须在带 default_class_room_id 的时段之前） ==========
INSERT INTO t_class_room (id, tenant_id, branch_id, name, capacity, note)
SELECT 270001,1,1,'A101',12,'回归教室A' WHERE NOT EXISTS (SELECT 1 FROM t_class_room WHERE id=270001);
INSERT INTO t_class_room (id, tenant_id, branch_id, name, capacity, note)
SELECT 270002,1,1,'B201',10,'回归教室B' WHERE NOT EXISTS (SELECT 1 FROM t_class_room WHERE id=270002);

-- 周一 09:00-10:30（张老师）便于 2026-09-07 签到回归
INSERT INTO t_teacher_availability (id,tenant_id,branch_id,teacher_id,day_of_week,start_minute,end_minute,capacity,valid_from,status,default_class_room_id)
SELECT 230007,1,1,1101,1, 540, 630, 8, '2026-05-01', 1, 270001
WHERE NOT EXISTS (SELECT 1 FROM t_teacher_availability WHERE id=230007);

UPDATE t_teacher_availability SET default_class_room_id=270001 WHERE id IN (230001,230002,230007) AND (default_class_room_id IS NULL OR default_class_room_id=0);
UPDATE t_teacher_availability SET default_class_room_id=270002 WHERE id IN (230003,230004,230005,230006) AND (default_class_room_id IS NULL OR default_class_room_id=0);

-- ========== 分组标签 ==========
INSERT INTO t_class_group (id, tenant_id, branch_id, name, course_id, head_teacher_id, capacity, status, tag_color)
SELECT 280001,1,1,'启蒙一组',220001,1101,10,1,'#C45C26'
WHERE NOT EXISTS (SELECT 1 FROM t_class_group WHERE id=280001);
INSERT INTO t_class_group (id, tenant_id, branch_id, name, course_id, head_teacher_id, capacity, status, tag_color)
SELECT 280002,1,1,'探索一组',220002,1102,10,1,'#2F6F4E'
WHERE NOT EXISTS (SELECT 1 FROM t_class_group WHERE id=280002);

-- ========== 学员 ==========
INSERT INTO t_student (id,tenant_id,branch_id,enroll_no,name,gender,birthday,enroll_date,status,mentor_teacher_id,current_stage_id)
SELECT 240001,1,1,'DEMO0001','小启', 1,'2021-06-01','2026-05-01',1, 1101, 200001
WHERE NOT EXISTS (SELECT 1 FROM t_student WHERE id=240001);
INSERT INTO t_student (id,tenant_id,branch_id,enroll_no,name,gender,birthday,enroll_date,status,mentor_teacher_id,current_stage_id)
SELECT 240002,1,1,'DEMO0002','小探', 2,'2018-06-01','2026-05-01',1, 1101, 200002
WHERE NOT EXISTS (SELECT 1 FROM t_student WHERE id=240002);
INSERT INTO t_student (id,tenant_id,branch_id,enroll_no,name,gender,birthday,enroll_date,status,mentor_teacher_id,current_stage_id)
SELECT 240003,1,1,'DEMO0003','小成', 1,'2016-06-01','2026-05-01',1, 1102, 200003
WHERE NOT EXISTS (SELECT 1 FROM t_student WHERE id=240003);
INSERT INTO t_student (id,tenant_id,branch_id,enroll_no,name,gender,birthday,enroll_date,status,mentor_teacher_id,current_stage_id)
SELECT 240004,1,1,'DEMO0004','小进', 2,'2014-06-01','2026-05-01',1, 1102, 200004
WHERE NOT EXISTS (SELECT 1 FROM t_student WHERE id=240004);
INSERT INTO t_student (id,tenant_id,branch_id,enroll_no,name,gender,birthday,enroll_date,status,mentor_teacher_id,current_stage_id)
SELECT 240005,1,1,'DEMO0005','小辨', 1,'2012-06-01','2026-05-01',1, 1103, 200005
WHERE NOT EXISTS (SELECT 1 FROM t_student WHERE id=240005);

INSERT INTO t_student_class_group (id, tenant_id, student_id, class_group_id)
SELECT 290001,1,240001,280001 WHERE NOT EXISTS (SELECT 1 FROM t_student_class_group WHERE id=290001);
INSERT INTO t_student_class_group (id, tenant_id, student_id, class_group_id)
SELECT 290002,1,240002,280001 WHERE NOT EXISTS (SELECT 1 FROM t_student_class_group WHERE id=290002);
INSERT INTO t_student_class_group (id, tenant_id, student_id, class_group_id)
SELECT 290003,1,240003,280002 WHERE NOT EXISTS (SELECT 1 FROM t_student_class_group WHERE id=290003);
INSERT INTO t_student_class_group (id, tenant_id, student_id, class_group_id)
SELECT 290004,1,240004,280002 WHERE NOT EXISTS (SELECT 1 FROM t_student_class_group WHERE id=290004);
INSERT INTO t_student_class_group (id, tenant_id, student_id, class_group_id)
SELECT 290005,1,240005,280002 WHERE NOT EXISTS (SELECT 1 FROM t_student_class_group WHERE id=290005);

-- ========== 家长 ==========
INSERT INTO t_guardian (id, tenant_id, name, phone, is_main_contact, can_pickup, qr_code)
SELECT 300001,1,'启爸','13900000001',1,1,'QR-DEMO-0001'
WHERE NOT EXISTS (SELECT 1 FROM t_guardian WHERE id=300001);
INSERT INTO t_guardian (id, tenant_id, name, phone, is_main_contact, can_pickup, qr_code)
SELECT 300002,1,'探妈','13900000002',1,1,'QR-DEMO-0002'
WHERE NOT EXISTS (SELECT 1 FROM t_guardian WHERE id=300002);
INSERT INTO t_guardian (id, tenant_id, name, phone, is_main_contact, can_pickup, qr_code)
SELECT 300003,1,'成爸','13900000003',1,1,'QR-DEMO-0003'
WHERE NOT EXISTS (SELECT 1 FROM t_guardian WHERE id=300003);

INSERT INTO t_student_guardian_relation (id, tenant_id, student_id, guardian_id, relation)
SELECT 310001,1,240001,300001,'父亲' WHERE NOT EXISTS (SELECT 1 FROM t_student_guardian_relation WHERE id=310001);
INSERT INTO t_student_guardian_relation (id, tenant_id, student_id, guardian_id, relation)
SELECT 310002,1,240002,300002,'母亲' WHERE NOT EXISTS (SELECT 1 FROM t_student_guardian_relation WHERE id=310002);
INSERT INTO t_student_guardian_relation (id, tenant_id, student_id, guardian_id, relation)
SELECT 310003,1,240003,300003,'父亲' WHERE NOT EXISTS (SELECT 1 FROM t_student_guardian_relation WHERE id=310003);

-- ========== 订阅 ==========
INSERT INTO t_lesson_subscription (id,tenant_id,branch_id,student_id,teacher_id,teacher_availability_id,valid_from,status,source)
SELECT 250001,1,1,240001,1101,230001,'2026-05-01',1,'NORMAL' WHERE NOT EXISTS (SELECT 1 FROM t_lesson_subscription WHERE id=250001);
INSERT INTO t_lesson_subscription (id,tenant_id,branch_id,student_id,teacher_id,teacher_availability_id,valid_from,status,source)
SELECT 250002,1,1,240002,1101,230002,'2026-05-01',1,'NORMAL' WHERE NOT EXISTS (SELECT 1 FROM t_lesson_subscription WHERE id=250002);
INSERT INTO t_lesson_subscription (id,tenant_id,branch_id,student_id,teacher_id,teacher_availability_id,valid_from,status,source)
SELECT 250003,1,1,240003,1102,230003,'2026-05-01',1,'NORMAL' WHERE NOT EXISTS (SELECT 1 FROM t_lesson_subscription WHERE id=250003);
INSERT INTO t_lesson_subscription (id,tenant_id,branch_id,student_id,teacher_id,teacher_availability_id,valid_from,status,source)
SELECT 250004,1,1,240004,1102,230004,'2026-05-01',1,'NORMAL' WHERE NOT EXISTS (SELECT 1 FROM t_lesson_subscription WHERE id=250004);
INSERT INTO t_lesson_subscription (id,tenant_id,branch_id,student_id,teacher_id,teacher_availability_id,valid_from,status,source)
SELECT 250005,1,1,240005,1103,230005,'2026-05-01',1,'NORMAL' WHERE NOT EXISTS (SELECT 1 FROM t_lesson_subscription WHERE id=250005);
INSERT INTO t_lesson_subscription (id,tenant_id,branch_id,student_id,teacher_id,teacher_availability_id,valid_from,status,source)
SELECT 250006,1,1,240001,1101,230007,'2026-05-01',1,'NORMAL' WHERE NOT EXISTS (SELECT 1 FROM t_lesson_subscription WHERE id=250006);
INSERT INTO t_lesson_subscription (id,tenant_id,branch_id,student_id,teacher_id,teacher_availability_id,valid_from,status,source)
SELECT 250007,1,1,240002,1101,230007,'2026-05-01',1,'NORMAL' WHERE NOT EXISTS (SELECT 1 FROM t_lesson_subscription WHERE id=250007);

INSERT INTO t_student_mentor_history (id,tenant_id,branch_id,student_id,from_teacher_id,to_teacher_id,reason)
SELECT 260001,1,1,240001,NULL,1101,'首次绑定' WHERE NOT EXISTS (SELECT 1 FROM t_student_mentor_history WHERE id=260001);
INSERT INTO t_student_mentor_history (id,tenant_id,branch_id,student_id,from_teacher_id,to_teacher_id,reason)
SELECT 260002,1,1,240002,NULL,1101,'首次绑定' WHERE NOT EXISTS (SELECT 1 FROM t_student_mentor_history WHERE id=260002);
INSERT INTO t_student_mentor_history (id,tenant_id,branch_id,student_id,from_teacher_id,to_teacher_id,reason)
SELECT 260003,1,1,240003,NULL,1102,'首次绑定' WHERE NOT EXISTS (SELECT 1 FROM t_student_mentor_history WHERE id=260003);
INSERT INTO t_student_mentor_history (id,tenant_id,branch_id,student_id,from_teacher_id,to_teacher_id,reason)
SELECT 260004,1,1,240004,NULL,1102,'首次绑定' WHERE NOT EXISTS (SELECT 1 FROM t_student_mentor_history WHERE id=260004);
INSERT INTO t_student_mentor_history (id,tenant_id,branch_id,student_id,from_teacher_id,to_teacher_id,reason)
SELECT 260005,1,1,240005,NULL,1103,'首次绑定' WHERE NOT EXISTS (SELECT 1 FROM t_student_mentor_history WHERE id=260005);

-- ========== 本周课次（2026-09-07 周一 + 2026-09-12 周六） ==========
INSERT INTO t_lesson (id,tenant_id,branch_id,class_group_id,class_room_id,teacher_id,start_at,end_at,status,teacher_availability_id,source)
SELECT 320001,1,1,280001,270001,1101,'2026-09-07 09:00:00.000','2026-09-07 10:30:00.000','SCHEDULED',230007,1
WHERE NOT EXISTS (SELECT 1 FROM t_lesson WHERE id=320001);
INSERT INTO t_lesson (id,tenant_id,branch_id,class_group_id,class_room_id,teacher_id,start_at,end_at,status,teacher_availability_id,source)
SELECT 320002,1,1,280001,270001,1101,'2026-09-12 09:00:00.000','2026-09-12 10:30:00.000','SCHEDULED',230001,1
WHERE NOT EXISTS (SELECT 1 FROM t_lesson WHERE id=320002);
INSERT INTO t_lesson (id,tenant_id,branch_id,class_group_id,class_room_id,teacher_id,start_at,end_at,status,teacher_availability_id,source)
SELECT 320003,1,1,280002,270002,1102,'2026-09-12 14:00:00.000','2026-09-12 16:00:00.000','SCHEDULED',230003,1
WHERE NOT EXISTS (SELECT 1 FROM t_lesson WHERE id=320003);

INSERT INTO t_lesson_student (id,tenant_id,branch_id,lesson_id,student_id,subscription_id,source,status)
SELECT 330001,1,1,320001,240001,250006,'SUBSCRIPTION','BOOKED'
WHERE NOT EXISTS (SELECT 1 FROM t_lesson_student WHERE id=330001);
INSERT INTO t_lesson_student (id,tenant_id,branch_id,lesson_id,student_id,subscription_id,source,status)
SELECT 330002,1,1,320001,240002,250007,'SUBSCRIPTION','BOOKED'
WHERE NOT EXISTS (SELECT 1 FROM t_lesson_student WHERE id=330002);
INSERT INTO t_lesson_student (id,tenant_id,branch_id,lesson_id,student_id,subscription_id,source,status)
SELECT 330003,1,1,320002,240001,250001,'SUBSCRIPTION','BOOKED'
WHERE NOT EXISTS (SELECT 1 FROM t_lesson_student WHERE id=330003);
INSERT INTO t_lesson_student (id,tenant_id,branch_id,lesson_id,student_id,subscription_id,source,status)
SELECT 330004,1,1,320003,240003,250003,'SUBSCRIPTION','BOOKED'
WHERE NOT EXISTS (SELECT 1 FROM t_lesson_student WHERE id=330004);
