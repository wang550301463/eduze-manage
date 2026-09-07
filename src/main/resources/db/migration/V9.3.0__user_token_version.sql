-- Session invalidation support: bump token_version to revoke outstanding JWTs.
ALTER TABLE t_user
    ADD COLUMN token_version INT NOT NULL DEFAULT 1 COMMENT 'JWT session version' AFTER last_login_at;
