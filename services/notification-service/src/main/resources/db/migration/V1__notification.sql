CREATE TABLE notification_message (
 id VARCHAR(36) NOT NULL PRIMARY KEY,tenant_id VARCHAR(36) NOT NULL,branch_id VARCHAR(36) NOT NULL,
 recipient_id VARCHAR(36) NOT NULL,student_id VARCHAR(36) NOT NULL,title VARCHAR(120) NOT NULL,body VARCHAR(1000) NOT NULL,
 business_type VARCHAR(40) NOT NULL,business_id VARCHAR(36) NOT NULL,aggregate_key VARCHAR(100) NOT NULL,
 path VARCHAR(300) NOT NULL,status VARCHAR(20) NOT NULL,attempts INT NOT NULL DEFAULT 0,
 next_at DATETIME(6) NOT NULL,lease_until DATETIME(6),expires_at DATETIME(6),last_error VARCHAR(80),
 visible TINYINT NOT NULL DEFAULT 1,read_at DATETIME(6),confirmed_at DATETIME(6),created_at DATETIME(6) NOT NULL,
 KEY idx_notification_inbox(tenant_id,recipient_id,visible,created_at),KEY idx_notification_due(status,next_at),
 KEY idx_notification_aggregate(tenant_id,aggregate_key),KEY idx_notification_family(tenant_id,student_id,recipient_id)
) COMMENT='Delivery status differs from user read and business confirmation';
CREATE TABLE notification_subscription (
 tenant_id VARCHAR(36) NOT NULL,user_id VARCHAR(36) NOT NULL,template_key VARCHAR(40) NOT NULL,accepted TINYINT NOT NULL,
 PRIMARY KEY(tenant_id,user_id,template_key)
) COMMENT='User subscription intent; actual delivery eligibility is decided by WeChat';
CREATE TABLE notification_cursor (
 tenant_id VARCHAR(36) NOT NULL,aggregate_key VARCHAR(100) NOT NULL,revision BIGINT NOT NULL,
 PRIMARY KEY(tenant_id,aggregate_key)
) COMMENT='Monotonic event revisions reject out-of-order schedules and publications';
