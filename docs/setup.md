# 环境配置与源码运行

## 所需环境

JDK17、Maven3.9.x、Node24（与.nvmrc和CI一致）、npm、MySQL8。使用npm ci按锁文件安装。普通图书与账号功能不需要模型Key；AI真实生成需接收者自行配置服务商账号和额度。

## 首次数据库初始化

使用自己新建的空MySQL测试实例，确认不会覆盖已有演示库。客户端连接时指定UTF-8：

```text
mysql --default-character-set=utf8mb4 -u 自己的账号 -p
```

输入自己的口令后，在源码根目录对应路径依次执行：

```sql
SOURCE database/schema.sql;
USE ebook_recommend_db;
SOURCE database/migrations/V004__security.sql;
SHOW TABLES;
```

应得到12表。固定角色/权限由V004写入，用户和图书初始为空；注册页面创建自己的USER账号，密码由BCrypt处理。137本馆藏只存在于原本机演示库，不随源码导出，不对原库重复导入。源码新库显示空书库属于预期行为。

## 后端配置

以下字段由接收者在本机填写，可使用进程环境变量或自行建立被Git忽略的.env。真实值不得提交；配置模板不能直接作为真实密钥使用。

| 字段 | 设置 |
| --- | --- |
| DB_URL | jdbc:mysql://127.0.0.1:3306/ebook_recommend_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false |
| DB_USERNAME、DB_PASSWORD | 自己数据库的账号和口令 |
| SERVER_PORT | 18085，或自己确认空闲的端口 |
| JWT_SECRET_BASE64 | 本机生成至少32字节随机值的Base64；不要使用示例字串 |
| COOKIE_SECURE | 本机HTTP为false；HTTPS部署为true |
| CORS_ALLOWED_ORIGIN | http://127.0.0.1:5179，和实际前端来源一致 |
| DEEPSEEK_API_KEY | 可选；留空时AI生成不可用，普通功能正常 |
| DEEPSEEK_MODEL | deepseek-flash，实际可用性按供应商账号 |

本机生成JWT值可执行`node -e "console.log(require('node:crypto').randomBytes(32).toString('base64'))"`，仅将输出保存到自己的本机配置，不放入报告、截图或仓库。

在源码根目录执行：

```text
mvn --batch-mode --no-transfer-progress verify
java -jar target/EbookRecommendSystem-0.0.1-SNAPSHOT.jar
```

配置已设置到运行进程时可直接java启动；scripts/run-backend.ps1要求本机.env。端口与数据库配置以自己填写的值为准，看到应用启动后检查`http://127.0.0.1:18085/api/books`。新空库应返回200与空列表。

## 前端安装和运行

在frontend目录执行：

```text
npm ci
npm run typecheck
npm run test
npm run build
```

在同一PowerShell窗口指定后端地址并运行：

```powershell
$env:VITE_BACKEND_URL='http://127.0.0.1:18085'
npm run dev -- --host 127.0.0.1 --port 5179 --strictPort
```

访问`http://127.0.0.1:5179/books`。需要预览已构建文件时，在同一代理配置下执行`npm run preview -- --host 127.0.0.1 --port 5179 --strictPort`。端口占用时strictPort报错退出，不自动换端口；不要混用localhost与127.0.0.1，以免Cookie/CORS来源不同。

## 停止与再次启动

源码服务在各自启动窗口按Ctrl+C停止；再次启动先确认端口释放。已有本机M25运行版的用户使用其启动.cmd，READY后保持窗口，输入q并回车等待STOPPED。不根据旧PID结束其他程序。

## 验证范围

独立MySQL8.0.44空实例已验证基础6表、安全6表、V004重复执行和后端空书库API；没有连接原演示库或调用模型。Windows临时MySQL采用ASCII数据目录通过。最终源码构建/自动测试以准确提交的GitHub CI核对；H2测试、真实MySQL检查、模型实测与模拟回放分别保留。

源码压缩包不含.git，普通mvn/npm命令可运行；scripts/verify.ps1包含Git文件检查，应在Git克隆副本中使用，解压包缺Git历史不表示编译失败。打包交付不含.env、原用户库、依赖缓存、旧运行版或私人材料。助手历史只保留在同一应用内存，刷新/退出/换号会清空，介绍与引用不等同图书全文。
