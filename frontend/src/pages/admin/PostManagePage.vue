<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Document } from '@element-plus/icons-vue'
import { adminApi } from '@/api/modules/admin'
import { categoryApi } from '@/api/modules/category'
import { useUserStore } from '@/stores/user'
import { useVisibility } from '@/composables/useVisibility'
import { formatDate } from '@/utils/format'
import LoadingSkeleton from '@/components/common/LoadingSkeleton.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import type { PostVO, Category } from '@/api/types'

const { visibilityLabel } = useVisibility()
const userStore = useUserStore()

// ── Filters ──
const keyword = ref('')
const selectedCategoryId = ref<string | ''>('')

// ── Categories ──
const categories = ref<Category[]>([])

// ── Table data ──
const posts = ref<PostVO[]>([])
const currentPage = ref(1)
const pageSize = ref(15)
const total = ref(0)

// ── State flags ──
const loading = ref(false)
const error = ref<string | null>(null)
const initialState = ref(true)

// ── Type options ──

const postTypeLabel: Record<number, string> = { 0: '普通', 1: '精华', 2: '置顶' }
const postTypeTagType: Record<number, '' | 'danger'> = { 0: '', 1: '', 2: 'danger' }

const visibilityOptions: Record<number, { label: string; class: string }> = {
  0: { label: '公开', class: 'post-manage__vis--public' },
  1: { label: '登录可见', class: 'post-manage__vis--login' },
  2: { label: '粉丝可见', class: 'post-manage__vis--followers' },
  3: { label: '私密', class: 'post-manage__vis--private' },
}

// ── Fetch ──
async function fetchPosts() {
  loading.value = true
  error.value = null

  try {
    const params: Record<string, unknown> = {
      page: currentPage.value,
      size: pageSize.value,
    }
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    if (selectedCategoryId.value !== '') params.categoryId = selectedCategoryId.value

    const res = await adminApi.getPosts(params)
    const data = res.data
    posts.value = data.records
    total.value = data.total
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '加载失败'
    error.value = msg
    posts.value = []
    total.value = 0
  } finally {
    loading.value = false
    initialState.value = false
  }
}

async function fetchCategories() {
  try {
    const res = await categoryApi.getList()
    categories.value = res.data ?? []
  } catch {
    // Non-critical, silently fail
  }
}

// ── Search ──
function handleSearch() {
  currentPage.value = 1
  fetchPosts()
}

function handleReset() {
  keyword.value = ''
  selectedCategoryId.value = ''
  currentPage.value = 1
  fetchPosts()
}

function handlePageChange(page: number) {
  currentPage.value = page
  fetchPosts()
}

