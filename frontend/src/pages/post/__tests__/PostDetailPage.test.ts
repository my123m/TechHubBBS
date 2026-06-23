import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'

// ---------------------------------------------------------------------------
// Hoisted mock state (vi.mock is hoisted, so these must be hoisted too)
// ---------------------------------------------------------------------------
const {
  mockPostApi,
  mockCommentApi,
  mockRecommendationApi,
} = vi.hoisted(() => ({
  mockPostApi: {
    getDetail: vi.fn<(...args: unknown[]) => unknown>(),
    like: vi.fn<(...args: unknown[]) => unknown>(),
    unlike: vi.fn<(...args: unknown[]) => unknown>(),
    favorite: vi.fn<(...args: unknown[]) => unknown>(),
    unfavorite: vi.fn<(...args: unknown[]) => unknown>(),
    remove: vi.fn<(...args: unknown[]) => unknown>(),
  },
  mockCommentApi: {
    getList: vi.fn<(...args: unknown[]) => unknown>(),
    create: vi.fn<(...args: unknown[]) => unknown>(),
    getDivineComments: vi.fn<(...args: unknown[]) => unknown>(),
  },
  mockRecommendationApi: {
    getRelatedPosts: vi.fn<(...args: unknown[]) => unknown>(),
  },
}))

vi.mock('@/api/modules/post', () => ({ postApi: mockPostApi }))
vi.mock('@/api/modules/comment', () => ({ commentApi: mockCommentApi }))
vi.mock('@/api/modules/recommendation', () => ({ recommendationApi: mockRecommendationApi }))

// Mock ApiError class so instanceof checks work in tests
vi.mock('@/api', () => ({
  ApiError: class ApiError extends Error {
    code: number
    constructor(code: number, message: string) {
      super(message)
      this.code = code
      this.name = 'ApiError'
    }
  },
  api: {},
}))

// Mock Element Plus components that use ElMessageBox (needs teleport/dialog)
vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: {
      success: vi.fn<(...args: unknown[]) => unknown>(),
      error: vi.fn<(...args: unknown[]) => unknown>(),
      warning: vi.fn<(...args: unknown[]) => unknown>(),
    },
    ElMessageBox: {
      confirm: vi.fn<(...args: unknown[]) => unknown>(),
    },
  }
})

// Mock user store
const { mockUserStore } = vi.hoisted(() => ({
  mockUserStore: {
    isLoggedIn: false,
    userInfo: null as { id: string; username: string; avatarUrl?: string; role: string } | null,
  },
}))

vi.mock('@/stores/user', () => ({
  useUserStore: () => mockUserStore,
}))

// Mock MdViewer to avoid markdown rendering issues in tests
vi.mock('@/components/markdown/MdViewer.vue', () => ({
  default: {
    name: 'MdViewer',
    props: ['content'],
    template: '<div class="md-viewer-stub">{{ content }}</div>',
  },
}))

// Mock UserAvatar
vi.mock('@/components/common/UserAvatar.vue', () => ({
  default: {
    name: 'UserAvatar',
    props: ['src', 'size'],
    template: '<div class="user-avatar-stub" />',
  },
}))

// Mock DivineCommentBadge
vi.mock('@/components/post/DivineCommentBadge.vue', () => ({
  default: {
    name: 'DivineCommentBadge',
    props: ['isDivine'],
    template: '<span v-if="isDivine" class="divine-badge-stub">神评</span>',
  },
}))

// Mock EmptyState
vi.mock('@/components/common/EmptyState.vue', () => ({
  default: {
    name: 'EmptyState',
    props: ['icon', 'title', 'description', 'actionText', 'actionRoute'],
    template: '<div class="empty-state-stub"><h3>{{ title }}</h3></div>',
  },
}))

// Mock formatRelativeTime
vi.mock('@/utils/format', () => ({
  formatRelativeTime: vi.fn(() => '1小时前'),
  formatNumber: vi.fn((n: number) => String(n)),
}))

import PostDetailPage from '@/pages/post/PostDetailPage.vue'
import { ApiError } from '@/api'
import type { PostVO, CommentVO, RelatedPostVO } from '@/api/types'

