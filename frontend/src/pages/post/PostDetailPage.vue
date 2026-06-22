<script setup lang="ts">
/**
 * PostDetailPage.vue — Full-featured post detail page with 7 sections:
 *   1. Header (title, author, stats)
 *   2. Content (MdViewer)
 *   3. Actions (like, favorite, edit, delete, share)
 *   4. Divine Comments (gold section, shown when divineCommentCount > 0)
 *   5. AI Panel
 *   6. Comments (list + nested replies + comment form)
 *   7. Related Posts (horizontal scroll mini cards)
 */
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  View, Star, ChatDotRound, Medal, Share, Edit, Delete,
  ChatLineSquare, WarningFilled,
  Lock, Promotion,
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { postApi } from '@/api/modules/post'
import { commentApi } from '@/api/modules/comment'
import { recommendationApi } from '@/api/modules/recommendation'
import { ApiError } from '@/api'
import { formatRelativeTime, formatNumber } from '@/utils/format'
import type { PostVO, CommentVO, RelatedPostVO } from '@/api/types'

import MdViewer from '@/components/markdown/MdViewer.vue'
import AiSummaryPanel from '@/components/ai/AiSummaryPanel.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import DivineCommentBadge from '@/components/post/DivineCommentBadge.vue'

// ---------------------------------------------------------------------------
// Router & Stores
// ---------------------------------------------------------------------------
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const postId = computed(() => route.params.id as string)

// ---------------------------------------------------------------------------
// Reactive State
// ---------------------------------------------------------------------------
const loading = ref(true)
const errorCode = ref<number | null>(null)

// Post
const post = ref<PostVO | null>(null)
const liked = ref(false)
const favorited = ref(false)
const likeCount = ref(0)

// Divine comments
const divineComments = ref<CommentVO[]>([])
const divineLoading = ref(false)

// Comments
const comments = ref<CommentVO[]>([])
const commentTotal = ref(0)
const commentPage = ref(1)
const commentPageSize = ref(10)
const commentLoading = ref(false)
const commentContent = ref('')
const commentSubmitting = ref(false)

// Inline reply state
const replyTarget = ref<string | null>(null) // comment id being replied to
const replyContent = ref('')
const replySubmitting = ref(false)

// Related posts
const relatedPosts = ref<RelatedPostVO[]>([])
const relatedLoading = ref(false)

// ---------------------------------------------------------------------------
// Computed
// ---------------------------------------------------------------------------
const isAuthor = computed(() => {
  if (!post.value || !userStore.userInfo) return false
  return post.value.authorId === userStore.userInfo.id
})

// ---------------------------------------------------------------------------
// Section 1+2: Fetch post detail
// ---------------------------------------------------------------------------
async function fetchPost() {
  loading.value = true
  errorCode.value = null
  try {
    const res = await postApi.getDetail(postId.value)
    post.value = res.data
    liked.value = res.data.liked
    favorited.value = res.data.favorited
    likeCount.value = res.data.likeCount
    document.title = res.data.title + ' - TechHub'
  } catch (err: unknown) {
    if (err instanceof ApiError) {
      errorCode.value = err.code
    } else {
      errorCode.value = 500
    }
  } finally {
    loading.value = false
  }
}

// ---------------------------------------------------------------------------
// Section 4: Divine comments
// ---------------------------------------------------------------------------
async function fetchDivineComments() {
  if (!post.value || post.value.divineCommentCount === 0) return
  divineLoading.value = true
  try {
    const res = await commentApi.getDivineComments(postId.value)
    divineComments.value = res.data ?? []
  } catch {
    divineComments.value = []
  } finally {
    divineLoading.value = false
  }
}

// ---------------------------------------------------------------------------
// Section 6: Comments
// ---------------------------------------------------------------------------
async function fetchComments(page = 1) {
  commentLoading.value = true
  commentPage.value = page
  try {
    const res = await commentApi.getList(postId.value, {
      page,
      size: commentPageSize.value,
    })
    comments.value = res.data?.records ?? []
    commentTotal.value = res.data?.total ?? 0
  } catch {
    comments.value = []
    commentTotal.value = 0
  } finally {
    commentLoading.value = false
  }
}