// ── Actions ──
async function handleSetType(post: PostVO, type: number) {
  try {
    await adminApi.setPostType(post.id, type)
    post.type = type as PostVO['type']
    ElMessage.success(`已将该帖设为${postTypeLabel[type]}`)
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleLock(post: PostVO) {
  const isLocked = post.status === 0
  const action = isLocked ? '解锁' : '锁定'
  try {
    await ElMessageBox.confirm(
      `确定要${action}该帖子吗？${isLocked ? '解锁后用户可继续评论。' : '锁定后用户将无法评论。'}`,
      `${action}确认`,
      { confirmButtonText: action, cancelButtonText: '取消', type: 'warning' },
    )
    await adminApi.lockPost(post.id)
    post.status = isLocked ? 1 : 0
    ElMessage.success(`${action}成功`)
  } catch {
    // Cancelled or error
  }
}

async function handleDelete(post: PostVO) {
  try {
    await ElMessageBox.confirm(
      `确定要删除帖子「${post.title}」吗？此操作不可撤销。`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'error' },
    )
    await adminApi.deletePost(post.id)
    posts.value = posts.value.filter((p) => p.id !== post.id)
    total.value -= 1
    ElMessage.success('删除成功')
  } catch {
    // Cancelled or error
  }
}

// ── Init ──
onMounted(() => {
  fetchCategories()
  fetchPosts()
})
</script>

<template>
  <div class="post-manage">
    <!-- Header -->
    <div class="post-manage__header">
      <h2 class="post-manage__title">帖子管理</h2>
      <span class="post-manage__count" v-if="!loading && !error">共 {{ total }} 篇</span>
    </div>

    <!-- Filters -->
    <div class="post-manage__filters">
      <div class="post-manage__filter-row">
        <el-input
          v-model="keyword"
          placeholder="搜索帖子标题..."
          :prefix-icon="Search"
          clearable
          class="post-manage__search"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select
          v-model="selectedCategoryId"
          placeholder="按版块筛选"
          clearable
          class="post-manage__category-select"
          @change="handleSearch"
        >
          <el-option
            v-for="cat in categories"
            :key="cat.id"
            :label="cat.name"
            :value="cat.id"
          />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <!-- Loading -->
    <LoadingSkeleton v-if="loading && initialState" variant="table" />

    <!-- Error -->
    <div v-else-if="error" class="post-manage__error">
      <el-alert
        :title="error"
        type="error"
        show-icon
        :closable="false"
      >
        <template #default>
          <el-button type="primary" size="small" @click="fetchPosts">
            重试
          </el-button>
        </template>
      </el-alert>
    </div>

    <!-- Empty -->
    <EmptyState
      v-else-if="posts.length === 0"
      :icon="Document"
      title="暂无帖子"
      description="当前筛选条件下没有匹配的帖子"
    />

    <!-- Table -->
    <div v-else class="post-manage__table-wrap">
      <el-table
        :data="posts"
        stripe
        border
        v-loading="loading"
        element-loading-text="加载中..."
        class="post-manage__table"
        :header-cell-class-name="() => 'post-manage__th'"
      >
        <el-table-column prop="id" label="ID" width="180" show-overflow-tooltip />

        <el-table-column label="标题" min-width="220">
          <template #default="{ row }: { row: PostVO }">
            <router-link
              :to="`/posts/${row.id}`"
              class="post-manage__title-link"
            >
              {{ row.title }}
            </router-link>
          </template>
        </el-table-column>

        <el-table-column label="作者" width="120">
          <template #default="{ row }: { row: PostVO }">
            <router-link
              :to="`/users/${row.authorId}`"
              class="post-manage__author-link"
            >
              {{ row.authorName }}
            </router-link>
          </template>
        </el-table-column>

        <el-table-column prop="categoryName" label="版块" width="100" />

        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }: { row: PostVO }">
            <el-tag
              :type="postTypeTagType[row.type]"
              :effect="row.type === 0 ? 'plain' : 'dark'"
              size="small"
              :class="{ 'post-manage__tag-featured': row.type === 1 }"
            >
              {{ postTypeLabel[row.type] }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }: { row: PostVO }">
            <el-tag
              v-if="row.status === 0"
              type="danger"
              size="small"
            >
              已锁定
            </el-tag>
            <el-tag v-else type="success" size="small" effect="plain">
              正常
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="可见性" width="100" align="center">
          <template #default="{ row }: { row: PostVO }">
            <span
              class="post-manage__vis"
              :class="visibilityOptions[row.visibility]?.class"
            >
              {{ visibilityLabel(row.visibility) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" width="160" align="center">
          <template #default="{ row }: { row: PostVO }">
            <span class="post-manage__time">{{ formatDate(row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }: { row: PostVO }">
            <div class="post-manage__actions">
              <el-select
                :model-value="row.type"
                size="small"
                class="post-manage__type-select"
                @change="(val: number) => handleSetType(row, val)"
              >
                <el-option label="普通" :value="0" />
                <el-option label="精华" :value="1" />
                <el-option label="置顶" :value="2" />
              </el-select>

              <el-button
                :type="row.status === 0 ? 'success' : 'warning'"
                size="small"
                plain
                @click="handleLock(row)"
              >
                {{ row.status === 0 ? '解锁' : '锁定' }}
              </el-button>

              <el-popconfirm
                v-if="userStore.isAdmin"
                title="确定要强制删除吗？"
                confirm-button-text="删除"
                cancel-button-text="取消"
                @confirm="handleDelete(row)"
              >
                <template #reference>
                  <el-button type="danger" size="small" plain>
                    删除
                  </el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <div
        v-if="total > pageSize"
        class="post-manage__pagination"
      >
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next, jumper"
          background
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.post-manage {
  padding: var(--th-spacing-6);

  &__header {
    display: flex;
    align-items: baseline;
    gap: var(--th-spacing-3);
    margin-bottom: var(--th-spacing-6);
  }

  &__title {
    font-size: 20px;
    font-weight: 700;
    color: var(--el-text-color-primary);
    margin: 0;
  }

  &__count {
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }

  // ── Filters ──
  &__filters {
    margin-bottom: var(--th-spacing-6);
  }

  &__filter-row {
    display: flex;
    align-items: center;
    gap: var(--th-spacing-3);
    flex-wrap: wrap;
  }

  &__search {
    width: 260px;
  }

  &__category-select {
    width: 160px;
  }

  // ── Error ──
  &__error {
    margin-bottom: var(--th-spacing-4);
  }

  // ── Table ──
  &__table-wrap {
    background: var(--el-bg-color-overlay);
    border-radius: var(--th-radius-lg);
    border: 1px solid var(--el-border-color-light);
  }

  &__table {
    width: 100%;

    :deep(.post-manage__th) {
      background: var(--el-fill-color);
      color: var(--el-text-color-secondary);
      font-weight: 600;
      font-size: 13px;
    }
  }

  &__title-link {
    color: var(--el-text-color-primary);
    text-decoration: none;
    display: block;
    max-width: 280px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;

    &:hover {
      color: var(--el-color-primary);
    }
  }

  &__author-link {
    color: var(--el-color-primary);
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }

  // ── Type tag ──
  &__tag-featured {
    --el-tag-bg-color: var(--th-color-elite-bg);
    --el-tag-border-color: var(--th-color-elite);
    --el-tag-text-color: var(--th-color-elite);
  }

  // ── Visibility ──
  &__vis {
    font-size: 12px;
    padding: 2px 8px;
    border-radius: var(--th-radius-sm);
    font-weight: 500;

    &--public {
      color: var(--th-visibility-public);
      background: rgba(63, 185, 80, 0.12);
    }

    &--login {
      color: var(--th-visibility-login);
      background: rgba(88, 166, 255, 0.12);
    }

    &--followers {
      color: var(--th-visibility-followers);
      background: rgba(210, 153, 34, 0.12);
    }

    &--private {
      color: var(--th-visibility-private);
      background: rgba(248, 81, 73, 0.12);
    }
  }

  &__time {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  // ── Actions ──
  &__actions {
    display: flex;
    align-items: center;
    gap: var(--th-spacing-2);
  }

  &__type-select {
    width: 90px;
  }

  // ── Pagination ──
  &__pagination {
    display: flex;
    justify-content: center;
    padding: var(--th-spacing-4) 0;
  }
}
</style>
