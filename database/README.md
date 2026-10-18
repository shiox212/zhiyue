# 数据库结构

本项目提供基础6表的schema.sql和安全6表迁移V004，共12表，不包含原用户记录、演示账号或137本馆藏。

在自己的全新MySQL8测试实例中，以UTF-8客户端依次执行：

```sql
SOURCE database/schema.sql;
USE ebook_recommend_db;
SOURCE database/migrations/V004__security.sql;
SHOW TABLES;
```

schema.sql会创建ebook_recommend_db，只在首次空库执行，基础索引不重复创建；V004可重复执行，不是应用自动迁移。不要在已有演示库重跑基础脚本。MySQL客户端使用`--default-character-set=utf8mb4`，完整步骤见[环境说明](../docs/setup.md)。

初始化后图书列表为空。通过注册页面创建自己的测试账号，密码使用 BCrypt；如需演示图书，请在本机测试库添加虚构书籍，勿上传真实用户数据。

复制 .env.example 为本机 .env，设置自己的连接信息。CI 使用 H2，不需要真实 MySQL 密码。

独立MySQL8.0.44新实例已验证12表初始化、V004重复执行、初始用户/图书均0、固定角色2/权限4，以及后端空书库API200。该验证未使用现有MySQL服务或用户数据，没有模型请求。Windows自行安装临时MySQL时建议使用ASCII数据目录；本轮中文临时目录启动失败、相同参数的ASCII目录通过，两份原记录均保留。
