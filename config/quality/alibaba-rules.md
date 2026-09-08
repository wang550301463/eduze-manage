# 阿里 Java 开发规范检查映射

基线为阿里《Java 开发手册》黄山版。P3C 官方地址：https://github.com/alibaba/p3c 。本项目使用 Java 17 兼容的 Checkstyle 10.21.2、PMD 7.7.0、SpotBugs 4.8.6；不是宣称这些规则完整等同于 P3C 或手册全部强制项。

## 自动门禁

- 命名、常量大写、数组声明、单行多语句、Tab、禁止内部 JDK 包：Checkstyle，配置 checkstyle.xml。
- equals/hashCode 一致、资源关闭、finally 返回、直接调用 Thread.run、System.out、printStackTrace：PMD，配置 pmd.xml。
- 高置信度字节码缺陷：SpotBugs；所有高优先级发现必须处理。
- 格式：Spotless + google-java-format AOSP；固定版本。
- Controller 不直接使用 Mapper/JDBC、不得依赖其他服务实现：ArchUnit 与 scripts/check-boundaries.py 双检查。
- 数据库 URL/账号唯一、内部端口不暴露、旧卷不复用：scripts/tests/test_platform_topology.py；运行环境还要执行账号授权负向测试。
- TypeScript、ESLint、自动化业务测试、集成测试：scripts/verify-platform.sh。
- 依赖漏洞与密钥：scripts/security-platform.sh；无法拉取漏洞库视为检查失败，不等同于无漏洞。

## 每次评审必须核查的手工强制项

- 命名表达业务含义；类/接口、公开方法、复杂逻辑有必要的说明；注释与实现一致。
- DTO 与数据库对象分离；Controller 无事务规则；持久化及业务状态变更位于应用服务。原兼容接口应逐个评审，不能借“兼容”跳过权限。
- 异常有业务语义、统一错误响应；日志参数化；不输出口令、Token、完整手机号、微信密钥、媒体签名 URL。
- 线程池有界，超时与重试有上限，资源有关闭路径，ThreadLocal 在 finally 清理。
- SQL 显式字段，必需索引与分页上限；数据归属与机构/校区校验；服务数据库账号只有本库权限。
- 金额使用整型最小货币单位并检查溢出，或精确十进制；课时和资金流水只追加，纠错用冲正。
- 不用浮点数处理交易；不以分布式缓存读模型作扣课与授权的最终依据。
- 事件生产与本地业务同一事务，Inbox 与消费效果同一事务，重复/乱序/未知结果有明确语义。
- 生产依赖无 SNAPSHOT、LATEST、RELEASE 与版本范围；镜像发布记录 digest，数据库迁移只向前兼容。

## 项目补充（不冒称阿里手册条文）

跨服务无实现依赖、无跨库 SQL/事务；独立服务账号；Outbox/Inbox；父母实时授权；签名链接最长五分钟；付款与权益状态分离。服务级循环同步依赖应在接口评审时避免，调用链有超时预算。

## 例外登记

适用强制项原则上清零。例外只能按单条规则登记：规则、定位、理由、负责人、到期日、跟踪任务与验证证据。不能关闭整个扫描器或整套规则来通过验收。当前未登记生产强制项豁免。原单体源码在 legacy/monolith 中，仅作历史回归，不能进入新镜像。前端原有四项 hooks/refresh 警告保留原 lint 上限5，新增代码不得增加此基线。

静态扫描通过只证明所选自动规则通过；正式发布仍需上述人工评审与外部接入验收记录。
