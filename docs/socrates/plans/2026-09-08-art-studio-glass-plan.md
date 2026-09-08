# 美术宝 · 轻透工作空间实施计划

Goal: 将已确认的柔和半透明 Mac 风格落实到运行中的 Demo，同时打通现有业务中的搜索、课表、请假路径。

Architecture: 保留 React / Spring Boot 和现有权限、业务数据；共享设计变量与壳层，按日常教学和机构管理组织工作入口。新增业务数据之前不伪造家长端、作品或经营指标。

Tech Stack: React、TypeScript、Tailwind、Radix、TanStack Query、Vitest；Maven、Docker Compose、Nginx HTTPS。

## 设计依据

- [Apple Materials](https://developer.apple.com/design/human-interface-guidelines/materials)：将半透明材质用于导航、工具栏等操作层，内容层确保对比度。采用柔和环境色、白色磨砂界面和克制的陶土色强调。
- [Apple Sidebars](https://developer.apple.com/design/human-interface-guidelines/sidebars)：稳定、分组的导航；当前页清晰可辨。
- [Apple Split views](https://developer.apple.com/design/human-interface-guidelines/split-views)：列表与详情保持上下文，小屏使用独立抽屉，键盘可以进入和退出。

## 实施与验收

- [x] 1. 共享视觉：globals.css、基础控件、对话框与抽屉；建立环境背景、磨砂导航、白色阅读面；支持减少动态效果与小屏。
- [x] 2. 应用壳层：品牌、分组导航、上下文工具栏；移除无效通知按钮，收起导航仍有可访问名称。
- [x] 3. 登录体验：双栏艺术构图、清楚的登录表单与错误反馈；窄屏优先登录。
- [x] 4. 工作台：真实校区指标、今日/下一课、教学快捷入口；按权限和角色呈现，空态与错误分开。
- [x] 5. 全局搜索：正确实体深链接、异步竞态处理、可访问标题；路由测试。
- [x] 6. 课表详情：激活实际挂载的教师课表、支持键盘与深链接、按课次查真实名单；范围回归测试。
- [x] 7. 请假流程：按姓名选学员、日期校验、学员内真实请假历史；范围与日期回归测试。
- [x] 8. 复核与部署：独立规格/代码审查、前端测试/构建、Maven打包、保留现有 Docker 数据挂载重新部署。
- [x] 9. 浏览器验证：HTTPS 无证书错误；新视觉肉眼可辨；桌面/窄屏、搜索到详情、课次名单、请假日期与返回路径；记录证据。

本次变更保持现有登录和后端权限。四端完整产品规划另见 docs/2026-09-08-art-platform-product-analysis.md；本阶段落实机构/教师业务界面的视觉与流程，不将不存在的作品、家长消息或成长数据展示为已实现。
