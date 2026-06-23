<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { categoryApi } from '@/api/modules/category'
import { postApi } from '@/api/modules/post'
import { useUserStore } from '@/stores/user'
import LoadingSkeleton from '@/components/common/LoadingSkeleton.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import PostCard from '@/components/post/PostCard.vue'
import NoticeBanner from '@/components/notice/NoticeBanner.vue'
import type { Category, PostVO } from '@/api/types'

const route = useRoute()
const userStore = useUserStore()

const categoryId = computed(() => route.params.categoryId as string)

// ── Category ──
const category = ref<Category | null>(null)
const categoryLoading = ref(true)
const categoryError = ref(false)

// ── Posts ──
const sort = ref('new')
const page = ref(1)
const size = 20
const posts = ref<PostVO[]>([])
const total = ref(0)
const totalPages = ref(0)
const postLoading = ref(true)
const postError = ref(false)
const postErrorMsg = ref('')

async function fetchCategory() {
  categoryLoading.value = true
  categoryError.value = false
  try {
    const res = await categoryApi.getList()
    if (res.data) {
      category.value = res.data.find((c) => c.id === categoryId.value) ?? null
    }
  } catch {
    categoryError.value = true
  } finally {
    categoryLoading.value = false
  }
}

async function fetchPosts() {
  postLoading.value = true
  postError.value = false
  try {
    const res = await postApi.getList({
      categoryId: categoryId.value,
      sort: sort.value,
      page: page.value,
      size,
    })
    if (res.data) {
      posts.value = res.data.records
      total.value = res.data.total
      totalPages.value = res.data.pages
    }
  } catch (e: unknown) {
    postError.value = true
    postErrorMsg.value = e instanceof Error ? e.message : '加载帖子失败，请稍后重试'
  } finally {
    postLoading.value = false
  }
}

function onSortChange(val: string) {
  sort.value = val
  page.value = 1
  fetchPosts()
}

