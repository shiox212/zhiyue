# 知阅课程实验项目

知阅提供图书浏览、用户登录、书架和阅读互动。后端 Java 17 / Spring Boot 3.3.5 / MyBatis，前端 Vue 3 / TypeScript / Vite，数据库 MySQL 8。实验四改用 JWT、HttpOnly Cookie、RBAC 和 CSRF 校验；实验五于 2026-10-08 修订为悬浮图书知识助手，支持站内资料检索和模型通用知识。本地实现完成，后端验证待补，用户待审。

## 发布副本

这是从本机实验二代码快照创建的独立 Git 历史。本机原始提交和私人实验材料保留在本地，本仓库不携带原始 SQL 数据、旧口令历史、私人 PDF/Word 附件及原始基线标签。业务源码和自动化测试保持一致。

main/develop 为实验二基线，feature/experiment-3-git-ci 引入实验三 CI 修复。基线不启用旧的占位 CI，实际工作流由实验三功能分支引入，便于真实 PR 审阅。

## 启动

按 database/README.md 初始化独立测试数据库，再执行 database/migrations/V004__security.sql。在本机 .env 设置 DB_URL、DB_USERNAME、DB_PASSWORD、JWT_SECRET_BASE64（至少 32 个随机字节的 Base64 编码）。安装 JDK 17、Maven 3.9+、Node 24。详细安全与迁移说明见 [实验四说明](docs/experiment-4-security.md)。

后端在根目录执行 mvn spring-boot:run；前端在 frontend 目录执行 npm ci、npm run dev。默认后端 http://localhost:8081，前端 http://localhost:5173。前端说明见 frontend/README.md。

后端端口可在 .env 中设置 SERVER_PORT；前端通过 frontend/.env.local 的 VITE_BACKEND_URL 指向对应后端。浏览器使用 http://localhost:5173，与 CORS 白名单一致。本地 HTTP 设 COOKIE_SECURE=false；HTTPS 环境使用 true。当前入口是 Vue 前端，历史静态 HTML 和旧 Session 拦截器不参与当前认证流程。

图书知识助手还需在本机 .env 配置 DEEPSEEK_API_KEY，可选 DEEPSEEK_MODEL=deepseek-flash，然后重启后端。通过 OpenAI Java SDK 兼容接口访问 DeepSeek 官方地址；不需要 OpenAI 账户。本次独立副本未复制私密配置，服务与模型尚未联调。只配置 Key 不代表连接验证通过，实际调用仍可能失败。

站内内容页点击右下角“问”打开面板，点击关闭按钮、入口或按 Escape 收起；`/assistant` 保留完整页面，登录页不显示入口。访客打开后可点击“登录后提问”。站内模式 `LIBRARY` 核对公开书名、作者、分类、简介与已收录项目资料，引用可展开并打开对应图书详情；无命中不调用模型。通用模式 `GENERAL` 解释文学与阅读知识，不读取站内图书、不提供站内引用，也不是实时联网事实。问答历史仅在当前页面会话内保留，收起再打开仍可查看；刷新、退出或切换账号后清空，不向模型发送历史上下文。详见 [实验五说明](docs/experiment-5-rag.md)。

## 验证与实验状态

后端在根目录执行 mvn verify。前端执行 npm run typecheck、npm run test、npm run build。前端未配置 ESLint。实验四本地测试覆盖 JWT、CSRF、RBAC、跨用户访问、刷新重放、退出撤销和 AI 输入检查。实际结果以各次验证记录为准，不能推断本分支已通过云端 CI。

密码使用 BCrypt，已移除在线明文兼容，旧数据需显式离线迁移。个人数据接口由服务端核对身份。AI 输入检查只是模型接入前的有限防护，仓库不代表生产安全验收通过。

实验五于 2026-10-04 完成本地验证：后端 81 项测试、前端 11 项测试、类型检查与生产构建通过，并完成真实 MySQL / DeepSeek 调用和浏览器验证。测试通过不等于模型每次都答对；真实验证中也出现被引用校验拦截的输出。

2026-10-08 修订后定向前端测试 2 个文件、15 项通过，类型检查与生产构建通过，依据代码 Agent 的当日日志；文档修订未重跑。后端 Maven 在依赖解析阶段失败，未进入编译或测试；最新离线缓存仍缺 openai-java 4.69.3 与 spring-security-test 6.3.4。本次真实浏览器、数据库与模型未复测，不能沿用 2026-10-04 的结果作为新功能验证。

用户已恢复本任务 Git 提交与上传授权。主 Agent已在独立副本提交实现 `a5c6b807a55033e6ac73d94181d5a9f928ac5b80`，但 GitHub 推送因连接 github.com:443 失败，远程未更新；原副本仍保留旧基线。真实 PR、云端 CI、人工评审及实验截图以实际证据为准。实验五仍待用户审阅，不自动进入下一实验。
