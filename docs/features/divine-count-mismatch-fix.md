# 修复：神评专区显示"暂无神评"但神评计数 > 0

| 属性 | 值 |
|------|-----|
| 分支 | `fix/divine-count-mismatch-47` |
| 日期 | 2026-06-23 |
| 类型 | Bug 修复 |
| Issue | [#47](https://github.com/my123m/TechHubBBS/issues/47) |
| 影响范围 | 后端 CommentServiceImpl、DivineCommentScheduler、data.sql；前端 PostDetailPage.vue |

## 根因分析

### 根因 1：删除神评评论时未递减 `divineCommentCount`

`CommentServiceImpl.delete()` 在删除评论时只递减了 `post.commentCount`，未检查被删评论是否为神评（`isDivine == 1`），也未递减 `post.divineCommentCount`。删除神评后计数器变成脏数据，且无其他路径修正。

`DivineCommentScheduler` 每 5 分钟扫描现存评论做晋升/撤销，但不会检测"计数与实际行数不一致"的情况，因此脏计数会永久存在。

### 根因 2：种子数据 `divine_comment_count` 与实际神评评论不一致

`data.sql` 中帖子 33（微服务架构设计中的十大陷阱）和帖子 41（2026 年 Java 后端开发者学习路线图）的 `divine_comment_count = 1`，但两个帖子下均不存在 `is_divine = 1` 的评论。使用种子数据初始化后，打开这两个帖子即可复现。

### 影响

- 帖子头部统计行显示神评奖章图标 + 计数，但神评专区显示"暂无神评"
- 前端 `v-if="post.divineCommentCount > 0"` 基于脏计数显示神评专区
- 计数与实际列表不一致，用户体验割裂

## 修复方案

### 1. `CommentServiceImpl.delete()` — 删除神评时同步递减计数

删除评论时检查 `comment.isDivine`，若为 1 则同步递减 `post.divineCommentCount`：

```java
if (comment.getIsDivine() != null && comment.getIsDivine() == 1
        && post.getDivineCommentCount() != null && post.getDivineCommentCount() > 0) {
    post.setDivineCommentCount(post.getDivineCommentCount() - 1);
}
```

### 2. `data.sql` — 修正种子数据并补充神评

**修正不一致的计数**：帖子 30/34 的 `divine_comment_count` 从 0 改为 1（各有 1 条神评评论），帖子 38/45 从 2 改为 1（各有 1 条神评评论）。

**为帖子 33/41 添加神评**：将评论 55（帖子 33）和评论 65（帖子 41）提升为神评（`is_divine=1`），增加互动数据（`likeCount`/`recommendCount` 达到阈值），`divine_comment_count` 设为 1。同步添加对应的 `comment_recommend` 推荐记录和 `DIVINE` 通知记录。

### 3. `DivineCommentScheduler` — 新增每小时对账任务

新增 `reconcileDivineCounts()` 方法，每小时执行一次，覆盖两种漂移场景：
- 帖子有 `is_divine=1` 评论但 `divineCommentCount` 为 0（如种子数据错误）
- 帖子 `divineCommentCount > 0` 但无实际神评评论（如删除后遗留）

对账逻辑：先查询所有 `is_divine=1` 评论按 `post_id` 分组得到实际计数，再查询所有 `divineCommentCount > 0` 的帖子，逐一比对并修正不一致的计数。

### 4. `PostDetailPage.vue` — 前端防御性修正

`fetchDivineComments()` 获取实际列表后，将 `post.divineCommentCount` 同步为 `divineComments.length`。移除 `divineCommentCount === 0` 的短路判断，确保即使后端计数为 0 但实际有评论时也能正确获取。

## 文件变更

| 操作 | 文件 |
|------|------|
| 修改 | `backend/src/main/java/com/techhub/service/impl/CommentServiceImpl.java` — `delete()` 方法新增神评计数递减逻辑 |
| 修改 | `backend/src/main/java/com/techhub/scheduler/DivineCommentScheduler.java` — 新增 `reconcileDivineCounts()` 对账方法 |
| 修改 | `backend/src/main/resources/db/data.sql` — 帖子 33/41 的 `divine_comment_count` 由 1 改为 0 |
| 修改 | `frontend/src/pages/post/PostDetailPage.vue` — `fetchDivineComments()` 移除短路判断，获取后同步计数 |
| 新增 | `docs/features/divine-count-mismatch-fix.md` |

## 验证方式

- `mvn compile -q` — 编译通过
- `pnpm build` — 前端构建通过
- 使用种子数据初始化数据库，打开帖子 33 和 41 详情页，神评专区不再显示
- 删除一条神评评论后，重新打开帖子详情页，神评计数正确递减
- 等待对账任务执行（或手动触发），检查日志中"对账修正"输出

## 已知限制

- 对账任务每小时执行一次，极端情况下计数不一致最多持续 1 小时
- 帖子列表页（PostCard）仍显示后端返回的 `divineCommentCount`，若后端计数滞后，列表页的神评徽章可能短暂不准确，刷新页面后前端详情页会修正
