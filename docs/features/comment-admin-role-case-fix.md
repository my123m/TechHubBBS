# 修复：CommentServiceImpl 管理员/版主权限大小写不匹配

| 属性 | 值 |
|------|-----|
| 分支 | `fix/comment-admin-role-case` |
| 日期 | 2026-06-23 |
| 类型 | Bug 修复（安全/权限） |
| Issue | [#65](https://github.com/my123m/TechHubBBS/issues/65) |
| 影响范围 | 后端 `CommentServiceImpl.java`（2 行），新增 `CommentServiceImplTest.java` |

## 根因分析

`CommentServiceImpl` 的 `isAdmin()` 和 `isAdminOrModerator()` 使用 `RoleEnum.ADMIN.getCode()`（返回小写 `"admin"`）与 `SecurityUtils.getCurrentRole()`（返回大写 `"ADMIN"`，来自 Spring Security `ROLE_ADMIN` 去除 `ROLE_` 前缀）进行 `equals` 比对，大小写不匹配导致判断永远返回 false。

同项目 `PostServiceImpl`、`UserServiceImpl`、`RecommendationServiceImpl`、`CategoryNoticeServiceImpl`、`DivineCommentServiceImpl` 均使用大写字符串直接比对：`"ADMIN".equals(role)`，唯独 `CommentServiceImpl` 使用了枚举 `getCode()` 值。

`RoleEnum` 定义：`ADMIN("admin")`、`MODERATOR("moderator")`（均为小写 code），而 `SecurityUtils.getCurrentRole()` 返回大写（来自 JWT claim 或 Spring Security GrantedAuthority）。

## 受影响调用点

| 方法 | 行号 | 影响 |
|------|------|------|
| `delete()` | L135 | 管理员/版主无法删除他人评论 → HTTP 403 |
| `listByPost()` | L54 | `isAdmin` 永远 false → 可见性判断降级，管理员看不到私密帖子评论 |
| `create()` | L85 | 同上 |

## 修复方案

将两处方法改为与项目其他 5 个 Service 一致的大写字面量比对，移除已不再使用的 `RoleEnum` import。

## 文件变更

| 操作 | 文件 |
|------|------|
| 修改 | `backend/src/main/java/com/techhub/service/impl/CommentServiceImpl.java` |
| 新增 | `backend/src/test/java/com/techhub/service/CommentServiceImplTest.java` |
| 新增 | `docs/features/comment-admin-role-case-fix.md`（本文件） |

## 验证方式

- `mvn -q -pl backend compile` — 编译通过
- `mvn -q -pl backend test` — 新增 CommentServiceImplTest 通过（管理员/版主删除他人评论、isAdmin 正确传递），现有测试无回归

## 已知限制

- `RoleEnum` 的 `getCode()` 返回小写是设计意图（用于数据库存储等场景），`SecurityUtils.getCurrentRole()` 返回大写是 Spring Security 约定。两者不一致是全局性隐患，但项目内已有 5 个 Service 统一使用大写比对，本次修复仅补齐 `CommentServiceImpl`。建议后续将 `SecurityUtils` 角色返回规范化（统一大小写）以避免类似问题。
