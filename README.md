# 知阅课程实验项目

知阅提供图书浏览、用户登录、书架和阅读互动。后端 Java 17 / Spring Boot 3.3.5 / MyBatis，前端 Vue 3 / TypeScript / Vite，数据库 MySQL 8。实验四改用 JWT、HttpOnly Cookie、RBAC 和 CSRF 校验；LLM/RAG 尚未接入。

## 发布副本

这是从本机实验二代码快照创建的独立 Git 历史。本机原始提交和私人实验材料保留在本地，本仓库不携带原始 SQL 数据、旧口令历史、私人 PDF/Word 附件及原始基线标签。业务源码和自动化测试保持一致。

main/develop 为实验二基线，feature/experiment-3-git-ci 引入实验三 CI 修复。基线不启用旧的占位 CI，实际工作流由实验三功能分支引入，便于真实 PR 审阅。

## 启动

按 database/README.md 初始化独立测试数据库，再执行 database/migrations/V004__security.sql。在本机 .env 设置 DB_URL、DB_USERNAME、DB_PASSWORD、JWT_SECRET_BASE64（至少 32 个随机字节的 Base64 编码）。安装 JDK 17、Maven 3.9+、Node 24。详细安全与迁移说明见 [实验四说明](docs/experiment-4-security.md)。

后端在根目录执行 mvn spring-boot:run；前端在 frontend 目录执行 npm ci、npm run dev。默认后端 http://localhost:8081，前端 http://localhost:5173。前端说明见 frontend/README.md。

后端端口可在 .env 中设置 SERVER_PORT；前端通过 frontend/.env.local 的 VITE_BACKEND_URL 指向对应后端。浏览器使用 http://localhost:5173，与 CORS 白名单一致。本地 HTTP 设 COOKIE_SECURE=false；HTTPS 环境使用 true。当前入口是 Vue 前端，历史静态 HTML 和旧 Session 拦截器不参与当前认证流程。

## 验证与实验状态

后端在根目录执行 mvn verify。前端执行 npm run typecheck、npm run test、npm run build。前端未配置 ESLint。实验四本地测试覆盖 JWT、CSRF、RBAC、跨用户访问、刷新重放、退出撤销和 AI 输入检查。实际结果以各次验证记录为准，不能推断本分支已通过云端 CI。

密码使用 BCrypt，已移除在线明文兼容，旧数据需显式离线迁移。个人数据接口由服务端核对身份。AI 输入检查只是模型接入前的有限防护，仓库不代表生产安全验收通过。

用户已要求暂缓 GitHub 上传；实验四仅在本地分支开展。真实 PR、云端 CI、人工评审及实验截图以实际证据为准。每个实验完成后等待用户审阅，不自动进入下一实验。