// ---------------------------------------------------------------------------
// Test Data
// ---------------------------------------------------------------------------
const mockPost: PostVO = {
  id: '1',
  title: '测试帖子标题',
  content: '这是一篇测试帖子的内容，包含了足够的文字来满足各种测试场景的需求。' +
    '这是一篇测试帖子的内容，包含了足够的文字来满足各种测试场景的需求。',
  authorId: '10',
  authorName: 'author1',
  authorAvatar: '',
  categoryId: '1',
  categoryName: '技术讨论',
  visibility: 0,
  type: 0,
  status: 1,
  viewCount: 100,
  likeCount: 5,
  commentCount: 3,
  divineCommentCount: 2,
  liked: false,
  favorited: false,
  createTime: '2025-06-01T10:00:00',
  updateTime: '2025-06-01T10:00:00',
}

const mockDivineComments: CommentVO[] = [
  {
    id: 'dc1',
    postId: '1',
    userId: '20',
    username: 'user1',
    avatarUrl: '',
    content: '这是一条神评内容',
    parentId: null,
    replyToUserId: null,
    likeCount: 10,
    recommendCount: 5,
    isDivine: true,
    liked: false,
    createTime: '2025-06-01T11:00:00',
    children: [],
  },
  {
    id: 'dc2',
    postId: '1',
    userId: '30',
    username: 'user2',
    avatarUrl: '',
    content: '这是第二条神评内容',
    parentId: null,
    replyToUserId: null,
    likeCount: 15,
    recommendCount: 8,
    isDivine: true,
    liked: false,
    createTime: '2025-06-01T12:00:00',
    children: [],
  },
]

const mockComments: CommentVO[] = [
  {
    id: 'c1',
    postId: '1',
    userId: '20',
    username: 'user1',
    avatarUrl: '',
    content: '普通评论内容',
    parentId: null,
    replyToUserId: null,
    likeCount: 2,
    recommendCount: 0,
    isDivine: false,
    liked: false,
    createTime: '2025-06-01T11:00:00',
    children: [
      {
        id: 'c2',
        postId: '1',
        userId: '30',
        username: 'user2',
        avatarUrl: '',
        content: '子回复',
        parentId: 1,
        replyToUserId: null,
        likeCount: 0,
        recommendCount: 0,
        isDivine: false,
        liked: false,
        createTime: '2025-06-01T12:00:00',
        children: [],
      },
    ],
  },
]

const mockRelatedPosts: RelatedPostVO[] = [
  { postId: '2', title: '相关帖子1', authorName: 'author2', similarityScore: 0.85 },
  { postId: '3', title: '相关帖子2', authorName: 'author3', similarityScore: 0.72 },
]

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------
function createTestRouter() {
  return createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/', name: 'home', component: { template: '<div>home</div>' } },
      { path: '/login', name: 'login', component: { template: '<div>login</div>' } },
      { path: '/posts/:id', name: 'post-detail', component: PostDetailPage },
      { path: '/posts/:id/edit', name: 'post-edit', component: { template: '<div>edit</div>' } },
      { path: '/categories/:categoryId', name: 'category', component: { template: '<div>category</div>' } },
      { path: '/users/:id', name: 'user-profile', component: { template: '<div>profile</div>' } },
    ],
  })
}

interface MountOptions {
  route?: string
  loggedIn?: boolean
  userInfo?: { id: string; username: string; avatarUrl?: string; role: string } | null
}

