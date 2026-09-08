# 身份服务

独立拥有机构、校区、员工、角色、权限、微信关联和会话。不会读取教务表。JWT 仅由该服务签发；其余服务通过内部 introspection 校验并获取当前数据库的权限、角色、校区，权限查询不使用延迟缓存。

配置：`IDENTITY_DB_URL`、`IDENTITY_DB_USER`、`IDENTITY_DB_PASSWORD`；`IDENTITY_REDIS_USER/PASSWORD`（也可 Spring 标准环境变量）；`JWT_SECRET`；`IDENTITY_SERVICE_TOKEN`；`eduze.runtime.callers.<service>`；`eduze.runtime.services.<service>`。Redis keys 均使用 `identity:` 前缀。Flyway 加载自身迁移和 `db/runtime`。

首次启动通过 `BOOTSTRAP_ADMIN_USERNAME` 和 `BOOTSTRAP_ADMIN_PASSWORD`（至少 12 位）创建 ID 1001 管理员；不植入通用密码。正式环境启用 `prod`。微信配置 `WECHAT_APP_ID`、`WECHAT_SECRET`，使用实际 code2Session，未配置返回 503。

旧 `/api/auth`、`/api/me`、`/api/users`、`/api/roles`、`/api/permissions`、`/api/branches`、`/api/teachers` 保持归属本服务。

- `POST /api/v1/identity/wechat/login {code}`：返回 LoginResponse。
- `POST /api/v1/identity/wechat/link {code}`：已登录员工凭微信码关联；若微信已有家长账号，保留原账号，单独记录员工 ID，不搬移家庭授权。
- `POST /api/v1/identity/switch-role {role:PARENT|STAFF}`：签发新令牌。PARENT 令牌无员工权限；STAFF 使用当前或已关联的员工账号。刷新保留选择的身份。
- `POST /internal/identity/introspect {token}`：原始 Actor，只接受配置的服务凭证。异常会话返回 401。
- 内部 users/branches 批量目录和 teachers 目录保留实时校区权限校验；通知服务有专用微信收件人接口。

已具备自动化：JWT 兼容、签名角色过期后读取当前授权、版本撤销/黑名单、父身份权限收窄；独立 MySQL + Redis 初始化和 HTTP 内部凭证边界。微信实际 AppID 联调仍需真实环境。
