# 修复：getFavorites() 分页总数在过滤已删除帖子前计算，导致分页不准确

| 属性 | 值 |
|------|-----|
| 分支 | `fix/favorites-pagination-deleted` |
| 日期 | 2026-06-21 |
| 类型 | Bug 修复 |
| Issue | [#19](https://github.com/my123m/TechHubBBS/issues/19) |
| 影响范围 | 后端 FavoriteMapper.java, UserServiceImpl.java, FavoritesPaginationIntegrationTest.java |

## 根因分析

### 分页总数与记录数不一致

`UserServiceImpl.getFavorites()` 使用 `favoriteMapper.selectPage()` 分页查询收藏记录，`favResult.getTotal()` 包含用户所有收藏（含已删除帖子的收藏）。但返回给前端的 `records` 在应用层过滤了 `post.deleted == 0`，导致：

- `total` 和 `pages` 基于全部收藏数计算（偏大）
- `records` 仅包含未删除帖子的收藏（可能少于 `size`）
- 翻到末页时出现空页或少量记录

### 为何不在应用层修复

在应用层做"查全量再手动分页"会违反 pageSize 上限 50 的设计，且在大收藏量下产生不必要的 DB 和内存开销。正确方案是将过滤下沉到 SQL 层，让分页插件基于过滤后的结果集计算 `total` 和 `LIMIT`。

## 修复方案

### 新增 Mapper 方法——SQL JOIN 过滤

`FavoriteMapper` 新增 `selectVisibleFavorites()` 方法，使用 `@Select` + `INNER JOIN post` + `p.deleted = 0` 在 SQL 层过滤已删除帖子：

```java
@Select("SELECT f.* FROM favorite f " +
        "INNER JOIN post p ON f.post_id = p.id " +
        "WHERE f.user_id = #{userId} AND p.deleted = 0 " +
        "ORDER BY f.create_time DESC")
Page<Favorite> selectVisibleFavorites(Page<Favorite> page, @Param("userId") Long userId);
```

MyBatis-Plus 分页插件自动对该 JOIN 查询生成正确的 `COUNT` + `LIMIT`，`getTotal()` 此时为过滤后准确值。

### 服务层适配

`UserServiceImpl.getFavorites()` 改用 `favoriteMapper.selectVisibleFavorites()`。移除应用层 `.filter(p -> p.getDeleted() == 0)`（过滤已下沉 SQL，且 `selectBatchIds` 受 `@TableLogic` 兜底）。

## 文件变更

| 操作 | 文件 |
|------|------|
| 修改 | `backend/.../mapper/FavoriteMapper.java` — 新增 `selectVisibleFavorites()` 方法 |
| 修改 | `backend/.../service/impl/UserServiceImpl.java` — `getFavorites()` 改用 `selectVisibleFavorites`，移除应用层 filter |
| 新增 | `backend/.../integration/FavoritesPaginationIntegrationTest.java` — 4 个分页元数据验证用例 |
| 新增 | `backend/src/test/resources/sql/h2-favorites-pagination-data.sql` — 测试种子数据（15 帖含 5 已删除） |
| 新增 | `docs/features/favorites-pagination-fix.md` |

## 验证方式

- `mvn compile -q` — 编译通过
- `mvn test -Dtest="FavoritesPaginationIntegrationTest"` — 4 个测试全部通过
- `mvn test -Dtest="UserControllerTest"` — 现有 getFavorites mock 测试不受影响（签名不变）

## 已知限制

- `PostServiceImpl.listPosts()` 和 `UserServiceImpl.getUserPosts()` 存在同类分页 total 不准确问题（应用层 visible 过滤后 total 未更新），参见 Issue [#14](https://github.com/my123m/TechHubBBS/issues/14)，本次不在修复范围内