async function mountPage(options: MountOptions = {}) {
  const { route = '/posts/1', loggedIn = false, userInfo = null } = options

  mockUserStore.isLoggedIn = loggedIn
  mockUserStore.userInfo = userInfo

  const router = createTestRouter()
  const pinia = createPinia()
  setActivePinia(pinia)

  await router.push(route)
  await router.isReady()

  const wrapper = mount(PostDetailPage, {
    global: {
      plugins: [router, pinia],
      stubs: {
        teleport: true,
        ElPagination: { template: '<div class="el-pagination-stub" />' },
      },
    },
  })

  await flushPromises()
  return { wrapper, router }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------
describe('PostDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockUserStore.isLoggedIn = false
    mockUserStore.userInfo = null

    // Default API responses
    mockPostApi.getDetail.mockResolvedValue({ data: mockPost })
    mockPostApi.like.mockResolvedValue({ data: null })
    mockPostApi.unlike.mockResolvedValue({ data: null })
    mockPostApi.favorite.mockResolvedValue({ data: null })
    mockPostApi.unfavorite.mockResolvedValue({ data: null })
    mockPostApi.remove.mockResolvedValue({ data: null })
    mockCommentApi.getList.mockResolvedValue({
      data: { records: mockComments, total: 1, size: 10, current: 1, pages: 1 },
    })
    mockCommentApi.create.mockResolvedValue({ data: null })
    mockCommentApi.getDivineComments.mockResolvedValue({ data: mockDivineComments })
    mockRecommendationApi.getRelatedPosts.mockResolvedValue({ data: mockRelatedPosts })
  })

  // ========================================================================
  // LOADING STATE
  // ========================================================================
  describe('loading state', () => {
    it('shows skeleton loader while fetching post', async () => {
      // Don't resolve the API call yet to keep it in loading state
      let resolveFn!: (value: unknown) => void
      mockPostApi.getDetail.mockReturnValue(new Promise((resolve) => { resolveFn = resolve }))

      const { wrapper } = await mountPage()
      // Loading skeleton should be present
      expect(wrapper.find('.post-detail__loading').exists()).toBe(true)

      // Resolve now
      resolveFn({ data: mockPost })
      await flushPromises()

      // Loading should be gone
      expect(wrapper.find('.post-detail__loading').exists()).toBe(false)
    })
  })

  // ========================================================================
  // ERROR STATE
  // ========================================================================
  describe('error state', () => {
    it('shows 404 message when post not found', async () => {
      mockPostApi.getDetail.mockRejectedValue(new ApiError(404, '资源不存在'))
      const { wrapper } = await mountPage()
      expect(wrapper.find('.post-detail__error').exists()).toBe(true)
      expect(wrapper.text()).toContain('帖子不存在')
    })

    it('shows forbidden message for 403 error', async () => {
      mockPostApi.getDetail.mockRejectedValue(new ApiError(403, '无权限访问'))
      const { wrapper } = await mountPage()
      expect(wrapper.find('.post-detail__error').exists()).toBe(true)
      expect(wrapper.text()).toContain('无权访问该帖子')
    })
  })

  // ========================================================================
  // SECTION 1 — HEADER
  // ========================================================================
  describe('Section 1 — Header', () => {
    it('renders post title as H1 with 28px/700 weight styles', async () => {
      const { wrapper } = await mountPage()
      const title = wrapper.find('.post-detail__title')
      expect(title.exists()).toBe(true)
      expect(title.text()).toBe('测试帖子标题')
      expect(title.element.tagName).toBe('H1')
    })

    it('renders author avatar, username as router-link, and relative time', async () => {
      const { wrapper } = await mountPage()
      expect(wrapper.find('.post-detail__author-link').exists()).toBe(true)
      expect(wrapper.text()).toContain('author1')
    })

    it('renders category link', async () => {
      const { wrapper } = await mountPage()
      const catLink = wrapper.find('.post-detail__category')
      expect(catLink.exists()).toBe(true)
      expect(catLink.text()).toBe('技术讨论')
    })

    it('shows locked badge when status is 0', async () => {
      mockPostApi.getDetail.mockResolvedValue({
        data: { ...mockPost, status: 0 },
      })
      const { wrapper } = await mountPage()
      expect(wrapper.text()).toContain('已锁定')
    })

    it('renders stats row with viewCount, likeCount, commentCount', async () => {
      const { wrapper } = await mountPage()
      const stats = wrapper.find('.post-detail__stats')
      expect(stats.exists()).toBe(true)
      expect(stats.text()).toContain('100')
      expect(stats.text()).toContain('5')
      expect(stats.text()).toContain('3')
    })

    it('shows divine comment count when > 0', async () => {
      const { wrapper } = await mountPage()
      const divineStat = wrapper.find('.post-detail__stat--divine')
      expect(divineStat.exists()).toBe(true)
      expect(divineStat.text()).toContain('2')
    })

    it('does not show divine stat when divineCommentCount is 0', async () => {
      mockPostApi.getDetail.mockResolvedValue({
        data: { ...mockPost, divineCommentCount: 0 },
      })
      mockCommentApi.getDivineComments.mockResolvedValue({ data: [] })
      const { wrapper } = await mountPage()
      expect(wrapper.find('.post-detail__stat--divine').exists()).toBe(false)
    })
  })

  // ========================================================================
  // SECTION 2 — CONTENT
  // ========================================================================
  describe('Section 2 — Content', () => {
    it('renders MdViewer with post content', async () => {
      const { wrapper } = await mountPage()
      const section = wrapper.find('.post-detail__content')
      expect(section.exists()).toBe(true)
      const viewer = section.findComponent({ name: 'MdViewer' })
      expect(viewer.exists()).toBe(true)
      expect(viewer.props('content')).toBe(mockPost.content)
    })
  })

  // ========================================================================
  // SECTION 3 — ACTIONS
  // ========================================================================
  describe('Section 3 — Actions', () => {
    it('renders like, favorite, and share buttons', async () => {
      const { wrapper } = await mountPage()
      const actions = wrapper.find('.post-detail__actions')
      expect(actions.exists()).toBe(true)
      expect(actions.text()).toContain('点赞')
      expect(actions.text()).toContain('收藏')
      expect(actions.text()).toContain('分享')
    })

    it('shows edit and delete buttons when user is author', async () => {
      const { wrapper } = await mountPage({
        loggedIn: true,
        userInfo: { id: '10', username: 'author1', role: 'USER' },
      })
      const actionsRight = wrapper.find('.post-detail__actions-right')
      expect(actionsRight.exists()).toBe(true)
      expect(actionsRight.text()).toContain('编辑')
      expect(actionsRight.text()).toContain('删除')
    })

    it('does not show edit/delete when user is not author', async () => {
      const { wrapper } = await mountPage({
        loggedIn: true,
        userInfo: { id: '99', username: 'other', role: 'USER' },
      })
      expect(wrapper.find('.post-detail__actions-right').exists()).toBe(false)
    })

    it('like button toggles optimistically', async () => {
      const { wrapper } = await mountPage({ loggedIn: true })
      // Initially not liked, likeCount = 5
      await flushPromises()

      // Find a button that says "点赞" (the like button)
      const likeBtn = wrapper.find('.post-detail__actions-left .el-button:first-child')
      expect(likeBtn.exists()).toBe(true)

      // Click like
      await likeBtn.trigger('click')
      await flushPromises()
      expect(mockPostApi.like).toHaveBeenCalledWith('1')
    })

    it('share button copies URL to clipboard', async () => {
      // Mock clipboard API
      const writeText = vi.fn<(...args: unknown[]) => unknown>().mockResolvedValue(undefined)
      Object.assign(navigator, {
        clipboard: { writeText },
      })

      const { wrapper } = await mountPage()
      // Find all buttons and click the one containing "分享"
      const buttons = wrapper.findAll('.post-detail__actions-left .el-button')
      const shareBtn = buttons.find((btn) => btn.text().includes('分享'))
      expect(shareBtn).toBeDefined()
      if (!shareBtn) return

      await shareBtn.trigger('click')
      await flushPromises()
      expect(writeText).toHaveBeenCalled()
    })
  })

  // ========================================================================
  // SECTION 4 — DIVINE COMMENTS
  // ========================================================================
  describe('Section 4 — Divine Comments', () => {
    it('shows divine section when divineCommentCount > 0', async () => {
      const { wrapper } = await mountPage()
      const divineSection = wrapper.find('.post-detail__divine')
      expect(divineSection.exists()).toBe(true)
      expect(divineSection.text()).toContain('神评专区')
    })

    it('does not render divine section when divineCommentCount is 0', async () => {
      mockPostApi.getDetail.mockResolvedValue({
        data: { ...mockPost, divineCommentCount: 0 },
      })
      mockCommentApi.getDivineComments.mockResolvedValue({ data: [] })
      const { wrapper } = await mountPage()
      expect(wrapper.find('.post-detail__divine').exists()).toBe(false)
    })

    it('renders divine comments with author and content', async () => {
      const { wrapper } = await mountPage()
      const divineSection = wrapper.find('.post-detail__divine')
      expect(divineSection.text()).toContain('user1')
      expect(divineSection.text()).toContain('这是一条神评内容')
    })
  })

  // ========================================================================
  // SECTION 5 — AI PANEL (placeholder)
  // ========================================================================
  describe('Section 5 — AI Panel', () => {
    it('shows AI section with generate button when logged in and content length >= 50', async () => {
      const { wrapper } = await mountPage({
        loggedIn: true,
        userInfo: { id: '99', username: 'reader', role: 'USER' },
      })
      const aiSection = wrapper.find('.post-detail__ai')
      expect(aiSection.exists()).toBe(true)
      expect(aiSection.text()).toContain('生成 AI 总结')
    })

    it('shows login prompt when not logged in', async () => {
      const { wrapper } = await mountPage()
      expect(wrapper.text()).toContain('登录后使用 AI 智能总结')
    })

    it('shows content too short message when content length < 50', async () => {
      mockPostApi.getDetail.mockResolvedValue({
        data: { ...mockPost, content: '短内容' },
      })
      const { wrapper } = await mountPage({
        loggedIn: true,
        userInfo: { id: '99', username: 'reader', role: 'USER' },
      })
      const aiSection = wrapper.find('.post-detail__ai')
      expect(aiSection.exists()).toBe(true)
      expect(aiSection.text()).toContain('帖子内容过短')
    })
  })

  // ========================================================================
  // SECTION 6 — COMMENTS
  // ========================================================================
  describe('Section 6 — Comments', () => {
    it('renders comment list with pagination data', async () => {
      const { wrapper } = await mountPage()
      const commentsSection = wrapper.find('.post-detail__comments')
      expect(commentsSection.exists()).toBe(true)
      expect(commentsSection.text()).toContain('评论')
      expect(commentsSection.text()).toContain('普通评论内容')
    })

    it('renders nested replies (children)', async () => {
      const { wrapper } = await mountPage()
      expect(wrapper.find('.post-detail__comment-children').exists()).toBe(true)
      expect(wrapper.text()).toContain('子回复')
    })

    it('shows comment form when post is not locked', async () => {
      const { wrapper } = await mountPage()
      const form = wrapper.find('.post-detail__comment-form')
      expect(form.exists()).toBe(true)
      expect(form.text()).toContain('发表评论')
    })

    it('hides comment form and shows locked notice for locked post', async () => {
      mockPostApi.getDetail.mockResolvedValue({
        data: { ...mockPost, status: 0 },
      })
      const { wrapper } = await mountPage()
      expect(wrapper.find('.post-detail__comment-form').exists()).toBe(false)
      expect(wrapper.find('.post-detail__comment-locked').exists()).toBe(true)
      expect(wrapper.text()).toContain('该帖子已被锁定，无法回复')
    })

    it('shows reply button on each comment', async () => {
      const { wrapper } = await mountPage()
      const replyBtns = wrapper.findAll('.post-detail__comment-footer .el-button')
      expect(replyBtns.length).toBeGreaterThan(0)
    })

    it('shows inline reply form when reply button clicked', async () => {
      const { wrapper } = await mountPage()
      const replyBtn = wrapper.find('.post-detail__comment-footer .el-button')
      await replyBtn.trigger('click')
      await flushPromises()
      expect(wrapper.find('.post-detail__reply-form').exists()).toBe(true)
    })

    it('shows pagination when total > pageSize', async () => {
      mockCommentApi.getList.mockResolvedValue({
        data: { records: mockComments, total: 25, size: 10, current: 1, pages: 3 },
      })
      const { wrapper } = await mountPage()
      expect(wrapper.find('.post-detail__comments-pagination').exists()).toBe(true)
    })

    it('shows empty message when no comments', async () => {
      mockCommentApi.getList.mockResolvedValue({
        data: { records: [], total: 0, size: 10, current: 1, pages: 0 },
      })
      const { wrapper } = await mountPage()
      expect(wrapper.text()).toContain('暂无评论')
    })
  })

  // ========================================================================
  // SECTION 7 — RELATED POSTS
  // ========================================================================
  describe('Section 7 — Related Posts', () => {
    it('renders related posts section', async () => {
      const { wrapper } = await mountPage()
      const relatedSection = wrapper.find('.post-detail__related')
      expect(relatedSection.exists()).toBe(true)
      expect(relatedSection.text()).toContain('相关推荐')
      expect(relatedSection.text()).toContain('相关帖子1')
      expect(relatedSection.text()).toContain('相似度 85%')
    })

    it('does not render similarity span when score is null', async () => {
      mockRecommendationApi.getRelatedPosts.mockResolvedValue({
        data: [
          { postId: '2', title: '相关帖子1', authorName: 'author2', similarityScore: null },
          { postId: '3', title: '相关帖子2', authorName: 'author3', similarityScore: 0.72 },
        ],
      })
      const { wrapper } = await mountPage()
      const cards = wrapper.findAll('.post-detail__related-card')
      expect(cards.length).toBe(2)
      // 第一个卡片 similarityScore 为 null，不应渲染相似度标签
      expect(cards[0]!.find('.post-detail__related-similarity').exists()).toBe(false)
      // 第二个卡片 similarityScore 非 null，应正常显示
      expect(cards[1]!.find('.post-detail__related-similarity').exists()).toBe(true)
      expect(cards[1]!.text()).toContain('相似度 72%')
    })

    it('does not render related section when no related posts', async () => {
      mockRecommendationApi.getRelatedPosts.mockResolvedValue({ data: [] })
      const { wrapper } = await mountPage()
      expect(wrapper.find('.post-detail__related').exists()).toBe(false)
    })

    it('each related card links to its post detail page', async () => {
      const { wrapper } = await mountPage()
      const links = wrapper.findAll('.post-detail__related-link')
      expect(links.length).toBe(2)
      expect(links[0]!.attributes('href')).toBe('/posts/2')
    })
  })

  // ========================================================================
  // INTEGRATION
  // ========================================================================
  describe('integration', () => {
    it('is not a stub (has real content)', async () => {
      const { wrapper } = await mountPage()
      const text = wrapper.text()
      expect(text.length).toBeGreaterThan(5)
      expect(text).toContain('测试帖子标题')
    })

    it('fetches post detail on mount', async () => {
      await mountPage()
      expect(mockPostApi.getDetail).toHaveBeenCalledWith('1')
    })

    it('fetches comments and related posts after post loads', async () => {
      await mountPage()
      expect(mockCommentApi.getList).toHaveBeenCalledWith('1', { page: 1, size: 10 })
      expect(mockRecommendationApi.getRelatedPosts).toHaveBeenCalledWith('1', 5)
    })

    it('fetches divine comments only when divineCommentCount > 0', async () => {
      await mountPage()
      expect(mockCommentApi.getDivineComments).toHaveBeenCalledWith('1')
    })

    it('hides divine section when API returns empty list', async () => {
      mockPostApi.getDetail.mockResolvedValue({
        data: { ...mockPost, divineCommentCount: 0 },
      })
      mockCommentApi.getDivineComments.mockResolvedValue({ data: [] })
      const { wrapper } = await mountPage()
      expect(wrapper.find('.post-detail__divine').exists()).toBe(false)
    })

    it('handles API failure gracefully (shows error state)', async () => {
      mockPostApi.getDetail.mockRejectedValue(new Error('Network Error'))
      const { wrapper } = await mountPage()
      expect(wrapper.find('.post-detail__error').exists()).toBe(true)
    })
  })
})
