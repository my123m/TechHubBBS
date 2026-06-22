# 修复：相关帖子相似度为 null 时显示误导性“0%”

| 属性 | 值 |
|------|-----|
| 分支 | `fix/post-similarity-null-display` |
| 日期 | 2026-06-21 |
| 类型 | 修复 |
| 影响范围 | 前端 PostDetailPage.vue · PostDetailPage.test.ts |
| 关联 Issue | [#29](https://github.com/my123m/TechHubBBS/issues/29) |

## 修复的缺陷

`PostDetailPage.vue` 相关推荐区域，`RelatedPostVO.similarityScore` 类型为 `number | null`。此前使用 `?? 0` 兜底：
- TS 编译错误（TS18047）已通过 `?? 0` 修复
- **但 UX 问题未解决**：当 `similarityScore` 为 null（如尚未计算相似度）时仍显示"相似度 0%"，误导用户以为该帖子与当前帖毫无关联

## 修复内容

### PostDetailPage.vue（第 757-759 行）

在 `<span class="post-detail__related-similarity">` 上增加 `v-if="related.similarityScore != null"`，当 `similarityScore` 为 null 时不渲染相似度标签。TS 自动收窄类型，无需 `?? 0` 兜底。

```vue
<!-- 改后 -->
<span v-if="related.similarityScore != null" class="post-detail__related-similarity">
  相似度 {{ (related.similarityScore * 100).toFixed(0) }}%
</span>
```

### PostDetailPage.test.ts

新增测试用例「does not render similarity span when score is null」：
- mock 第一个 RelatedPostVO 的 `similarityScore: null`
- 断言该卡片中 `.post-detail__related-similarity` 不存在
- 断言同列表中非 null 的第二个卡片仍正常显示相似度

## 不涉及的改动

- **Issue #27**（PostEditPage categoryId 类型错误）已在 commit `9c2b2f3`（经 PR #32 合入 upstream/dev）中修复，无需额外代码改动。PR 描述中说明并请求关闭 #27。

## 验证方式

- `pnpm type-check`（vue-tsc）：0 error
- `pnpm test:unit --run`（vitest）：40/40 通过
- 手动验证：进入帖子详情页，若某相关帖子的 `similarityScore` 为 null，该卡片仅显示标题，不显示相似度标签

## 已知限制

- 无
