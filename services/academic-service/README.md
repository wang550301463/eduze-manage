# 教务服务

独立拥有学员、家庭授权、课程报名目录、固定班、授课时段、课次名单、签到、请假、课时包和不可变流水。身份资料通过 typed HTTP directory 读取；不依赖身份服务实体或 Mapper，不查询身份/教学数据库。多学生/课次显示使用批量目录和请求内缓存。

配置：`ACADEMIC_DB_URL`、`ACADEMIC_DB_USER`、`ACADEMIC_DB_PASSWORD`；`ACADEMIC_REDIS_USER/PASSWORD`；`ACADEMIC_SERVICE_TOKEN`；运行时身份服务地址、调用方 token。该服务不需要 JWT 签名密钥。Redis keys 使用 `academic:`。Flyway 包含自身迁移和 `db/runtime`。

家庭 API 前缀 `/api/v1/academic/family`：员工创建 `invites {studentId}`，家长 `bindings {code}` 申请，员工 `bindings/{id}/approve` 确认；DELETE 立即撤销。邀请只存 SHA-256 摘要，24 小时有效、单人领取、重复请求幂等。未审批、过期或其他家庭不能访问学生。

`children` 返回已批准的孩子；`children/{id}/schedule?from&to`、`balance`、`leaves` 提供自助服务。请假 POST 需要 `Idempotency-Key`，仅能选择孩子自己的未来课次。

内部访问以真实转发 Bearer 为准：学生及班级 access/batch-access、班级 students、lessons/validate。批量 access 返回每个 ID 的 allowed（最多 200），授权失败不泄漏对象详情。通知与作品服务专用 recipients 只返回当前有效家长，按内部服务凭证和单机构配置校验。

签到在本地事务内校验课次名单和校区，按最早到期原则扣除一节有效课时；course_id 为空的课时包通用，否则需匹配课次课程。试听 source=4 不扣课。重复/并发签到只能成功一次；余额不足整笔回滚。VOID 只能冲正本学生的 ATTEND、由原流水计算恢复额度并恢复原课时包，不能重复冲正。分钟字段表示实际教学时长；`remainingLessonsAfter` 表示准确的权益余额，旧 `balanceAfterMinutes` 不再用 60 分钟假设折算。

事务 Outbox 事件：`academic.lesson-changed`（lessonId/revision/status/startTime/endTime/studentIds）、`academic.leave-approved`、`academic.family-authorization-changed`；通知失败不回滚已提交教务。

测试覆盖独立 MySQL/Redis 服务启动、无身份表、家长 HTTP 权限、邀请审批及实时撤销、教师授课关系、并发签到、课程匹配、单次冲正。旧跨领域 IT 留在 monolith 作为历史基线；不声称已完成所有旧端到端回归。

## 招生与订单集成

`POST /internal/academic/trial-reservations` 只接受 engagement 服务；以真实课次 ID 锁定名额，创建试听学员及 TRIAL 名单。标准班级加人和手动课次加人使用相同课次容量锁；取消操作释放名单名额，重复预约 ID 不生成新学员。GET 单个预约和 POST cancel 支持超时对账与释放。

`commerce.order-paid` 只发放课程权益，以原订单 ID 唯一约束去重，不占班级名额；校验学员、校区、课程。成功与语义失败分别输出 `academic.entitlement-issued` / `academic.entitlement-rejected`。

退款 freeze/complete/release 只接受 commerce。冻结原订单准确课时，签到按 remaining - frozen 校验，退款失败释放可用权益，成功后扣除原订单冻结额度并追加不可变 ORDER_REFUND 流水。COMPLETE 与 RELEASE 为终态，晚到请求不可复活已退款额度。家长余额与 PC 课时包展示均扣除冻结部分；`lessonUnitsDelta` 单独记录权益变化，分钟字段不承担货币或权益换算。

### 试听转正式报名

`POST /internal/academic/trial-reservations/{id}/enroll` 接收 `{enquiryId,enrollmentId,classGroupId}`，仅 engagement 服务可调用，必须转发具备 `classgroup:assign` 权限的员工 Bearer 身份。指定班级须与试听同校区并有名额；教务在本地事务创建真实班级成员和课表订阅，再写入 `academic.enrollment-confirmed` Outbox 事件。相同报名请求重放不会重复建成员或发事件。已取消预约不可报名，已报名预约不可走试听取消接口。

### 请假与名单提醒边界

有 `lessonId` 的请假仅匹配该课次；无 `lessonId` 才按日期范围。审批遇到已有非请假考勤记录时返回冲突，要求先走明确的考勤纠正流程，避免覆盖签到和课时流水。签到、今日名单与缺勤任务使用同一课次匹配规则。

名单变更在本地事务内递增课次版本并发布完整 `academic.lesson-changed` 名单快照。通知服务发送前可调用 `GET /internal/academic/lessons/{lessonId}/students/{studentId}/notification-access`（仅 notification 服务凭证），返回 `{allowed,revision,startAt}`，用于拒绝被移除学生、取消课次和过期版本提醒。
