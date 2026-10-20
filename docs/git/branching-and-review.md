# 分支、Pull Request 与评审流程

## 分支关系

```text
main
  └─ develop
       ├─ feature/auth-jwt
       ├─ feature/vue-frontend
       ├─ feature/ai-rag
       ├─ fix/具体问题
       └─ docs/具体材料
```

`main` 只接收经过验收的版本；日常功能先进入 `develop`。功能分支不得长期脱离 `develop`，避免一次产生大量冲突。

## 一个功能的完整操作

```bash
git switch develop
git pull
git switch -c feature/example

# 编辑、验证后
git status
git diff
git add 具体文件
git commit -m "feat: 完成示例功能"
git push -u origin feature/example
```

随后在 GitHub 创建面向 `develop` 的 Pull Request，按模板记录测试、风险、AI 使用和人工核对。评审意见处理完毕且 CI 通过后再合并。

## 冲突练习记录

课程要求的冲突解决应在独立练习分支完成并如实记录：冲突原因、冲突标记、最终取舍、验证方式及对应提交。不要为了截图故意破坏正式功能，也不要伪造他人评审。

## 建议保护规则

可按协作需要为`main`配置以下规则；本节是建议，不表示仓库已经启用这些保护：

- 必须通过 Pull Request 合并。
- 至少一名人工评审者批准。
- 必须通过后端、前端与仓库安全检查。
- 禁止强制推送和直接删除。

