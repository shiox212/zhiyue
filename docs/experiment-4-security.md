# 实验四：JWT、RBAC 与 AI 输入边界

## 运行与数据库

当前代码必须使用实验四数据库结构。新空库先执行 `database/schema.sql`，
然后执行 `database/migrations/V004__security.sql`。基础脚本固定使用
`ebook_recommend_db`；如使用其他实验库，先替换脚本内数据库名，避免写入旧库。
基础建表脚本只执行一次；V004 可重复执行。已有业务库先备份，再仅执行 V004。
不要向原始纯净提交包写入配置、实验数据或生成文件。

复制 `.env.example` 为 `.env` 后配置 MySQL。`JWT_SECRET_BASE64` 必须是至少
32 个随机字节的 Base64 编码，不能使用示例占位符或单元测试密钥。
密钥只保存在服务端本地配置；更换密钥会让此前签名的访问令牌失效。
`.env`、`frontend/.env.local` 均由 Git 忽略。

源码安装与启动统一见[环境说明](setup.md)：JDK17、Maven3.9.x、Node24、MySQL8，
前端5179、后端18085，代理/CORS保持同一主机名和来源。
使用IDEA时运行目录为含pom.xml的项目根；本机HTTP设COOKIE_SECURE=false，HTTPS使用true。
当前入口是Vue前端，旧静态HTML和旧Session拦截器不参与当前认证。

## 认证与撤销

1. 客户端 GET `/api/csrf`，保留响应 Cookie，读取 JSON 中的 token。
2. 修改请求带 `X-XSRF-TOKEN`；登录、注册、刷新、退出也必须校验 CSRF。
3. POST `/api/session` 成功后，后端写入 HttpOnly、SameSite=Strict 的
   `ZY_ACCESS` 和 `ZY_REFRESH`。JSON 只返回用户信息，不返回密码或凭据。
4. `ZY_ACCESS` 是 HS256 JWT，默认有效期 900 秒。校验签名、过期时间、
   issuer 和 audience 后，还要检查数据库账号状态及 token_version。
5. `ZY_REFRESH` 是随机凭据，默认有效期 7 天，只允许 `/api/session` 路径。
   数据库只保存 SHA-256 摘要。刷新时在同一事务和账号行锁内消费旧摘要、
   发放新凭据；一个刷新凭据只能成功使用一次。
6. DELETE `/api/session` 退出全部设备；修改密码、变更账号状态也会递增
   token_version 并删除全部刷新摘要。前端收到 401 只尝试刷新一次。

前端不在 localStorage/sessionStorage 存储令牌。Cookie 自动携带凭据，因此
仍必须启用 CSRF；HttpOnly 也不能代替 XSS 防护。登录态不依赖 HttpSession。

## 权限模型

新增角色、权限、角色权限、用户角色、账号版本、刷新摘要 6 张表。
角色为 USER 和 ADMIN。V004 初次根据旧 `user.role` 初始化关系；
服务端实际授权以关系表为准，不接受注册请求传入的管理员角色。

| 范围 | 服务端校验 |
| --- | --- |
| 图书查询、公开书评 | 允许匿名查询 |
| 个人中心、书架、评分、书评互动、推荐 | 必须认证，传入 userId 必须为本人 |
| `/api/admin/**` | 必须具有 ADMIN 角色 |
| 管理图书、用户、书评与统计 | 另外由 `@PreAuthorize` 检查具体权限 |

权限为 `book:manage`、`user:manage`、`review:moderate`、`stats:read`。
本实验通过管理 API 验证权限，没有新增 Vue 管理后台。管理员须由数据库
维护人员针对明确账户赋予；不能通过公开注册接口自提权。

## 旧密码的离线迁移

已移除明文密码在线登录兼容。旧数据应先备份，并停止其他应用实例，
再在项目根目录执行下列维护命令。它不会启动 Web 服务，成功后关闭上下文退出。

```powershell
java -jar target/EbookRecommendSystem-0.0.1-SNAPSHOT.jar --spring.main.web-application-type=none --app.security.migrate-legacy-passwords=true --app.recommend.initial-delay-ms=3600000
```

迁移只将非 BCrypt 值转为 BCrypt，保留原有 BCrypt 值，不打印密码。
非离线模式启用迁移开关会拒绝启动。维护后移除开关，再正常运行项目。
已泄露的旧密码仍需用户自行更换；转为哈希不能收回已泄露的历史数据。

## AI 输入检查与限制

POST `/api/safety/validate` 是实验四的检查接口，需要认证和 CSRF。
请求包含 question 和 references；响应明确给出 `modelCalled=false`。

问题最多 1000 字符；参考资料最多 8 段，每段最多 3000 字符，合计最多
12000 字符。拒绝非法控制字符、资料编号和部分明显越界指令。
服务端固定系统规则，将用户问题和资料作为独立的不可信数据保留。

该安全检查接口自身不调用模型、检索知识库或执行工具；整合版本的模型与检索由
[实验五助手接口](experiment-5-rag.md)负责。关键词规则可能误判或漏判，不能保证抵御全部提示注入。
客户端提交的资料编号也不能直接当作可信引用。

## 验证边界

后端单元/集成测试使用隔离的 H2 测试库；`security-fixture.sql` 内的
`DROP ALL OBJECTS` 只用于该库，严禁在业务数据库执行。
真实 MySQL 和浏览器记录在项目外的独立实验报告目录。
CSRF 集成测试通过真实 Cookie 和请求头完成，避免测试辅助仓库污染。

整合源码已经上传公开main，CI按相应提交核对，入口见[证据索引](evidence/evidence-index.md)。
实验四检查范围不包含生产部署和完整安全审计；实验五有单实例模型调用限制，不能当作分布式生产限流。
