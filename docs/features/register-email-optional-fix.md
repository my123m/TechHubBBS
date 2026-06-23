# 修复：注册时邮箱字段应为选填

| 属性 | 值 |
|------|-----|
| 分支 | `fix/register-email-optional` |
| 日期 | 2026-06-23 |
| 类型 | Bug 修复 |
| Issue | [#88](https://github.com/my123m/TechHubBBS/issues/88) |
| 影响范围 | 后端 RegisterRequest.java, AuthServiceImpl.java, AuthControllerTest.java, 数据库 schema |

## 根因分析

注册接口中邮箱字段标记为选填（前端 placeholder 为"邮箱（选填）"，TS 类型 `email?: string`），但后端存在三层阻碍导致不填邮箱时注册失败：

1. **DTO 校验**：`RegisterRequest.email` 标注 `@NotBlank`，空值直接被 Spring Validation 拦截返回 400。
2. **Service 逻辑**：`AuthServiceImpl.register()` 无条件执行邮箱唯一性检查，email 为 null 时 `eq(User::getEmail, null)` 行为不可预期。
3. **数据库约束**：`user.email` 列定义为 `NOT NULL`，即使绕过校验也无法插入空值。

## 修复方案

### 后端校验

移除 `RegisterRequest.email` 的 `@NotBlank` 注解，保留 `@Email` 格式校验（Jakarta Validation 对 null 值自动跳过 `@Email` 校验）。

### Service 空值保护

`AuthServiceImpl.register()` 中邮箱唯一性检查增加 null/blank 判断，仅在用户实际填写邮箱时执行查重。写入 User 实体时将空白字符串统一转为 null。

### 数据库 Schema

将 4 个 schema 文件中 `user.email` 列从 `NOT NULL` 改为 `DEFAULT NULL`：
- `backend/src/main/resources/db/schema.sql`（MySQL 主库）
- `backend/src/test/resources/sql/h2-schema.sql`（H2 测试库）
- `sqlserver/02_create_tables.sql`（SQL Server）
- `sqlserver/00_combined.sql`（SQL Server 合并脚本）

MySQL 的 `UNIQUE` 约束允许多个 NULL 值，唯一约束无需修改。

### 测试

`AuthControllerTest` 新增 `register_NoEmail_Returns200` 用例，验证不携带 email 字段时注册接口返回 200。

## 文件变更

| 操作 | 文件 |
|------|------|
| 修改 | `backend/.../dto/auth/RegisterRequest.java` — 移除 email 的 `@NotBlank` |
| 修改 | `backend/.../service/impl/AuthServiceImpl.java` — 邮箱查重加空值保护，空白转 null |
| 修改 | `backend/.../controller/AuthControllerTest.java` — 新增无邮箱注册成功测试 |
| 修改 | `backend/src/main/resources/db/schema.sql` — email 列改为 `DEFAULT NULL` |
| 修改 | `backend/src/test/resources/sql/h2-schema.sql` — email 列改为 `DEFAULT NULL` |
| 修改 | `sqlserver/02_create_tables.sql` — email 列改为 `NULL` |
| 修改 | `sqlserver/00_combined.sql` — email 列改为 `NULL` |
| 新增 | `docs/features/register-email-optional-fix.md` |

## 验证方式

- `mvn compile -q` — 编译通过
- `mvn test -Dtest="AuthControllerTest"` — 全部用例通过（含新增 1 个）
