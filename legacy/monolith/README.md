# 原单体基线归档

此目录保留拆分前的后端实现与原测试，用于行为比较，不参与平台 Maven 聚合构建，不连接新服务数据库。原部署文件仍在 docker/ 下，仅供旧环境查阅。

从仓库根目录运行旧单元测试：`./mvnw -f legacy/monolith/pom.xml -Dskip.frontend.build=true test`。跨域集成测试需要单独的旧版测试数据库，不能指向平台服务库。首发回归以 services/ 的测试和平台验收脚本为准。
