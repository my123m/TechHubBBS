# 修复：InteractionServiceImpl 点赞/收藏未校验帖子可见性导致信息泄露

| 属性 | 值 |
|------|-----|
| 分支 | `fix/interaction-visibility` |
| 日期 | 2026-06-23 |
| 类型 | 安全修复 |
| Issue | [#68](https://github.com/my123m/TechHubBBS/issues/68) |
| 影响范围 | InteractionServiceImpl.java, DivineCommentServiceImpl.java, 测试文件, 功能文档 |

## 根因分析

### 可见性校验缺失

`InteractionServiceImpl` 的以下 6 个方法在操作前仅检查目标实体是否存在，未调用 `PostVisibilityService.checkVisibleOrThrow()` 校验帖子可见性：

- `likePost()` — L48
- `unlikePost()` — L79
- `favoritePost()` — L109
- `unfavoritePost()` — L128
- `likeComment()` — L149
- `unlikeComment()` — L181

攻击者可通过猜测 ID 对私密/关注可见/粉丝可见的帖子进行点赞或收藏，通过通知系统和交互反馈确认帖子存在并获取其 ID，造成信息泄露。

对比 `CommentServiceImpl.create()` 已正确处理：
```java
boolean isAdmin = isAdmin();
postVisibilityService.checkVisibleOrThrow(post, userId, isAdmin);
```

### DivineCommentServiceImpl.listDivineComments() 同类问题

`listDivineComments()` 直接查询指定帖子的神评列表，未校验帖子可见性。任意登录用户可查询私密帖子的神评列表。

## 修复方案

### InteractionServiceImpl

1. 注入 `PostVisibilityService` 依赖
2. 新增 `isAdmin()` 辅助方法（采用 `"ADMIN".equals(SecurityUtils.getCurrentRole())`，与 `UserServiceImpl` / `PostServiceImpl` / `RecommendationServiceImpl` 一致）
3. `likePost` / `favoritePost`: `postMapper.selectById` 后调用 `checkVisibleOrThrow(post, userId, isAdmin())`
4. `unlikePost` / `unfavoritePost`: 操作前查询 post 并校验可见性（不可见抛 404，不泄露存在性），复用已查询的 post 引用避免二次查询
5. `likeComment` / `unlikeComment`: 查询 comment 后查询 parent post（`comment.getPostId()`），校验 post 可见性

### DivineCommentServiceImpl

1. 注入 `PostVisibilityService` 依赖
2. `listDivineComments(postId)`: 查询神评列表前先查 post 并校验可见性
3. 新增 `isAdmin()` 辅助方法

### 设计决策：`isAdmin()` 实现

采用 `"ADMIN".equals(role)` 而非 `RoleEnum.ADMIN.getCode().equals(role)`：
- `SecurityUtils.getCurrentRole()` 返回大写（如 "ADMIN"），而 `RoleEnum.ADMIN.getCode()` 返回小写（"admin"），后者存在已知的大小写不匹配 bug
- 4 个现有服务（`UserServiceImpl`, `PostServiceImpl`, `RecommendationServiceImpl`, `DivineCommentServiceImpl.forceSetDivine`）均使用大写字面量比较
- `CommentServiceImpl.isAdmin()` 的 `RoleEnum` 模式已被 PR #86 跟踪修复

## 文件变更

| 操作 | 文件 |
|------|------|
| 修改 | `backend/.../service/impl/InteractionServiceImpl.java` — 注入 `PostVisibilityService`，6 个方法加可见性校验，新增 `isAdmin()` |
| 修改 | `backend/.../service/impl/DivineCommentServiceImpl.java` — 注入 `PostVisibilityService`，`listDivineComments` 加可见性校验，新增 `isAdmin()` |
| 修改 | `backend/.../test/.../InteractionServiceImplTest.java` — 新增 `@Mock PostVisibilityService`，3 个 likeComment 测试补 `postMapper.selectById` mock，新增 6 个不可见场景 404 测试 |
| 修改 | `backend/.../test/.../DivineCommentServiceTest.java` — 新增 `@Mock PostVisibilityService`，listDivineComments 测试补 `postMapper.selectById` mock |
| 修改 | `backend/.../test/.../VisibilityMatrixIntegrationTest.java` — 新增 `InteractionVisibility` 嵌套类（8 个集成测试） |
| 新增 | `docs/features/interaction-visibility-fix.md` |

## 验证方式

- `mvn compile -q` — 编译通过
- `mvn test -Dtest="InteractionServiceImplTest,InteractionControllerTest"` — 23/23 通过（19 + 4）
- `mvn test -Dtest="DivineCommentServiceTest"` — 17/17 通过
- `mvn test -Dtest="VisibilityMatrixIntegrationTest"` — 36/36 通过（含新增 8 个 InteractionVisibility 测试 + 预存 28 个）
- `mvn test -Dtest="DivineCommentIntegrationTest"` — 11/11 通过

### 集成测试覆盖矩阵

`InteractionVisibility` 嵌套类覆盖：

| 场景 | 端点 | 结果 |
|------|------|------|
| 陌生人点赞私密帖子 | `POST /posts/{id}/likes` | 404 |
| 陌生人收藏私密帖子 | `POST /posts/{id}/favorites` | 404 |
| 陌生人点赞登录可见帖子 | `POST /posts/{id}/likes` | 200 |
| 陌生人收藏登录可见帖子 | `POST /posts/{id}/favorites` | 200 |
| 作者点赞自己的私密帖子 | `POST /posts/{id}/likes` | 200 |
| 作者收藏自己的私密帖子 | `POST /posts/{id}/favorites` | 200 |
| 管理员点赞私密帖子 | `POST /posts/{id}/likes` | 200 |
| 粉丝点赞粉丝可见帖子 | `POST /posts/{id}/likes` | 200 |

## 已知限制

- 与 `CommentServiceImpl.isAdmin()` 使用不同的 `isAdmin()` 实现（后者使用 `RoleEnum` 模式，存在大小写 bug，PR #86 跟踪修复）。当 PR #86 合并后，两者行为将一致。
- `DivineCommentServiceImpl` 仅在 `listDivineComments()` 增加可见性校验，`recommend()` 和 `cancelRecommend()` 未增加（issue 未报告且攻击面小：recommend 需要注册 7 天且帖子评论数 >10，攻击成本高）
