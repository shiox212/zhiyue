# 知阅 Vue 前端（实验二）

本目录是独立的 Vue 3 + TypeScript + Vite 工程，通过 Axios 请求 Spring Boot。
实验二使用服务器 Session，不使用 JWT；JWT 与角色权限属于实验四。

## 本地运行

先在项目根目录配置本机 `.env` 中的数据库连接信息并启动后端：

```powershell
mvn spring-boot:run
```

然后在本目录打开另一个终端：

```powershell
npm ci
npm run dev
```

访问 `http://localhost:5173`。开发服务器将 `/api` 请求代理到
`http://localhost:8081`。后端接口文档位于
`http://localhost:8081/swagger-ui/index.html`。

请使用 `localhost` 保持主机名一致，不要混用 `127.0.0.1`。
数据库密码仅保存在本机环境配置中，不要写入前端或提交到 Git。

## 验证与构建

```powershell
npm run typecheck
npm run test
npm run build
```

构建输出为 `dist/`，不是 Spring Boot 的静态资源目录。
`npm run preview -- --host 127.0.0.1` 在 5174 端口预览静态构建，
并将 `/api` 代理到独立运行的 8081 后端。此方式用于本地独立产物联调，
不是公网生产服务器；正式部署仍需下文的静态托管与反向代理。

## 独立部署说明

后端在项目根目录执行 `mvn clean package`，通过 `java -jar` 启动生成的 JAR。
前端 `dist/` 交给静态服务器托管。推荐在静态服务器上把 `/api/` 反向代理至后端，
并把未知页面路径回退至 `index.html`，以支持 Vue Router 的 history 路由。

如果前端直接访问后端地址，需在构建前设置 `VITE_API_BASE_URL` 为后端的 `/api` 地址，
同时将后端 `CORS_ALLOWED_ORIGIN` 设置为准确的前端来源（协议、主机和端口），
不能使用通配符配合凭证。跨站 Cookie 还受浏览器策略影响；生产建议同源反向代理和 HTTPS。
前端环境变量会进入浏览器产物，不可放入密钥。

## 当前验证边界（2026-09-19）

- F 盘主副本生产构建通过（含 TypeScript 检查），Vitest 2 项测试通过。
- 后端自动化测试已扩展为 30 项，包含 REST、Session、CORS、密码响应保护及 OpenAPI 校验。
- 已用真实 MySQL 和浏览器验证注册、登录、搜索、书架更新、资料保存、刷新恢复会话及退出。
- 已验证 5174 前端构建产物与 8081 JAR 独立运行；未进行公网生产部署。
- 依赖审计仍提示 Vitest / @vitest/mocker 共 2 项中等风险，审计返回 fixAvailable=false；未强制升级。
- 注册入口位于登录页，注册成功后返回登录表单。
- 保留原静态页面作为兼容参考，并不代表旧功能全部已完成 Vue 迁移。

课程报告和真实截图归档于个人项目目录下的“实验报告/实验二-前后端分离与RESTful-API”。
本实验等待用户审阅；未验证的推荐、评分、书评及密码修改完整流程不作完成宣称。
