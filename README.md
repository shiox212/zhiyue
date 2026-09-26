# 知阅课程实验项目

知阅提供图书浏览、用户登录、书架和阅读互动。后端 Java 17 / Spring Boot 3.3.5 / MyBatis，前端 Vue 3 / TypeScript / Vite，数据库 MySQL 8。当前使用 Session Cookie；JWT/RBAC、LLM/RAG 属于后续实验，尚未完成。

## 发布副本

这是从本机实验二代码快照创建的独立 Git 历史。本机原始提交和私人实验材料保留在本地，本仓库不携带原始 SQL 数据、旧口令历史、私人 PDF/Word 附件及原始基线标签。业务源码和自动化测试保持一致。

main/develop 为实验二基线，feature/experiment-3-git-ci 引入实验三 CI 修复。基线不启用旧的占位 CI，实际工作流由实验三功能分支引入，便于真实 PR 审阅。

## 启动

按 database/README.md 初始化独立测试数据库，并在本机 .env 设置 DB_URL、DB_USERNAME、DB_PASSWORD。安装 JDK 17、Maven 3.9+、Node 24。

后端在根目录执行 mvn spring-boot:run；前端在 frontend 目录执行 npm ci、npm run dev。默认后端 http://localhost:8081，前端 http://localhost:5173。前端说明见 frontend/README.md。

## 验证与实验状态

后端在根目录执行 mvn verify。前端执行 npm run typecheck、npm run test、npm run build。前端未配置 ESLint。历史本地记录为后端 30 项、前端 2 项测试通过；新的环境及云端结果需重新运行，不能由历史记录推断。

新注册密码使用 BCrypt，旧明文兼容、对象权限和开发依赖审计问题仍需后续完善。仓库不代表生产安全验收通过。

真实 PR、云端 CI、人工评审及实验截图以实际证据为准。实验三完成后等待用户审阅，不自动进入下一实验。
