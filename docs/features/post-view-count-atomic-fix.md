# 修复：浏览量递增改用原子 SQL 更新，消除竞态条件

| 属性 | 值 |
|------|-----|
| 分支 | `fix/post-view-count-race` |
| 日期 | 2026-06-22 |
| 类型 | Bug 修复 |
| Issue | [#21](https://github.com/my123m/TechHubBBS/issues/21) |
| 影响范围 | 后端 PostServiceImpl.java, PostServiceTest.java |

## 根因分析

### 读-改-写竞态条件

`PostServiceImpl.getPostDetail()` 浏览量递增采用 `post.setViewCount(post.getViewCount() + 1)` + `postMapper.updateById(post)` 三步模式，非原子操作。并发请求下多个线程同时读取相同 `viewCount` 值，各自递增后回写，后写入覆盖先写入，导致漏计数。

并发场景（两个请求同时进入）：
```
线程A: select → viewCount=100
线程B: select → viewCount=100
线程A: set 101 → updateById
线程B: set 101 → updateById (覆盖，漏计一次)
```

### 影响

- 高并发帖子浏览量偏低
- 首页热门排序公式 `viewCount * 1 + likeCount * 3 + commentCount * 5` 受偏低的浏览量影响
- 推荐系统冷启动热度计算依赖准确的浏览量

## 修复方案

将读-改-写替换为单条原子 SQL 更新：

```java
postMapper.update(null, new LambdaUpdateWrapper<Post>()
        .eq(Post::getId, postId)
        .setSql("view_count = view_count + 1"));
```

`setSql("view_count = view_count + 1")` 生成 `UPDATE post SET view_count = view_count + 1 WHERE id = ? AND deleted = 0`，由数据库行级锁保证原子性，消除竞态条件。

同时在内存中同步递增 `post.viewCount`（`post.setViewCount(post.getViewCount() + 1)`），确保返回的 `PostVO` 显示正确的递增后浏览量。此值与 DB 实际值可能因并发有微小偏差，但在可接受范围内。

## 文件变更

| 操作 | 文件 |
|------|------|
| 修改 | `backend/src/main/java/com/techhub/service/impl/PostServiceImpl.java` — 导入 `LambdaUpdateWrapper`；浏览量递增改用 `setSql` 原子更新 |
| 修改 | `backend/src/test/java/com/techhub/service/PostServiceTest.java` — 导入 `LambdaUpdateWrapper`；`returnsPostDetailAndIncrementsViews` 测试的 mock/verify 从 `updateById(post)` 改为 `update(null, wrapper)` |
| 新增 | `docs/features/post-view-count-atomic-fix.md` |

## 验证方式

- `mvn compile -q` — 编译通过
- `mvn test -Dtest="PostServiceTest"` — getPostDetail 相关测试通过
- `mvn test -Dtest="PostControllerTest"` — 控制器 mock 层不受影响
- 集成测试 `PostLifecycleIntegrationTest.getPostDetail_ReturnsFullDetail` 期望 `viewCount == 1`，原子更新下单线程行为一致，无需改动

## 已知限制

- `setSql` 直接写入 SQL 片段，表名/列名变更时需手动维护（项目未启用列名自动映射的替代方案，权衡后选择此方案）
- 返回给调用方的 `viewCount` 值为内存递增结果，在高并发下可能与 DB 实际值存在微小偏差（例如两个并发请求看到的返回值仅差 1，而 DB 已递增 2），属于可接受的近似行为
