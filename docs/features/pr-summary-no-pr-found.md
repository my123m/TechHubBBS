# 修复：PR Summary CI 对 fork PR 始终跳过

| 属性 | 值 |
|------|-----|
| 分支 | `fix/pr-summary-no-pr-found` |
| 日期 | 2026-06-21 |
| 类型 | 修复 |
| 影响范围 | `.github/workflows/pr-summary.yml` |

## 背景

PR Summary 在 Backend/Frontend CI 完成后始终输出 `Notice: No open PR found for this workflow run.`，PR 上不会出现 `ci-passed` / `ci-failed` 标签和 bot 评论。

## 根因

`pr-summary.yml:30` 使用 `github.rest.repos.listPullRequestsAssociatedWithCommit` 通过 `workflow_run.head_sha` 反查 PR。但对于 `pull_request` 事件触发的上游 workflow，`workflow_run.head_sha` 继承的是 GitHub Actions checkout 的 merge commit SHA（`refs/pull/{n}/merge`），而非 PR 的真实 head commit。

`listPullRequestsAssociatedWithCommit` 只能通过 PR 真实 head SHA 或 base 仓库历史中的 commit 关联，对临时 merge commit 无效，导致始终查不到 PR。

本仓库 `origin` (Liao-Ke) 是 `upstream` (my123m) 的 fork，所有 PR 来自 fork，100% 命中此陷阱。

## 历史上下文

`c6f027e fix(workflow): use listPullRequestsAssociatedWithCommit to find PR for fork PRs (#35)` 尝试用 `listPullRequestsAssociatedWithCommit` 替代 `pull_request_target` 方案来修复 fork PR 兼容性，但该 API 对 merge commit SHA 无效，引入本次回归。

## 改动

将 PR 查找方式从 `listPullRequestsAssociatedWithCommit(commit_sha)` 改为 `actions.getWorkflowRun(run_id)`：

- 调用 `github.rest.actions.getWorkflowRun` REST API 直接获取 workflow run 的 `pull_requests` 数组
- GitHub 在 workflow run 详情中内部维护 run↔PR 关联，能正确穿透 fork + merge commit
- 同时取 PR 真实 head SHA（`pull_requests[0].head.sha`）替换 merge commit SHA，使评论中显示的 commit hash 对开发者有意义

| 操作 | 文件 |
|------|------|
| 修改 | `.github/workflows/pr-summary.yml` — 替换第 28-38 行 PR 查找逻辑，第 57 行改为 `headSha` |

## 验证方式

1. 推送改动触发任一 PR 的 Backend / Frontend CI
2. CI 完成后查看 PR Summary 运行日志：应进入 `addLabels` + `createComment` 分支，不再输出 Notice
3. PR 上应出现 `ci-passed` 或 `ci-failed` 标签
4. PR 上应有 bot 评论显示 CI 状态和真实 commit hash

## 已知限制

- 仅对 `pull_request` 事件触发的上游 workflow 生效（已由 `if: github.event.workflow_run.event == 'pull_request'` 过滤）
- `getWorkflowRun` REST API 要求 workflow run 存在（即上游 workflow 已完成），workflow_run 事件本身已保证此条件
- 上游 workflow 如果是 `workflow_dispatch` 手动触发而非 PR 触发，`pull_requests` 数组为空，行为与修前一致（输出 Notice 后跳过）