async function handleCommentSubmit() {
  const content = commentContent.value.trim()
  if (!content) return
  commentSubmitting.value = true
  try {
    await commentApi.create(postId.value, { content })
    commentContent.value = ''
    ElMessage.success('评论成功')
    // Refresh list
    await fetchComments(1)
    // Increment local count
    if (post.value) post.value.commentCount += 1
  } catch (err: unknown) {
    if (err instanceof ApiError) {
      ElMessage.error(err.message)
    }
  } finally {
    commentSubmitting.value = false
  }
}

/** Open inline reply form for a specific comment */
function startReply(commentId: string) {
  replyTarget.value = commentId
  replyContent.value = ''
}

function cancelReply() {
  replyTarget.value = null
  replyContent.value = ''
}

async function handleReplySubmit(parentId: string) {
  const content = replyContent.value.trim()
  if (!content) return
  replySubmitting.value = true
  try {
    await commentApi.create(postId.value, { content, parentId })
    replyContent.value = ''
    replyTarget.value = null
    ElMessage.success('回复成功')
    await fetchComments(commentPage.value)
    if (post.value) post.value.commentCount += 1
  } catch (err: unknown) {
    if (err instanceof ApiError) {
      ElMessage.error(err.message)
    }
  } finally {
    replySubmitting.value = false
  }
}

// ---------------------------------------------------------------------------
// Section 7: Related Posts
// ---------------------------------------------------------------------------
async function fetchRelatedPosts() {
  relatedLoading.value = true
  try {
    const res = await recommendationApi.getRelatedPosts(postId.value, 5)
    relatedPosts.value = res.data ?? []
  } catch {
    relatedPosts.value = []
  } finally {
    relatedLoading.value = false
  }
}

// ---------------------------------------------------------------------------
// Section 3: Actions
// ---------------------------------------------------------------------------
async function handleLike() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  const currentLiked = liked.value
  // Optimistic update
  liked.value = !currentLiked
  likeCount.value += currentLiked ? -1 : 1
  try {
    if (currentLiked) {
      await postApi.unlike(postId.value)
    } else {
      await postApi.like(postId.value)
    }
  } catch {
    // Rollback on failure
    liked.value = currentLiked
    likeCount.value += currentLiked ? 1 : -1
  }
}

async function handleFavorite() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  const currentFavorited = favorited.value
  favorited.value = !currentFavorited
  try {
    if (currentFavorited) {
      await postApi.unfavorite(postId.value)
    } else {
      await postApi.favorite(postId.value)
    }
  } catch {
    // Rollback on failure
    favorited.value = currentFavorited
  }
}

function handleEdit() {
  router.push(`/posts/${postId.value}/edit`)
}

async function handleDelete() {
  try {
    await ElMessageBox.confirm(
      '确定要删除这篇帖子吗？此操作不可恢复。',
      '确认删除',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
      },
    )
    await postApi.remove(postId.value)
    ElMessage.success('帖子已删除')
    router.push('/')
  } catch (err: unknown) {
    // Cancelled or error
    if (err instanceof ApiError) {
      ElMessage.error(err.message)
    }
  }
}

async function handleShare() {
  try {
    await navigator.clipboard.writeText(window.location.href)
    ElMessage.success('链接已复制到剪贴板')
  } catch {
    // Fallback for older browsers
    const textarea = document.createElement('textarea')
    textarea.value = window.location.href
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    ElMessage.success('链接已复制到剪贴板')
  }
}

// ---------------------------------------------------------------------------
// Initialisation
// ---------------------------------------------------------------------------
onMounted(async () => {
  await fetchPost()
  if (post.value) {
    // Fire independent requests in parallel
    await Promise.allSettled([
      fetchDivineComments(),
      fetchComments(1),
      fetchRelatedPosts(),
    ])
    // 消息通知跳转携带 hash 时滚动到对应评论
    if (route.hash) {
      nextTick(() => scrollToHash(route.hash))
    }
  }
})

watch(() => route.hash, (hash) => {
  if (hash) {
    nextTick(() => scrollToHash(hash))
  }
})

/** 滚动到 hash 对应的评论元素，带重试机制（评论列表可能分页延迟加载） */
function scrollToHash(hash: string, retries = 5) {
  const id = hash.startsWith('#') ? hash.slice(1) : hash
  const el = document.getElementById(id)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'center' })
  } else if (retries > 0) {
    setTimeout(() => scrollToHash(hash, retries - 1), 300)
  }
}
</script>

