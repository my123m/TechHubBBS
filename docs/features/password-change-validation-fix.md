# 修复：PasswordChangeRequest.newPassword 缺少最小长度校验

| 属性 | 值 |
|------|-----|
| 分支 | `fix/password-change-validation` |
| 日期 | 2026-06-21 |
| 类型 | 安全修复 |
| Issue | [#20](https://github.com/my123m/TechHubBBS/issues/20) |
| 影响范围 | 后端 PasswordChangeRequest.java, UserControllerTest.java |

## 根因分析

`PasswordChangeRequest.newPassword` 仅标注 `@NotBlank`（非空校验），未添加 `@Size(min=6)` 长度约束。注册接口 `RegisterRequest.password` 要求密码长度至少 6 字符，此不一致导致用户修改密码时可设置极短密码（如单字符 "a"），削弱账户安全性。

Controller 入口 `UserController.changePassword()` 使用 `@Valid` 校验请求体，因此 DTO 缺少约束直接导致请求绕过长度校验。

## 修复方案

为 `PasswordChangeRequest.newPassword` 添加与 `RegisterRequest.password` 完全一致的 `@Size(min=6, max=100)` 约束，统一密码策略。

### 测试

`UserControllerTest` 新增 `changePassword_ShortNewPassword_Returns400` 用例，验证短密码（"a"）经 `@Valid` 校验后返回 400 状态码及对应错误消息。

## 文件变更

| 操作 | 文件 |
|------|------|
| 修改 | `backend/.../dto/auth/PasswordChangeRequest.java` — `newPassword` 增加 `@Size(min=6, max=100)` |
| 修改 | `backend/.../controller/UserControllerTest.java` — 新增短密码 400 测试 |
| 新增 | `docs/features/password-change-validation-fix.md` |

## 验证方式

- `mvn compile -q` — 编译通过
- `mvn test -Dtest="UserControllerTest"` — 全部用例通过（含新增 1 个）
