# 修复：帖子列表分页 total 在可见性过滤前计算，导致分页不准确

| 属性 | 值 |
|------|-----|
| 分支 | `fix/post-list-visibility-pagination` |
| 日期 | 2026-06-21 |
| 类型 | 修复 |
| Issue | [#14](https://github.com/my123m/TechHubBBS/issues/14) |
| 影响范围 | 后端 PostServiceImpl / UserServiceImpl / PostVisibilityService · 单元测试 · 集成测试 |

## 修复的缺陷

### 根因 — "先分页后过滤"反模式

`PostServiceImpl.listPosts()` 和 `UserServiceImpl.getUserPosts()` 中，MyBatis-Plus 分页查询的 `result.getTotal()` 来自数据库（未过滤可见性），但返回的 `records` 列表经过了应用层 `isVisible()` 过滤。`PageResult.pages` 由 `total/size` 计算，因此 `total` 和 `pages` 基于全部帖子数，而非当前用户实际可见的帖子数。

当存在非公开帖子时：
- 分页元数据偏大（如 total=25，实际可见仅 8）
- 翻到末尾页返回空列表
- 首页无限滚动提前终止或持续加载空页

### 修复方式 — 将可见性条件下推到 SQL

新增 `PostVisibilityService.applyVisibilityFilter()` 方法，将可见性规则翻译为 `LambdaQueryWrapper` 条件，注入到数据库查询中。数据库直接返回仅可见的帖子 → `result.getTotal()` 自动正确。

可见性→SQL 映射（与 `isVisible()` 语义严格对齐）：

| 浏览者 | SQL 条件 |
|--------|----------|
| 管理员 | 无过滤（全部可见） |
| 游客 | `visibility = PUBLIC` |
| 登录非管理员 | `author_id = :uid OR visibility = PUBLIC OR visibility = LOGIN_ONLY OR (visibility = FOLLOWERS_ONLY AND author_id IN (:关注列表))` |
| 空关注列表 | 不加 FOLLOWERS_ONLY 分支，避免 `IN ()` 无效 SQL |

## 文件变更

| 操作 | 文件 |
|------|------|
| 修改 | `backend/src/main/java/com/techhub/service/PostVisibilityService.java` — 新增 `applyVisibilityFilter()` |
| 修改 | `backend/src/main/java/com/techhub/service/impl/PostServiceImpl.java` — `listPosts()` 调用 `applyVisibilityFilter`，移除应用层 `isVisible` 循环 |
| 修改 | `backend/src/main/java/com/techhub/service/impl/UserServiceImpl.java` — `getUserPosts()` 同上，移除冗余 `deleted=0` |
| 修改 | `backend/src/test/java/com/techhub/service/PostServiceTest.java` — 移除已失效 `isVisible` stub，新增 `totalMatchesRecords` 回归测试 |
| 修改 | `backend/src/test/java/com/techhub/service/PostVisibilityServiceTest.java` — 新增 `applyVisibilityFilter` 四场景测试 |
| 修改 | `backend/src/test/java/com/techhub/integration/VisibilityMatrixIntegrationTest.java` — 新增 `listPosts`/`getUserPosts` 分页 total 回归测试 |

## 验证方式

- `mvn compile` 编译通过
- `mvn test -Dtest="PostServiceTest,PostVisibilityServiceTest"` 单元测试通过
- `mvn test -Dtest="VisibilityMatrixIntegrationTest"` 集成测试通过（含新分页回归）
  - 15 公开 + 10 私密帖，游客 size=10 → total=15, pages=2, page2 records=5
  - 作者查看自己帖子 → total=25, pages=3
- `mvn test -Dtest="UserControllerTest"` 控制器测试通过（mock 不受影响）

## 已知限制

- `UserServiceImpl.getUserPosts()` 原含冗余 `eq(Post::getDeleted, 0)`（`@TableLogic` 已自动处理），本次一并清理
- 关注列表极大时 `IN (followedIds)` 可能产生长 IN 子句；未来若成瓶颈可换为 EXISTS 子查询
- 可见性 SQL 条件与 `isVisible()` 方法体独立维护，变更时需同步更新（已在 `applyVisibilityFilter` 注释中标明）

## 相关知识

- `Follow` 表无 `@TableLogic`（关注关系不过滤已删除），查询 `followee_id` 列表时直接 `SELECT * FROM follow WHERE follower_id = ?`
