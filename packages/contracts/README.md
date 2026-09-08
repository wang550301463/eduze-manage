# API 类型来源

`openapi/*.json` 来自各服务实际启动后的 OpenAPI 导出，`generated/*.ts` 由根任务维护的生成命令产生，禁止手改。

`index.ts`、`business.ts` 为 PC 和原生小程序共同消费的薄适配层：

- 教学主题、课件、班级主题、学期计划输入来自 teaching schemas；作品草稿、作品条目、课堂动态、展览输入来自 portfolio schemas。
- 商品、订单、退款来自 commerce schemas；画室、咨询、活动、报名和续费输入来自 engagement schemas。
- 家庭绑定来自 academic schemas；微信登录身份来自 identity schemas；通知和上传凭证分别来自 notification、media schemas。
- Springdoc 当前将部分已填充响应字段标为可选。消费端使用 `Required` 明确已有 API 契约所保证的字段，保留真实 SQL nullable 字段；这不是运行时验证。
- 状态字符串在客户端收窄为展示与操作需要的联合类型，表单默认值要求完整数字字段。新增服务端状态需要同步显示映射。
- 主题与作品的历史快照、家庭孩子/课表投影、课效名单、媒体访问链接（含可选 thumbnailUrl）、成长报告以及部分统计/支付 Map 响应尚无具体 OpenAPI 响应 schema，因此仍保留明确的手写组合类型。不能将本工程描述为全部契约已自动生成。

图片列表优先使用 `thumbnailUrl`；`url` 始终保留原文件地址，点击查看和下载使用原文件。视频、音频不以缩略图替代播放源。