<template>
  <div class="post-detail">
    <!-- ============================================================ -->
    <!-- LOADING STATE                                                  -->
    <!-- ============================================================ -->
    <div v-if="loading" class="post-detail__loading">
      <div class="post-detail__loading-header">
        <div class="skeleton-line skeleton-line--title" />
        <div class="skeleton-line skeleton-line--meta" />
        <div class="skeleton-line skeleton-line--meta" style="width: 40%" />
      </div>
      <div class="post-detail__loading-content">
        <div class="skeleton-line skeleton-line--para" />
        <div class="skeleton-line skeleton-line--para" style="width: 90%" />
        <div class="skeleton-line skeleton-line--para" style="width: 80%" />
        <div class="skeleton-line skeleton-line--para" style="width: 60%" />
      </div>
    </div>

    <!-- ============================================================ -->
    <!-- ERROR / NOT FOUND                                              -->
    <!-- ============================================================ -->
    <div v-else-if="errorCode" class="post-detail__error">
      <EmptyState
        :icon="WarningFilled"
        :title="errorCode === 404 ? '帖子不存在' : '无权访问该帖子'"
        :description="errorCode === 404
          ? '该帖子可能已被删除、隐藏，或您没有查看权限。'
          : '您没有权限查看这篇帖子，请登录或联系作者。'"
        action-text="返回首页"
        action-route="/"
      />
    </div>

    <!-- ============================================================ -->
    <!-- MAIN CONTENT                                                   -->
    <!-- ============================================================ -->
    <template v-else-if="post">
      <!-- ======================================================== -->
      <!-- SECTION 1 — HEADER                                        -->
      <!-- ======================================================== -->
      <header class="post-detail__header">
        <h1 class="post-detail__title">{{ post.title }}</h1>

        <div class="post-detail__meta">
          <div class="post-detail__author">
            <router-link :to="`/users/${post.authorId}`" class="post-detail__author-link">
              <UserAvatar
                :src="post.authorAvatar"
                :size="40"
              />
              <span class="post-detail__username">{{ post.authorName }}</span>
            </router-link>
            <span class="post-detail__time">{{ formatRelativeTime(post.createTime) }}</span>
          </div>

          <div class="post-detail__meta-right">
            <router-link
              :to="`/categories/${post.categoryId}`"
              class="post-detail__category"
            >
              {{ post.categoryName }}
            </router-link>

            <!-- Status badges -->
            <el-tag v-if="post.status === 0" type="warning" size="small" effect="plain">
              <el-icon :size="12"><Lock /></el-icon>
              已锁定
            </el-tag>
          </div>
        </div>

        <!-- Stats row -->
        <div class="post-detail__stats">
          <span class="post-detail__stat">
            <el-icon :size="16"><View /></el-icon>
            <span>{{ formatNumber(post.viewCount) }}</span>
          </span>
          <span class="post-detail__stat">
            <el-icon :size="16"><Star /></el-icon>
            <span>{{ formatNumber(likeCount) }}</span>
          </span>
          <span class="post-detail__stat">
            <el-icon :size="16"><ChatDotRound /></el-icon>
            <span>{{ formatNumber(post.commentCount) }}</span>
          </span>
          <span
            v-if="post.divineCommentCount > 0"
            class="post-detail__stat post-detail__stat--divine"
          >
            <el-icon :size="16"><Medal /></el-icon>
            <span>{{ post.divineCommentCount }}</span>
          </span>
        </div>
      </header>

      <!-- ======================================================== -->
      <!-- SECTION 2 — CONTENT                                       -->
      <!-- ======================================================== -->
      <section class="post-detail__content">
        <MdViewer :content="post.content" />
      </section>

      <!-- ======================================================== -->
      <!-- SECTION 3 — ACTIONS                                        -->
      <!-- ======================================================== -->
      <section class="post-detail__actions">
        <div class="post-detail__actions-left">
          <!-- Like -->
          <el-button
            :type="liked ? 'danger' : 'default'"
            :plain="!liked"
            :icon="Star"
            size="default"
            @click="handleLike"
          >
            {{ liked ? '已赞' : '点赞' }}
            <span v-if="likeCount > 0" class="post-detail__action-count">
              {{ formatNumber(likeCount) }}
            </span>
          </el-button>

          <!-- Favorite -->
          <el-button
            :type="favorited ? 'warning' : 'default'"
            :plain="!favorited"
            :icon="Promotion"
            size="default"
            @click="handleFavorite"
          >
            {{ favorited ? '已收藏' : '收藏' }}
          </el-button>

          <!-- Share -->
          <el-button :icon="Share" size="default" @click="handleShare">
            分享
          </el-button>
        </div>

        <div v-if="isAuthor" class="post-detail__actions-right">
          <el-button :icon="Edit" size="default" @click="handleEdit">
            编辑
          </el-button>
          <el-button
            type="danger"
            :icon="Delete"
            size="default"
            plain
            @click="handleDelete"
          >
            删除
          </el-button>
        </div>
      </section>

      <!-- ======================================================== -->
      <!-- SECTION 4 — DIVINE COMMENTS                                -->
      <!-- ======================================================== -->
      <section
        v-if="post.divineCommentCount > 0"
        class="post-detail__divine"
      >
        <div class="post-detail__divine-header">
          <el-icon :size="20" class="post-detail__divine-icon">
            <Medal />
          </el-icon>
          <h2 class="post-detail__divine-title">神评专区</h2>
          <span class="post-detail__divine-count">
            {{ post.divineCommentCount }} 条神评
          </span>
        </div>

        <div v-if="divineLoading" class="post-detail__divine-skeleton">
          <div
            v-for="i in 3"
            :key="i"
            class="skeleton-line skeleton-line--comment"
          />
        </div>

        <div v-else-if="divineComments.length === 0" class="post-detail__divine-empty">
          暂无神评
        </div>

        <ul v-else class="post-detail__divine-list">
          <li
            v-for="comment in divineComments"
            :key="comment.id"
            :id="`comment-${comment.id}`"
            class="post-detail__divine-item"
          >
            <div class="post-detail__divine-item-author">
              <router-link
                :to="`/users/${comment.userId}`"
                class="post-detail__divine-item-user"
              >
                <UserAvatar :src="comment.avatarUrl" :size="28" />
                <span class="post-detail__divine-item-username">
                  {{ comment.username }}
                </span>
              </router-link>
              <span class="post-detail__divine-item-time">
                {{ formatRelativeTime(comment.createTime) }}
              </span>
            </div>
            <div class="post-detail__divine-item-content">
              <MdViewer :content="comment.content" />
            </div>
          </li>
        </ul>
      </section>

      <!-- ======================================================== -->
      <!-- SECTION 5 — AI PANEL                                        -->
      <!-- ======================================================== -->
      <section class="post-detail__ai">
        <AiSummaryPanel
          :post-id="postId"
          :post-content="post.content"
        />
      </section>

      <!-- ======================================================== -->
      <!-- SECTION 6 — COMMENTS                                       -->
      <!-- ======================================================== -->
      <section class="post-detail__comments">
        <div class="post-detail__comments-header">
          <h2 class="post-detail__comments-title">
            <el-icon :size="18"><ChatLineSquare /></el-icon>
            评论
            <span v-if="commentTotal > 0" class="post-detail__comments-count">
              ({{ commentTotal }})
            </span>
          </h2>
        </div>

        <!-- Comment form (top) -->
        <div
          v-if="post.status !== 0"
          class="post-detail__comment-form"
        >
          <div class="post-detail__comment-form-input">
            <el-input
              v-model="commentContent"
              type="textarea"
              :rows="3"
              placeholder="写下你的评论…"
              maxlength="2000"
              show-word-limit
              :disabled="commentSubmitting"
            />
          </div>
          <div class="post-detail__comment-form-actions">
            <el-button
              type="primary"
              :loading="commentSubmitting"
              :disabled="!commentContent.trim()"
              @click="handleCommentSubmit"
            >
              发表评论
            </el-button>
          </div>
        </div>

        <!-- Locked notice -->
        <div v-else class="post-detail__comment-locked">
          <el-icon :size="16"><Lock /></el-icon>
          <span>该帖子已被锁定，无法回复</span>
        </div>

        <!-- Comment list -->
        <div v-if="commentLoading" class="post-detail__comments-skeleton">
          <div
            v-for="i in 5"
            :key="i"
            class="skeleton-line skeleton-line--comment"
          />
        </div>

        <div
          v-else-if="comments.length === 0"
          class="post-detail__comments-empty"
        >
          暂无评论，来发表第一条评论吧
        </div>

        <ul v-else class="post-detail__comments-list">
          <li
            v-for="comment in comments"
            :key="comment.id"
            :id="`comment-${comment.id}`"
            class="post-detail__comment"
          >
            <!-- Comment header -->
            <div class="post-detail__comment-header">
              <router-link
                :to="`/users/${comment.userId}`"
                class="post-detail__comment-user"
              >
                <UserAvatar :src="comment.avatarUrl" :size="32" />
                <span class="post-detail__comment-username">
                  {{ comment.username }}
                </span>
              </router-link>
              <div class="post-detail__comment-badges">
                <DivineCommentBadge :is-divine="comment.isDivine" />
                <span class="post-detail__comment-time">
                  {{ formatRelativeTime(comment.createTime) }}
                </span>
              </div>
            </div>

            <!-- Comment body -->
            <div class="post-detail__comment-content">
              <MdViewer :content="comment.content" />
            </div>

            <!-- Comment footer -->
            <div class="post-detail__comment-footer">
              <el-button
                text
                size="small"
                :icon="ChatDotRound"
                @click="startReply(comment.id)"
              >
                回复
              </el-button>
            </div>

            <!-- Inline reply form -->
            <div
              v-if="replyTarget === comment.id"
              class="post-detail__reply-form"
            >
              <el-input
                v-model="replyContent"
                type="textarea"
                :rows="2"
                placeholder="写下你的回复…"
                maxlength="2000"
                show-word-limit
                :disabled="replySubmitting"
              />
              <div class="post-detail__reply-form-actions">
                <el-button
                  size="small"
                  :disabled="replySubmitting"
                  @click="cancelReply"
                >
                  取消
                </el-button>
                <el-button
                  type="primary"
                  size="small"
                  :loading="replySubmitting"
                  :disabled="!replyContent.trim()"
                  @click="handleReplySubmit(comment.id)"
                >
                  回复
                </el-button>
              </div>
            </div>

            <!-- Nested children -->
            <ul
              v-if="comment.children && comment.children.length > 0"
              class="post-detail__comment-children"
            >
              <li
                v-for="child in comment.children"
                :key="child.id"
                :id="`comment-${child.id}`"
                class="post-detail__comment-child"
              >
                <div class="post-detail__comment-child-header">
                  <router-link
                    :to="`/users/${child.userId}`"
                    class="post-detail__comment-child-user"
                  >
                    <UserAvatar :src="child.avatarUrl" :size="24" />
                    <span class="post-detail__comment-child-username">
                      {{ child.username }}
                    </span>
                  </router-link>
                  <span class="post-detail__comment-child-time">
                    {{ formatRelativeTime(child.createTime) }}
                  </span>
                </div>
                <div class="post-detail__comment-child-content">
                  <MdViewer :content="child.content" />
                </div>
                <div class="post-detail__comment-child-footer">
                  <el-button
                    text
                    size="small"
                    @click="startReply(comment.id)"
                  >
                    回复
                  </el-button>
                </div>
              </li>
            </ul>
          </li>
        </ul>

        <!-- Pagination -->
        <div
          v-if="commentTotal > commentPageSize"
          class="post-detail__comments-pagination"
        >
          <el-pagination
            background
            layout="prev, pager, next"
            :total="commentTotal"
            :page-size="commentPageSize"
            :current-page="commentPage"
            @current-change="fetchComments"
          />
        </div>
      </section>

      <!-- ======================================================== -->
      <!-- SECTION 7 — RELATED POSTS                                  -->
      <!-- ======================================================== -->
      <section
        v-if="relatedPosts.length > 0"
        class="post-detail__related"
      >
        <h2 class="post-detail__related-title">相关推荐</h2>

        <div class="post-detail__related-scroll">
          <div
            v-for="related in relatedPosts"
            :key="related.postId"
            class="post-detail__related-card"
          >
            <router-link
              :to="`/posts/${related.postId}`"
              class="post-detail__related-link"
            >
              <h4 class="post-detail__related-card-title">
                {{ related.title }}
              </h4>
              <span v-if="related.similarityScore != null" class="post-detail__related-similarity">
                相似度 {{ (related.similarityScore * 100).toFixed(0) }}%
              </span>
            </router-link>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<style lang="scss" scoped>