function onPageChange(p: number) {
  page.value = p
  fetchPosts()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// Watch for categoryId change (navigating between categories on same page instance)
watch(categoryId, () => {
  page.value = 1
  sort.value = 'new'
  fetchCategory()
  fetchPosts()
})

onMounted(() => {
  fetchCategory()
  fetchPosts()
})
</script>

<template>
  <div class="category-page">
    <!-- ══════════════════════════════════════════ -->
    <!-- Category Header                            -->
    <!-- ══════════════════════════════════════════ -->
    <header
      v-if="!categoryLoading && !categoryError && category"
      class="category-header"
    >
      <div class="category-header__info">
        <h1 class="category-header__name">{{ category.name }}</h1>
        <p
          v-if="category.description"
          class="category-header__desc"
        >
          {{ category.description }}
        </p>
      </div>
      <router-link
        v-if="userStore.isLoggedIn"
        :to="`/posts/new?category=${categoryId}`"
      >
        <el-button type="primary">发布新帖</el-button>
      </router-link>
    </header>

    <!-- Category header skeleton -->
    <header
      v-if="categoryLoading"
      class="category-header category-header--skeleton"
    >
      <div class="category-header__info">
        <el-skeleton animated class="category-header__skeleton">
          <template #template>
            <el-skeleton-item variant="text" style="width: 160px; height: 28px" />
            <el-skeleton-item variant="text" style="width: 320px; margin-top: 8px" />
          </template>
        </el-skeleton>
      </div>
    </header>

    <!-- Category error -->
    <el-alert
      v-if="categoryError"
      type="error"
      title="加载版块信息失败"
      show-icon
      :closable="false"
      class="category-page__alert"
    />

    <!-- Notice Banner -->
    <NoticeBanner :category-id="categoryId" />

    <!-- ══════════════════════════════════════════ -->
    <!-- Sort Bar                                   -->
    <!-- ══════════════════════════════════════════ -->
    <div
      v-if="!categoryLoading && !categoryError"
      class="sort-bar"
    >
      <el-radio-group
        v-model="sort"
        size="small"
        @change="onSortChange"
      >
        <el-radio-button value="new">最新</el-radio-button>
        <el-radio-button value="hot">最热</el-radio-button>
      </el-radio-group>
      <span
        v-if="!postLoading && total > 0"
        class="sort-bar__count"
      >
        共 {{ total }} 篇帖子
      </span>
    </div>

    <!-- ══════════════════════════════════════════ -->
    <!-- Post Error                                 -->
    <!-- ══════════════════════════════════════════ -->
    <el-alert
      v-if="postError"
      type="error"
      title="加载失败"
      :description="postErrorMsg"
      show-icon
      :closable="false"
      class="category-page__alert"
    >
      <template #default>
        <el-button
          size="small"
          text
          type="primary"
          @click="fetchPosts"
        >
          重试
        </el-button>
      </template>
    </el-alert>

    <!-- ══════════════════════════════════════════ -->
    <!-- Loading Skeleton                           -->
    <!-- ══════════════════════════════════════════ -->
    <LoadingSkeleton
      v-else-if="postLoading"
      variant="post-list"
    />

    <!-- ══════════════════════════════════════════ -->
    <!-- Empty State                                -->
    <!-- ══════════════════════════════════════════ -->
    <EmptyState
      v-else-if="!postLoading && posts.length === 0"
      title="该版块暂无帖子"
      description="还没有人在这里发布内容，成为第一个发帖的人吧"
      :action-text="userStore.isLoggedIn ? '发布第一个帖子' : undefined"
      :action-route="userStore.isLoggedIn ? `/posts/new?category=${categoryId}` : undefined"
    />

    <!-- ══════════════════════════════════════════ -->
    <!-- Post List                                  -->
    <!-- ══════════════════════════════════════════ -->
    <template v-else>
      <div class="post-list">
        <PostCard
          v-for="post in posts"
          :key="post.id"
          :post="post"
        />
      </div>

      <div
        v-if="totalPages > 1"
        class="pagination-wrapper"
      >
        <el-pagination
          :current-page="page"
          :page-count="totalPages"
          :total="total"
          layout="prev, pager, next"
          background
          @current-change="onPageChange"
        />
      </div>
    </template>
  </div>
</template>

<style lang="scss" scoped>
.category-page {
  max-width: var(--th-content-max-width);
  margin: 0 auto;
  padding: var(--th-spacing-4) var(--th-spacing-4) var(--th-spacing-12);

  &__alert {
    margin-bottom: var(--th-spacing-4);
  }
}

/* ======== Category Header ======== */
.category-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--th-spacing-4);
  padding-bottom: var(--th-spacing-6);
  margin-bottom: var(--th-spacing-4);
  border-bottom: 1px solid var(--el-border-color-light);

  &--skeleton {
    border-bottom-color: transparent;
  }

  &__info {
    flex: 1;
    min-width: 0;
  }

  &__name {
    margin: 0;
    font-size: 24px;
    font-weight: 700;
    font-family: var(--th-font-heading);
    color: var(--el-text-color-primary);
    line-height: 1.3;
  }

  &__desc {
    margin: var(--th-spacing-2) 0 0;
    font-size: 14px;
    color: var(--el-text-color-secondary);
    line-height: 1.6;
  }

  &__skeleton {
    width: 100%;
  }
}

/* ======== Sort Bar ======== */
.sort-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--th-spacing-4);
  padding-bottom: var(--th-spacing-3);
  border-bottom: 1px solid var(--el-border-color-light);

  &__count {
    font-size: 13px;
    color: var(--el-text-color-placeholder);
    flex-shrink: 0;
  }
}

/* ======== Post List ======== */
.post-list {
  border-radius: var(--th-radius-md);
  border: 1px solid var(--el-border-color-light);
  overflow: hidden;
}

/* ======== Pagination ======== */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: var(--th-spacing-6) 0 0;
}
</style>
