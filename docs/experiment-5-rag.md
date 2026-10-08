# 实验五：DeepSeek 图书知识助手与 RAG

2026-10-08 悬浮助手修订：本地实现完成，后端验证待补，用户待审。
实现提交 `a5c6b807a55033e6ac73d94181d5a9f928ac5b80`；主 Agent推送 GitHub 因连接 github.com:443 失败，远程未更新。用户已恢复提交与上传授权，未宣称云端 CI 通过。

## 使用与回答模式

站内内容页点击右下角“问”打开面板，关闭按钮、再次点击入口或 Escape 可收起；收起后保留当前页面会话的问答及进行中的请求。
`/assistant` 保留完整页面，登录页不显示入口。访客可打开面板并点击“登录后提问”，登录后回到原页面；访客不发送受保护的状态或问答请求。
两种问答模式均要求登录，POST 还要求 CSRF。刷新或组件卸载清空历史，退出/切换账号清空状态并忽略旧账号迟到结果。

| 模式 | 回答范围 | 依据与限制 |
| --- | --- | --- |
| `LIBRARY`（默认） | 站内书名、作者、分类、简介及已收录项目资料 | 最多发送 3 段命中资料；无命中返回 `NO_EVIDENCE`，不调用模型 |
| `GENERAL` | 文学背景、阅读概念与阅读建议 | 不检索站内图书，引用为空；标注“模型通用知识 · 非站内资料 · 非实时联网” |

每条历史显示提交时的模式、问题与结果。历史仅供页面查看，不发给模型；每次请求仍是单次独立问答。
站内引用可展开原文，图书引用提供 `/books/{bookId}` 详情链接，ID 由服务端命中资料重建。
简介不等于图书全文，引用存在不能保证结论正确；通用知识也可能有误，不代表联网核实或站内收录。

## 启动与配置

沿用实验四的 MySQL、JWT 和前后端配置。在项目根目录的 `.env` 设置：

```dotenv
DEEPSEEK_API_KEY=在本地填写自己的密钥
DEEPSEEK_MODEL=deepseek-flash
```

不要把真实密钥填入 `.env.example` 或提交 Git。IDEA 的运行目录必须为含 `pom.xml` 的项目根目录。
修改环境文件后重启后端。后端 `mvn spring-boot:run`，前端在 `frontend` 中执行 `npm ci`、`npm run dev`。
2026-10-04 的本机演示后端使用 `18081`；这不是本次副本已启动的证明。
独立副本未复制私密配置，需自行配置数据库、JWT 与模型密钥，并按实际后端端口设置前端 `VITE_BACKEND_URL`。
浏览器访问 `http://localhost:5173` 并登录；缺少 Key 时助手提示尚未配置，当前副本的基础阅读服务也未启动验证。

这里的“OpenAI 兼容”指请求协议和 Java SDK：依赖 `com.openai:openai-java:4.69.3`，
请求固定发送至 `https://api.deepseek.com`，使用 DeepSeek Key 与模型，不需要 OpenAI 账户。
使用 Chat Completions，System / User 分离，JSON 输出，关闭 thinking，最多 1200 输出 token，
40 秒超时，不自动重试，避免重复消耗。前端本接口等待上限为 55 秒。

## 从问题到回答

1. 已登录用户提交问题；Spring Security 检查登录与 CSRF。
2. `LearningAssistant.ask(userId, question, mode)` 检查输入；`LIBRARY` 检索项目资料与公开图书元数据，`GENERAL` 跳过检索。
3. `DocumentChunker` 按段落与 Unicode 字符切分；长段最大 480 字符，重叠 60 字符。
4. `TfidfIndex` 将英文词和中文相邻双字词转为 TF-IDF 稀疏向量，用余弦相似度找前 3 段，最低分 0.13。
5. `LIBRARY` 合并两类命中取最多 3 段；无命中返回 `NO_EVIDENCE`，不请求模型。命中时组合当前问题和片段为 User JSON；`GENERAL` 只发送问题及空 references。
6. `DeepSeekModel` 通过真实 Java SDK 请求模型，返回文本、模型名、请求编号与 token 计数。
7. `AnswerVerifier` 核对 JSON、资料编号、逐字引句；`GENERAL` 要求引用为空，不允许伪造站内引用。不合规返回 `UNVERIFIED` 并隐藏原回答。
8. Vue 以纯文本显示回答，资料标题和原文由服务端知识库提供，用户可以展开核对。

这里用的是本地统计向量，**没有调用神经网络 embedding API、没有部署向量数据库**。
小语料足以演示分块、向量化、检索、提示词增强；同义改写和复杂问题的召回仍有限。
引用存在并不保证回答在语义上正确，必须人工对照原文；测试中保留了这一反例。

