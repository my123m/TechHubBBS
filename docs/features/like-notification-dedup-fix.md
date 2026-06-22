# 修复：点赞通知去重逻辑导致后续点赞通知被静默丢弃

| 属性 | 值 |
|------|-----|
| 分支 | `fix/like-notification-dedup` |
| 日期 | 2026-06-21 |
| 类型 | Bug 修复 |
| Issue | [#17](https://github.com/my123m/TechHubBBS/issues/17) |
| 影响范围 | 后端 InteractionServiceImpl.java |

## 根因分析

`InteractionServiceImpl.likePost()` 和 `likeComment()` 中，点赞通知的去重查询只检查 `(接收者ID, LIKE, 源ID)` 三维组合，缺少"是谁点赞"的维度。一旦某个帖子/评论的第一条点赞产生了通知，此后所有其他用户的点赞都不会再产生通知——因为 `(接收者, LIKE, 源ID)` 已存在。

对比 `FollowServiceImpl.follow()`，关注通知的 `sourceId` 直接就是关注者 ID，三维去重等价于四维，所以关注通知没问题。但点赞通知的 `sourceId` 是帖子/评论 ID（非点赞者 ID），缺少点赞者维度导致误判。

## 修复方案

**不做去重，每次点赞都直接创建通知。** 理由：

1. 改动最小，不涉及数据库 schema 变更
2. `user_like` 表的唯一键约束已保证 `(userId, targetType, targetId)` 唯一，同一用户对同一帖子/评论只能点赞一次，不会走到通知逻辑的重复调用路径
3. 唯一可能重复的场景（用户取消点赞后再点赞）是边缘情况，语义上"B 重新点赞"产生新通知也可接受

### 改动对比

```java
// 修复前（错误的去重查询）
Long count = notificationMapper.selectCount(
    new LambdaQueryWrapper<Notification>()
        .eq(Notification::getUserId, post.getAuthorId())
        .eq(Notification::getType, "LIKE")
        .eq(Notification::getSourceId, postId));
if (count == null || count == 0) {
    notificationService.create(...);
}

// 修复后（直接创建通知）
notificationService.create(post.getAuthorId(), "LIKE", postId, "赞了你的帖子");
```

## 文件变更

| 操作 | 文件 |
|------|------|
| 修改 | `backend/.../service/impl/InteractionServiceImpl.java` — `likePost()` 和 `likeComment()` 移除 selectCount 去重；移除不再使用的 NotificationMapper 注入和 Notification import |
| 新增 | `backend/.../service/impl/InteractionServiceImplTest.java` — 10 个测试覆盖多用户点赞、自我点赞、异常容错等场景 |
| 新增 | `docs/features/like-notification-dedup-fix.md` — 本文档 |

## 验证方式

- `mvn compile` — 编译通过
- `mvn test -Dtest="InteractionServiceImplTest"` — 10 个测试全部通过
- `mvn test -Dtest="InteractionControllerTest,NotificationServiceImplTest"` — 回归测试不破坏

## 已知限制

用户取消点赞后再次点赞会重复产生通知（边缘场景，语义上可接受）。如需彻底消除此偶尔重复，需给 `notification` 表加 `act_user_id` 字段实现四维去重，但涉及数据库 schema 变更（L2 级别，另案处理）。
