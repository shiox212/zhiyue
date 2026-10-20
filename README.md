# 知阅课程实验项目

[![CI](https://github.com/shiox212/zhiyue/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/shiox212/zhiyue/actions/workflows/ci.yml?query=branch%3Amain)

知阅是面向读者的阅读社交与图书推荐平台，包含五个课程实验的整合成果。
课程源码入口为[main分支](https://github.com/shiox212/zhiyue/tree/main)。

## 功能

- 图书浏览、搜索、详情、书评、评分、书架阅读状态和个性化推荐。
- JWT登录、HttpOnly Cookie、CSRF、USER/ADMIN权限及本人数据访问控制。
- AUTO阅读助手：优先检索站内书目和项目资料，无命中时标明通用知识，资料不足时分区补充。
- 站内书卡、引用原文展开和详情跳转；同一应用内收起面板或切换页面后保留对话。
- 书架封面完整缩放，避免裁切书名和作者。

## 环境与启动

后端使用Java17、Spring Boot3.3.5和MyBatis；前端使用Vue3、TypeScript与Vite；数据库为MySQL8。
源码构建需要Maven3.9.x和Node24。

按[环境配置与启动说明](docs/setup.md)初始化自己的空数据库、设置本机配置并分别启动前后端。
该说明统一使用前端 <http://127.0.0.1:5179/books>、后端18085，修改端口时同步代理与CORS来源。
数据库包含基础六表和V004安全六表，首次空库没有用户和图书，需创建自己的测试数据。
AI真实生成需要接收者自行配置DeepSeek账号与额度，普通图书和账号功能不依赖模型Key。

仓库提供源码及公开占位配置模板，不含本机配置、原用户数据、137本演示馆藏或本机运行包。
持有M25本机演示包的用户可按[运行版说明](docs/final-state.md#已有本机演示包)启动，
该包与源码安装的配置方式不同。

## 验证

后端执行`mvn verify`；前端执行`npm run typecheck`、`npm run test`和`npm run build`。
[main的CI](https://github.com/shiox212/zhiyue/actions/workflows/ci.yml?query=branch%3Amain)
包含后端、前端和仓库文件检查。具体结果须与相应提交对应，前端未配置ESLint。

独立MySQL验证覆盖12表初始化、V004重复执行及空书库接口。
AI证据区分真实模型回答、页面观察与历史响应回放，范围见[验证证据索引](docs/evidence/evidence-index.md)。
实验设计分别见[安全与认证](docs/experiment-4-security.md)、[RAG阅读助手](docs/experiment-5-rag.md)。

## 使用边界

助手使用统计型TF-IDF检索，未部署神经向量库；书目简介不等于图书全文。
引用校验通过也不保证结论正确，通用回答不代表实时联网核实。
对话仅保存在应用内存，刷新、退出、切换账号或清空后会丢失。
模型请求可能计费，失败不自动重试。项目未作公网生产部署或手机真机完整验收。