// =========================================================================
// CSS Variables
// =========================================================================
$max-content-width: 800px;

// =========================================================================
// Container
// =========================================================================
.post-detail {
  max-width: $max-content-width;
  margin: 0 auto;
  padding: var(--th-spacing-lg) var(--th-spacing-md);
  min-height: 60vh;

  // ── Loading / Skeleton ──
  &__loading {
    padding: var(--th-spacing-xl) 0;
  }

  &__loading-header {
    margin-bottom: var(--th-spacing-xl);
  }

  &__loading-content {
    display: flex;
    flex-direction: column;
    gap: var(--th-spacing-sm);
  }

  // ── Error ──
  &__error {
    padding-top: var(--th-spacing-16);
  }
}

// =========================================================================
// Skeleton lines (shared)
// =========================================================================
.skeleton-line {
  height: 16px;
  border-radius: var(--th-radius-sm);
  background: var(--el-fill-color);
  margin-bottom: var(--th-spacing-sm);
  position: relative;
  overflow: hidden;

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(
      90deg,
      transparent 0%,
      var(--el-fill-color-light) 50%,
      transparent 100%
    );
    animation: shimmer 1.5s ease-in-out infinite;
  }

  &--title {
    height: 28px;
    width: 65%;
    margin-bottom: var(--th-spacing-md);
  }

  &--meta {
    width: 30%;
    height: 14px;
  }

  &--para {
    width: 100%;
  }

  &--comment {
    height: 60px;
    margin-bottom: var(--th-spacing-md);
  }
}