## 资料来源与更新

资料白名单位于 `src/main/resources/knowledge/zhiyue-guide.json`，当前 6 篇说明、14 个片段。
内容由 AI 根据当前项目代码整理，待用户复核；不自动扫描磁盘、用户上传文件或 `.env`。
修订增加受控数据库查询，仅投影图书 ID、书名、作者、分类、简介；不读取用户资料、书架、评论、密码或配置，不发送整库。
绑定问题和关键词，优先匹配完整书名/作者，候选上限 20 条；分块后仍用 TF-IDF 筛选并提升完整匹配优先级，召回有限，不保证查全。
状态接口的 documents/chunks 只描述项目资料白名单，不包含动态图书数量。
修改资料后应检查事实与保密范围，再重启服务重新建立索引。

| 资料 ID | 范围 | 核对位置 |
| --- | --- | --- |
| overview | 技术栈与分层 | pom.xml、frontend/package.json、controller/service/mapper |
| running | IDEA、工作目录与端口 | README、application.yml、frontend/vite.config.ts |
| authentication | JWT、刷新、退出、密码 | security 包、认证服务、实验四说明 |
| authorization | RBAC、归属校验 | SecurityConfig、方法权限注解、业务服务 |
| reading | 书架、阅读、评分与推荐 | 图书/书架/互动/推荐服务 |
| assistant | 本实验机制与边界 | ai 包、LearningAssistantController、学习助手页面 |

仅说明配置名称，不包括真实配置值。不发送数据库密码、JWT Key、用户资料或历史会话；
当前用户主动输入的文字会发给服务商，因此页面明确提醒不要输入个人信息和秘密。

## 接口与状态

| 接口 | 行为 |
| --- | --- |
| GET /api/assistant/status | 需登录；返回配置是否存在、模型名、资料数量，不返回 Key |
| POST /api/assistant/ask | 需登录与 CSRF；请求体为 `{ "question": "《三体》的作者和简介是什么？", "mode": "LIBRARY" }`；mode 可为 `LIBRARY` / `GENERAL`，省略按 `LIBRARY` 处理 |

`ANSWERED` 表示格式和引用检查通过；`GENERAL_KNOWLEDGE` 表示通用知识、无站内引用；`NO_EVIDENCE` 表示未调用模型；
`INSUFFICIENT` 表示模型判断资料不足，展示固定系统说明；`UNVERIFIED` 表示模型输出未通过校验。
接口错误使用实际 HTTP 错误码：输入 400、未登录 401、CSRF 403、限流 429、
不完整模型输出 502、缺少配置或上游失败 503。错误详情不包含 Key 或服务商原始响应。

每个用户每分钟最多 5 次模型请求，全服务最多同时 2 次；这是单实例内存限制，
不是分布式生产限流。问题最大 1000 字符。正则输入拦截只覆盖明显越界指令，不能保证抵御全部注入。
没有模型工具执行、用户私有数据查询、自动操作账号或回答持久化功能；会话内历史不作为模型上下文。

## 验证与真实失败

2026-10-04 执行 `mvn verify`，前端执行 `npm run test -- --run` 和 `npm run build`（含类型检查）。
当日 81 项后端测试使用本地模型替身或 HTTP 模拟服务，不消耗 Key；11 项前端测试检查错误显示、
重复提交、纯文本输出与引用。真实外部调用单独验证，不能把模拟测试写成服务商测试。

2026-10-04 的真实验证：令牌有效期和 IDEA 启动回答可对照原文；无关问题不调用模型；
Redis 错误前提问题两次返回引用不合规，被拦截。无资料基线提示词会推测 Redis 的默认过期值，
但本项目未使用 Redis。带资料的独立对照调用拒绝给出该值，说明补充资料有效但不能保证每次输出稳定。
详细请求编号、输出、截图和失败记录保存在仓库外的实验报告目录。

2026-10-08 返修后日志记录定向前端 2 个文件、15 项通过（视图 10、布局 5），类型检查与生产构建通过；文档 Agent未重跑。
新增后端测试包含完整书名/作者、超过 20 候选优先命中及 GENERAL 认证/CSRF/非法 mode 场景，但没有执行通过证据。
最新离线 Maven 使用本机缓存，缺 openai-java 4.69.3 和 spring-security-test 6.3.4，依赖解析失败，未进入编译或测试。
本次真实浏览器、数据库、模型未复测，新截图、真人评审及用户心得未收集；历史 81/11 不覆盖此次修订。

官方资料：[DeepSeek Chat Completions](https://api-docs.deepseek.com/api/create-chat-completion/)、
[DeepSeek 接入说明](https://api-docs.deepseek.com/guides/harness)、
[OpenAI Java SDK](https://github.com/openai/openai-java)。
