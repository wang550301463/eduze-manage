ALTER TABLE wechat_identity ADD COLUMN staff_user_id BIGINT NULL, ADD UNIQUE KEY uk_wechat_staff(tenant_id,app_id,staff_user_id);
