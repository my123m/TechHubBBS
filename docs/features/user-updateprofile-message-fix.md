# 修复：UserController.updateProfile 返回 "success" 而非 "更新成功"

| 属性 | 值 |
|------|-----|
| 分支 | `fix/user-updateprofile-message` |
| 日期 | 2026-06-22 |
| 类型 | Bug 修复 |
| Issue | [#26](https://github.com/my123m/TechHubBBS/issues/26) |
| 影响范围 | 后端 UserController.java, UserControllerTest.java, 前端 user.ts, user.spec.ts |

## 根因分析

`UserController.updateProfile()` 使用 `R.ok(T data)` 重载，其消息硬编码为 `"success"`。同项目其他更新接口（`CategoryController.update()`, `AdminCategoryController.update()`, `NoticeController.update()`）均使用 `R.ok("更新成功")` 重载，返回中文消息且不返回数据。

此外 `updateProfile` 返回 `R<UserDetailVO>` 携带用户数据，与上述更新接口返回 `R<Void>` 不一致。

前端 `SettingsPage.vue` 调用 `updateMe()` 后不使用响应 data，而是通过 `userStore.fetchUserInfo()` 重新拉取用户信息，因此移除数据返回不会影响前端功能。

## 修复方案

将 `updateProfile()` 对齐其他更新接口的模式：

1. 返回类型 `R<UserDetailVO>` → `R<Void>`
2. 删除 `R.ok(userService.getCurrentUser())`，改为 `return R.ok("更新成功")`
3. 前端 API 类型 `R<UserDetailVO>` → `R<null>`（项目约定 `R<Void>` 用 `R<null>`）
4. 前端测试 mock 类型同步更新

## 文件变更

| 操作 | 文件 |
|------|------|
| 修改 | `backend/.../controller/UserController.java` — `updateProfile()` 返回类型改为 `R<Void>`，返回 `R.ok("更新成功")` |
| 修改 | `backend/.../controller/UserControllerTest.java` — 断言 `$.message` 从 `"success"` 改为 `"更新成功"` |
| 修改 | `frontend/src/api/modules/user.ts` — `updateMe` 返回类型 `R<UserDetailVO>` → `R<null>` |
| 修改 | `frontend/src/api/modules/__tests__/user.spec.ts` — mock 类型 `R<UserDetailVO>` → `R<null>` |

## 验证方式

- `mvn test -Dtest=UserControllerTest` — 9 个测试全部通过，BUILD SUCCESS
- `pnpm type-check` — 前端类型检查通过
- `pnpm test:unit` — 63 个文件 843 个测试全部通过

## 已知限制

- 无。`UserDetailVO` import 保留（`getCurrentUser()` 方法仍使用）。
- `PostController.updatePost()` 存在同样模式（`R.ok(data)` 返回 `"success"`），不在本次修复范围内。
