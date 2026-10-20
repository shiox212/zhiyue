# 程序与课程验证证据索引

源码交付入口为[main](https://github.com/shiox212/zhiyue/tree/main)。
报告附录和源码包清单中的准确提交，应与[main的CI](https://github.com/shiox212/zhiyue/actions/workflows/ci.yml?query=branch%3Amain)对应。
下面的实验三记录用于展示课程过程，不替代后续提交的自动检查结果。

## Git与CI

| 证据 | 结果与范围 |
| --- | --- |
| [main CI](https://github.com/shiox212/zhiyue/actions/workflows/ci.yml?query=branch%3Amain) | 按准确提交查看后端、前端和仓库检查 |
| [PR2](https://github.com/shiox212/zhiyue/pull/2)、[PR3](https://github.com/shiox212/zhiyue/pull/3) | 五个实验的程序已依次整合到develop和main |
| [实验三PR1](https://github.com/shiox212/zhiyue/pull/1) | 早期CI练习，所含提交随程序集成而合并关闭，原draft元数据保留 |
| [实验三成功run36229661537](https://github.com/shiox212/zhiyue/actions/runs/36229661537) | 实验三阶段三项CI成功 |
| [故意失败run36230006323](https://github.com/shiox212/zhiyue/actions/runs/36230006323) | 独立练习分支上故意修改断言，证明检查会报错 |
| [恢复run36230243640](https://github.com/shiox212/zhiyue/actions/runs/36230243640) | 修复断言后恢复成功，不是最终交付提交的CI |

## 功能与运行

| 证据类型 | 已确认范围 |
| --- | --- |
| 自动测试 | H2、模型fixture与前端测试，另有类型检查和构建；不等于真实模型或MySQL验证 |
| 独立MySQL | 12表初始化、V004重复执行、0初始用户/图书、2角色/4权限、空书库API200 |
| 真实站内回答 | ANSWERED、modelCalled=true、deepseek-flash，书卡110、正确作者及1条引文 |
| 两次真实AUTO页面 | 无站内命中通用回答、部分资料不足时独立补充；页面未采集这两次请求的token等网络元信息 |
| 真实书卡导航 | 进入图书详情并重新展开助手，原两条问答仍在 |
| 历史响应回放 | 引文展开、详情跳转、重开后再次展开，属于模拟交互，未新增模型请求 |
| 用户体验与封面 | 搜索、详情、书架/推荐、旧问答检查；修复后封面完整；窄屏仅嵌入页面验证 |

课程合并报告包含实验截图。原始日志、完整截图及失败记录保存在本机实验报告与协作交接归档中，
不作为源码依赖上传。各实验测试数量只对应相应范围，不是最终主分支累计测试总数。
源代码与本机运行版的关系见[交付状态](../final-state.md)，个人学习体会与人工确认来自本人，
没有虚构他人的评审意见或GitHub Review。
