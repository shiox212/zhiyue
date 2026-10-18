# 程序与课程验证证据索引

当前程序位于[feature/experiment-5-rag](https://github.com/shiox212/zhiyue/tree/feature/experiment-5-rag)。当前整合提交、对应CI与PR1状态在2026-10-18通过GitHub API只读复核；实验三失败/恢复链接沿用原始历史记录。本机截图、测试输出与个人报告另行归档。

| 证据 | 结果与范围 |
| --- | --- |
| [已上传整合提交112e869](https://github.com/shiox212/zhiyue/commit/112e869b743754e8cfdcf99f11fdbdee08c48962) | 包含最终业务代码及M25说明。最终业务改动为6fc96ed的书架封面修复，其后提交为文档同步 |
| [对应CI run39021093470](https://github.com/shiox212/zhiyue/actions/runs/39021093470) | Backend verify、Frontend verify、Repository safety checks三个job全部completed/success，准确对应112e869 |
| [当前功能分支CI](https://github.com/shiox212/zhiyue/actions/workflows/ci.yml?query=branch%3Afeature%2Fexperiment-5-rag) | 后续说明提交使用各自准确SHA的运行，不沿用旧提交的通过结果 |
| [实验三草稿PR1](https://github.com/shiox212/zhiyue/pull/1) | feature/experiment-3-git-ci → develop，open/draft、未合并。远程reviews仍为空；本地材料中的提交者确认不等同GitHub Review |
| [实验三故意失败run36230006323](https://github.com/shiox212/zhiyue/actions/runs/36230006323) | 隔离练习分支上的故意断言失败，保留失败证据 |
| [实验三恢复run36230243640](https://github.com/shiox212/zhiyue/actions/runs/36230243640) | 修复断言后的历史恢复成功记录；历史结果不替代当前分支CI |
| [最终状态说明](../final-state.md) | M25源码/产物对应、启动停止及自动测试、真实问答、模拟回放的分层说明 |

## 本机原始证据

实验一至五独立材料、M30合并报告、截图、原始测试与交付记录保存在本机个人项目工作根，未作为源码文件上传。最终本机演示使用M25；137本馆藏已在演示库，不导出用户认证数据或重复导入。

真实模型验证与自动测试分开：站内回答与引文由T016/T018保留；无命中及部分资料不足的两次真实页面验收由T020保留，额度2/2已用完。引用跳转后重开另有历史真实响应回放，明确为模拟交互、0新模型。当前GitHub同步没有新增模型调用、数据库操作或服务验收。

私有仓库的教师访问权限仍需提交者核实。个人学习体会和人工确认按本人提供内容整理，不虚构外部评审人、具体意见或GitHub Review。
