# 修复：Category 雪花 ID 精度丢失导致版块信息不显示

| 属性 | 值 |
|------|-----|
| 分支 | `fix/category-snowflake-id-precision` |
| 日期 | 2026-06-23 |
| 类型 | Bug 修复 |
| Issue | [#78](https://github.com/my123m/TechHubBBS/issues/78) |
| 影响范围 | 前端类型定义、API 模块、页面组件、测试文件 |

## 根因分析

后端 `JacksonConfig` 将所有 `Long` 序列化为 `String`（防止 JS 精度丢失），`Category` 实体使用 `@TableId(type = IdType.ASSIGN_ID)` 雪花 ID（19 位）。但前端 `Category` 类型声明 `id: number`，与 API 实际返回的 `string` 不符。

`CategoryPage.vue` 用 `Number(route.params.categoryId)` 将路由参数转为 `number`，对 19 位雪花 ID 造成精度丢失（JS `Number` 安全整数上限为 2^53 ≈ 16 位）。随后 `res.data.find((c) => c.id === categoryId.value)` 尝试将 API 返回的 `string` ID 与精度丢失的 `number` 比较，**永远匹配失败**，导致版块名称和描述永远不显示。

同时 `fetchPosts()` 将精度丢失的 `number` 作为 `categoryId` 查询参数发送给后端，后端收到错误的版块 ID，返回错误版块的帖子或空列表。

### 同类问题

`PostEditPage.vue` 同样使用 `Number(post.categoryId)` 将 `PostVO.categoryId`（`string` 类型）转为 `number`，导致编辑帖子时版块下拉框无法匹配正确的选项。

## 修复方案

将所有 `categoryId` 和 `Category.id` 类型统一为 `string`，与后端序列化行为保持一致：

1. **类型定义** — `Category.id`、`PostListParams.categoryId`、`PostCreateRequest.categoryId`、`PostUpdateRequest.categoryId`、`PostDraft.categoryId`、`DraftSaveRequest.categoryId`、`DraftData.categoryId` 全部改为 `string`
2. **API 模块** — `categoryApi.update/toggleStatus/delete` 的 `id` 参数改为 `string`；`adminApi.getPosts` 的 `categoryId` 改为 `string`
3. **页面组件** — 移除 `CategoryPage.vue` 和 `PostEditPage.vue` 中的 `Number()` 转换，直接使用 `string` 类型
4. **管理后台** — `CategoryManagePage.vue` 的 `editingId` 和 `loadingIds` 改为 `string`；`PostManagePage.vue` 的 `selectedCategoryId` 改为 `string`

## 文件变更

| 操作 | 文件 |
|------|------|
| 修改 | `frontend/src/api/types/category.ts` — `Category.id: number` → `string` |
| 修改 | `frontend/src/api/types/post.ts` — `PostListParams.categoryId`、`PostCreateRequest.categoryId`、`PostUpdateRequest.categoryId` → `string` |
| 修改 | `frontend/src/api/types/draft.ts` — `PostDraft.categoryId`、`DraftSaveRequest.categoryId` → `string` |
| 修改 | `frontend/src/composables/useDraft.ts` — `DraftData.categoryId` → `string` |
| 修改 | `frontend/src/api/modules/category.ts` — `update/toggleStatus/delete` 的 `id` → `string` |
| 修改 | `frontend/src/api/modules/admin.ts` — `getPosts` 的 `categoryId` → `string` |
| 修改 | `frontend/src/pages/category/CategoryPage.vue` — 移除 `Number()` 转换，直接使用 `route.params.categoryId as string` |
| 修改 | `frontend/src/pages/post/PostEditPage.vue` — `form.categoryId` → `string`，移除 `Number()` 转换 |
| 修改 | `frontend/src/pages/post/PostCreatePage.vue` — `form.categoryId` → `string` |
| 修改 | `frontend/src/pages/admin/PostManagePage.vue` — `selectedCategoryId` → `string` |
| 修改 | `frontend/src/pages/admin/CategoryManagePage.vue` — `editingId` → `string`，`loadingIds` → `Set<string>` |
| 修改 | 7 个测试文件 — 测试数据中的 `categoryId` 和 `id` 从 `number` 改为 `string` |

## 验证方式

- `pnpm run type-check` — 类型检查通过，无错误
- `pnpm run test:unit` — 845 个测试全部通过

## 已知限制

- 无。此修复为纯前端类型对齐，不涉及后端变更
