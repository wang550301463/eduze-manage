-- 课程顾问不应查看/管理校区列表（与 BranchControllerIT、投产 RBAC 一致）
DELETE FROM t_role_permission WHERE role_id = 3 AND permission_id = 1;