@keyframes shimmer {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

// =========================================================================
// Section 1 — Header
// =========================================================================
.post-detail {
  &__header {
    margin-bottom: var(--th-spacing-lg);
    padding-bottom: var(--th-spacing-md);
    border-bottom: 1px solid var(--el-border-color-light);
  }

  &__title {
    margin: 0 0 var(--th-spacing-md);
    font-size: 28px;
    font-weight: 700;
    line-height: 1.35;
    color: var(--el-text-color-primary);
  }

  &__meta {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: var(--th-spacing-sm);
    margin-bottom: var(--th-spacing-md);
  }

  &__author {
    display: flex;
    align-items: center;
    gap: var(--th-spacing-sm);
  }

  &__author-link {
    display: inline-flex;
    align-items: center;
    gap: var(--th-spacing-sm);
    text-decoration: none;
    color: var(--el-text-color-primary);
    transition: opacity var(--th-transition-fast);

    &:hover {
      opacity: 0.8;
    }
  }

  &__username {
    font-size: 15px;
    font-weight: 500;
  }

  &__time {
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }

  &__meta-right {
    display: flex;
    align-items: center;
    gap: var(--th-spacing-sm);
  }

  &__category {
    font-size: 13px;
    color: var(--el-color-primary);
    text-decoration: none;
    padding: 2px var(--th-spacing-sm);
    background: var(--th-color-ai-panel-bg);
    border-radius: var(--th-radius-sm);
    transition: background-color var(--th-transition-fast);

    &:hover {
      background: var(--th-color-ai-panel-border);
    }
  }

  // Stats row
  &__stats {
    display: flex;
    align-items: center;
    gap: var(--th-spacing-lg);
    flex-wrap: wrap;
  }

  &__stat {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    font-size: 14px;
    color: var(--el-text-color-secondary);

    &--divine {
      color: var(--th-color-divine);
    }
  }
}

// =========================================================================
// Section 2 — Content
// =========================================================================
.post-detail {
  &__content {
    margin-bottom: var(--th-spacing-lg);
    padding-bottom: var(--th-spacing-lg);
    border-bottom: 1px solid var(--el-border-color-light);
    line-height: 1.8;
    font-size: 16px;
  }
}

// =========================================================================
// Section 3 — Actions
// =========================================================================
.post-detail {
  &__actions {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: var(--th-spacing-md);
    margin-bottom: var(--th-spacing-lg);
    padding-bottom: var(--th-spacing-lg);
    border-bottom: 1px solid var(--el-border-color-light);
  }

  &__actions-left,
  &__actions-right {
    display: flex;
    align-items: center;
    gap: var(--th-spacing-sm);
  }

  &__action-count {
    margin-left: 2px;
    font-weight: 500;
  }
}

// =========================================================================
// Section 4 — Divine Comments
// =========================================================================
.post-detail {
  &__divine {
    margin-bottom: var(--th-spacing-lg);
    padding: var(--th-spacing-lg);
    border: 2px solid var(--th-color-divine);
    border-radius: var(--th-radius-md);
    background: linear-gradient(
      135deg,
      var(--th-color-divine-bg, rgba(245, 158, 11, 0.05)),
      transparent 60%
    );
  }

  &__divine-header {
    display: flex;
    align-items: center;
    gap: var(--th-spacing-sm);
    margin-bottom: var(--th-spacing-md);
  }

  &__divine-icon {
    color: var(--th-color-divine);
  }

  &__divine-title {
    margin: 0;
    font-size: 18px;
    font-weight: 700;
    color: var(--th-color-divine);
  }

  &__divine-count {
    font-size: 13px;
    color: var(--el-text-color-secondary);
    margin-left: auto;
  }

  &__divine-empty {
    text-align: center;
    color: var(--el-text-color-secondary);
    padding: var(--th-spacing-md);
  }

  &__divine-list {
    list-style: none;
    padding: 0;
    margin: 0;
    display: flex;
    flex-direction: column;
    gap: var(--th-spacing-md);
  }

  &__divine-item {
    padding: var(--th-spacing-md);
    background: var(--el-bg-color);
    border-radius: var(--th-radius-sm);
    border: 1px solid var(--el-border-color-light);
  }

  &__divine-item-author {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: var(--th-spacing-sm);
  }

  &__divine-item-user {
    display: inline-flex;
    align-items: center;
    gap: var(--th-spacing-xs);
    text-decoration: none;
    color: var(--el-text-color-primary);

    &:hover {
      opacity: 0.8;
    }
  }

  &__divine-item-username {
    font-size: 14px;
    font-weight: 500;
  }

  &__divine-item-time {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  &__divine-item-content {
    font-size: 14px;
    line-height: 1.6;
  }

  &__divine-skeleton {
    display: flex;
    flex-direction: column;
    gap: var(--th-spacing-md);
  }
}

// =========================================================================
// Section 5 — AI Panel
// =========================================================================
.post-detail {
  &__ai {
    margin-bottom: var(--th-spacing-lg);
  }
}

// =========================================================================
// Section 6 — Comments
// =========================================================================
.post-detail {
  &__comments {
    margin-bottom: var(--th-spacing-lg);
  }

  &__comments-header {
    display: flex;
    align-items: center;
    margin-bottom: var(--th-spacing-md);
  }

  &__comments-title {
    display: inline-flex;
    align-items: center;
    gap: var(--th-spacing-xs);
    margin: 0;
    font-size: 18px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }

  &__comments-count {
    font-size: 14px;
    font-weight: 400;
    color: var(--el-text-color-secondary);
  }

  // Comment form
  &__comment-form {
    margin-bottom: var(--th-spacing-lg);
  }

  &__comment-form-input {
    margin-bottom: var(--th-spacing-sm);
  }

  &__comment-form-actions {
    display: flex;
    justify-content: flex-end;
  }

  // Locked notice
  &__comment-locked {
    display: flex;
    align-items: center;
    gap: var(--th-spacing-xs);
    padding: var(--th-spacing-md);
    margin-bottom: var(--th-spacing-md);
    background: var(--el-fill-color-light);
    border-radius: var(--th-radius-sm);
    font-size: 14px;
    color: var(--el-text-color-secondary);
  }

  // Empty
  &__comments-empty {
    text-align: center;
    padding: var(--th-spacing-xl) 0;
    color: var(--el-text-color-secondary);
    font-size: 14px;
  }

  // Skeleton
  &__comments-skeleton {
    display: flex;
    flex-direction: column;
  }

  // List
  &__comments-list {
    list-style: none;
    padding: 0;
    margin: 0;
  }

  // Comment item
  &__comment {
    padding: var(--th-spacing-md) 0;
    border-bottom: 1px solid var(--el-border-color-lighter);

    &:first-child {
      border-top: 1px solid var(--el-border-color-lighter);
    }
  }

  &__comment-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: var(--th-spacing-sm);
  }

  &__comment-user {
    display: inline-flex;
    align-items: center;
    gap: var(--th-spacing-xs);
    text-decoration: none;
    color: var(--el-text-color-primary);

    &:hover {
      opacity: 0.8;
    }
  }

  &__comment-username {
    font-size: 14px;
    font-weight: 500;
  }

  &__comment-badges {
    display: flex;
    align-items: center;
    gap: var(--th-spacing-sm);
  }

  &__comment-time {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  &__comment-content {
    font-size: 14px;
    line-height: 1.6;
    margin-bottom: var(--th-spacing-sm);
  }

  &__comment-footer {
    display: flex;
    align-items: center;
  }

  // Reply form (inline)
  &__reply-form {
    margin-top: var(--th-spacing-sm);
    margin-left: var(--th-spacing-sm);
    padding: var(--th-spacing-sm);
    background: var(--el-fill-color-lighter);
    border-radius: var(--th-radius-sm);

    .post-detail__reply-form-actions {
      display: flex;
      justify-content: flex-end;
      gap: var(--th-spacing-xs);
      margin-top: var(--th-spacing-xs);
    }
  }

  // Nested children
  &__comment-children {
    list-style: none;
    padding: 0;
    margin: var(--th-spacing-sm) 0 0 var(--th-spacing-xl);
    border-left: 2px solid var(--el-border-color-lighter);
    padding-left: var(--th-spacing-md);
  }

  &__comment-child {
    padding: var(--th-spacing-sm) 0;

    &:not(:last-child) {
      border-bottom: 1px dashed var(--el-border-color-lighter);
    }
  }

  &__comment-child-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: var(--th-spacing-xs);
  }

  &__comment-child-user {
    display: inline-flex;
    align-items: center;
    gap: var(--th-spacing-xs);
    text-decoration: none;
    color: var(--el-text-color-primary);

    &:hover {
      opacity: 0.8;
    }
  }

  &__comment-child-username {
    font-size: 13px;
    font-weight: 500;
  }

  &__comment-child-time {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  &__comment-child-content {
    font-size: 13px;
    line-height: 1.5;
    margin-bottom: var(--th-spacing-xs);
  }

  &__comment-child-footer {
    display: flex;
    align-items: center;
  }

  // Pagination
  &__comments-pagination {
    display: flex;
    justify-content: center;
    margin-top: var(--th-spacing-lg);
  }
}

// =========================================================================
// Section 7 — Related Posts
// =========================================================================
.post-detail {
  &__related {
    margin-bottom: var(--th-spacing-lg);
    padding: var(--th-spacing-lg);
    background: var(--el-fill-color-lighter);
    border-radius: var(--th-radius-md);
  }

  &__related-title {
    margin: 0 0 var(--th-spacing-md);
    font-size: 16px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }

  &__related-scroll {
    display: flex;
    gap: var(--th-spacing-md);
    overflow-x: auto;
    padding-bottom: var(--th-spacing-sm);
    scrollbar-width: thin;

    &::-webkit-scrollbar {
      height: 4px;
    }

    &::-webkit-scrollbar-thumb {
      background: var(--el-border-color);
      border-radius: 2px;
    }
  }

  &__related-card {
    flex-shrink: 0;
    width: 220px;
    padding: var(--th-spacing-md);
    background: var(--el-bg-color);
    border-radius: var(--th-radius-sm);
    border: 1px solid var(--el-border-color-light);
    transition: box-shadow var(--th-transition-fast);

    &:hover {
      box-shadow: var(--el-box-shadow-light);
    }
  }

  &__related-link {
    display: flex;
    flex-direction: column;
    gap: var(--th-spacing-xs);
    text-decoration: none;
  }

  &__related-card-title {
    margin: 0;
    font-size: 14px;
    font-weight: 500;
    color: var(--el-text-color-primary);
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    line-height: 1.4;
  }

  &__related-similarity {
    font-size: 12px;
    color: var(--el-color-primary);
    font-weight: 500;
  }
}
</style>
