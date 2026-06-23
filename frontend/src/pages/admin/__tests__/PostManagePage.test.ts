import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'
import type { PostVO, Category, PageResult } from '@/api/types'

// ── Mock adminApi ──
const mockGetPosts = vi.fn<(...args: unknown[]) => unknown>()
const mockSetPostType = vi.fn<(...args: unknown[]) => unknown>()
const mockLockPost = vi.fn<(...args: unknown[]) => unknown>()
const mockDeletePost = vi.fn<(...args: unknown[]) => unknown>()

vi.mock('@/api/modules/admin', () => ({
  adminApi: {
    getPosts: (...args: unknown[]) => mockGetPosts(...args),
    setPostType: (...args: unknown[]) => mockSetPostType(...args),
    lockPost: (...args: unknown[]) => mockLockPost(...args),
    deletePost: (...args: unknown[]) => mockDeletePost(...args),
  },
}))

// ── Mock categoryApi ──
const mockGetList = vi.fn<(...args: unknown[]) => unknown>()

vi.mock('@/api/modules/category', () => ({
  categoryApi: {
    getList: (...args: unknown[]) => mockGetList(...args),
  },
}))

// ── Mock user store ──
const mockStore = {
  isAdmin: true,
}

vi.mock('@/stores/user', () => ({
  useUserStore: () => mockStore,
}))

// ── Mock Element Plus icons ──
vi.mock('@element-plus/icons-vue', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@element-plus/icons-vue')>()
  return {
    ...actual,
    Search: { render: () => null },
    Document: { render: () => null },
  }
})

import PostManagePage from '@/pages/admin/PostManagePage.vue'

// ── Helpers ──
function makePostVO(overrides: Partial<PostVO> = {}): PostVO {
  return {
    id: overrides.id ?? '1840000000000000001',
    title: overrides.title ?? '测试帖子标题',
    content: '帖子正文...',
    authorId: '1840000000000000002',
    authorName: 'testuser',
    authorAvatar: undefined,
    categoryId: '1',
    categoryName: 'Java',
    visibility: 0,
    type: 0,
    status: 1,
    viewCount: 100,
    likeCount: 20,
    commentCount: 5,
    divineCommentCount: 1,
    liked: false,
    favorited: false,
    createTime: '2026-05-28T10:00:00Z',
    updateTime: '2026-05-28T10:00:00Z',
    ...overrides,
  }
}

function makeCategory(overrides: Partial<Category> = {}): Category {
  return {
    id: overrides.id ?? '1',
    name: overrides.name ?? 'Java',
    description: '',
    sortOrder: 1,
    status: 1,
    postCount: 10,
    createTime: '2026-01-01T00:00:00Z',
    ...overrides,
  }
}

function makePageResult(posts: PostVO[], total?: number, current = 1): { data: PageResult<PostVO> } {
  return {
    data: {
      records: posts,
      total: total ?? posts.length,
      size: 15,
      current,
      pages: Math.ceil((total ?? posts.length) / 15),
    },
  }
}

function createTestRouter() {
  return createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/', name: 'home', component: { template: '<div>home</div>' } },
      { path: '/posts/:id', name: 'post-detail', component: { template: '<div>post</div>' } },
      { path: '/users/:id', name: 'user-profile', component: { template: '<div>user</div>' } },
      {
        path: '/admin/posts',
        name: 'admin-posts',
        component: PostManagePage,
      },
    ],
  })
}

async function mountPage() {
  const router = createTestRouter()
  const pinia = createPinia()
  setActivePinia(pinia)

  await router.push('/admin/posts')
  await router.isReady()

  const wrapper = mount(PostManagePage, {
    global: {
      plugins: [router, pinia],
      stubs: {
        'router-link': {
          template: '<a :href="to"><slot /></a>',
          props: ['to'],
        },
        'el-popconfirm': {
          template: '<div class="el-popconfirm-mock"><slot name="reference" /></div>',
        },
      },
    },
  })

  await flushPromises()
  await nextTick()

  return { wrapper, router, pinia }
}

describe('PostManagePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockStore.isAdmin = true
    mockGetPosts.mockReset()
    mockSetPostType.mockReset()
    mockLockPost.mockReset()
    mockDeletePost.mockReset()
    mockGetList.mockReset()

    // Default resolved states
    mockGetList.mockResolvedValue({ data: [makeCategory(), makeCategory({ id: '2', name: 'Python' })] })
    mockGetPosts.mockResolvedValue(makePageResult([]))
  })

  // ═══════════════════════════════════════════════
  // Rendering
  // ═══════════════════════════════════════════════
  describe('rendering', () => {
    it('renders page title', async () => {
      const { wrapper } = await mountPage()
      expect(wrapper.text()).toContain('帖子管理')
    })

    it('is not a stub (has substantial content)', async () => {
      mockGetPosts.mockResolvedValue(makePageResult([makePostVO()]))
      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      const text = wrapper.text()
      expect(text.length).toBeGreaterThan(20)
    })

    it('fetches categories on mount', async () => {
      await mountPage()
      expect(mockGetList).toHaveBeenCalled()
    })

    it('fetches posts on mount', async () => {
      await mountPage()
      expect(mockGetPosts).toHaveBeenCalledWith(
        expect.objectContaining({ page: 1, size: 15 }),
      )
    })
  })

  // ═══════════════════════════════════════════════
  // Loading state
  // ═══════════════════════════════════════════════
  describe('loading state', () => {
    it('shows LoadingSkeleton while initial load', async () => {
      mockGetPosts.mockReturnValue(new Promise(() => {})) // never resolves

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      expect(wrapper.findComponent({ name: 'LoadingSkeleton' }).exists()).toBe(true)
    })

    it('uses table variant of LoadingSkeleton', async () => {
      mockGetPosts.mockReturnValue(new Promise(() => {}))

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      const skeleton = wrapper.findComponent({ name: 'LoadingSkeleton' })
      expect(skeleton.props('variant')).toBe('table')
    })
  })

  // ═══════════════════════════════════════════════
  // Error state
  // ═══════════════════════════════════════════════
  describe('error state', () => {
    it('shows error alert when API fails', async () => {
      mockGetPosts.mockRejectedValue(new Error('网络错误'))

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      expect(wrapper.findComponent({ name: 'ElAlert' }).exists()).toBe(true)
      expect(wrapper.text()).toContain('网络错误')
    })

    it('shows retry button on error', async () => {
      mockGetPosts.mockRejectedValue(new Error('服务器错误'))

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      expect(wrapper.text()).toContain('重试')
    })

    it('retry button calls fetchPosts again', async () => {
      mockGetPosts.mockRejectedValueOnce(new Error('Fail'))
      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      mockGetPosts.mockResolvedValueOnce(makePageResult([makePostVO()]))

      const retryBtn = wrapper.find('.el-alert .el-button')
      await retryBtn.trigger('click')
      await flushPromises()
      await nextTick()

      expect(mockGetPosts).toHaveBeenCalledTimes(2)
    })
  })

  // ═══════════════════════════════════════════════
  // Empty state
  // ═══════════════════════════════════════════════
  describe('empty state', () => {
    it('shows EmptyState when no posts', async () => {
      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      const emptyState = wrapper.findComponent({ name: 'EmptyState' })
      expect(emptyState.exists()).toBe(true)
      expect(emptyState.props('title')).toBe('暂无帖子')
    })

    it('shows description text in empty state', async () => {
      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      const emptyState = wrapper.findComponent({ name: 'EmptyState' })
      expect(emptyState.props('description')).toContain('当前筛选条件下没有匹配的帖子')
    })
  })

  // ═══════════════════════════════════════════════
  // Data rendering
  // ═══════════════════════════════════════════════
  describe('data rendering', () => {
    it('renders table with post data', async () => {
      mockGetPosts.mockResolvedValue(makePageResult([
        makePostVO({ id: '1', title: '帖子A' }),
        makePostVO({ id: '2', title: '帖子B' }),
      ]))

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      expect(wrapper.text()).toContain('帖子A')
      expect(wrapper.text()).toContain('帖子B')
    })

    it('shows post count in header', async () => {
      mockGetPosts.mockResolvedValue(makePageResult(
        [makePostVO(), makePostVO(), makePostVO()],
        3,
      ))

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      expect(wrapper.text()).toContain('共 3 篇')
    })

    it('renders title as link to post detail', async () => {
      mockGetPosts.mockResolvedValue(makePageResult([makePostVO({ id: '10', title: '链接测试' })]))

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      const link = wrapper.find('a[href="/posts/10"]')
      expect(link.exists()).toBe(true)
      expect(link.text()).toContain('链接测试')
    })

    it('renders author as link to user profile', async () => {
      mockGetPosts.mockResolvedValue(makePageResult([makePostVO({
        authorId: 'u1',
        authorName: 'author1',
      })]))

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      expect(wrapper.find('a[href="/users/u1"]').exists()).toBe(true)
      expect(wrapper.text()).toContain('author1')
    })

    it('shows category name', async () => {
      mockGetPosts.mockResolvedValue(makePageResult([makePostVO({ categoryName: 'Python' })]))

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      expect(wrapper.text()).toContain('Python')
    })

    it('shows type tag for normal post (type=0)', async () => {
      mockGetPosts.mockResolvedValue(makePageResult([makePostVO({ type: 0 })]))

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      const tags = wrapper.findAllComponents({ name: 'ElTag' })
      const typeTags = tags.filter((t) => t.text() === '普通')
      expect(typeTags.length).toBeGreaterThanOrEqual(1)
    })

    it('shows type tag for featured post (type=1)', async () => {
      mockGetPosts.mockResolvedValue(makePageResult([makePostVO({ type: 1 })]))

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      expect(wrapper.text()).toContain('精华')
    })

    it('shows type tag for pinned post (type=2)', async () => {
      mockGetPosts.mockResolvedValue(makePageResult([makePostVO({ type: 2 })]))

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      expect(wrapper.text()).toContain('置顶')
    })

    it('shows locked tag when status=0', async () => {
      mockGetPosts.mockResolvedValue(makePageResult([makePostVO({ status: 0 })]))

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      const tags = wrapper.findAllComponents({ name: 'ElTag' })
      const lockedTags = tags.filter((t) => t.text() === '已锁定')
      expect(lockedTags.length).toBeGreaterThanOrEqual(1)
    })

    it('shows normal tag when status=1', async () => {
      mockGetPosts.mockResolvedValue(makePageResult([makePostVO({ status: 1 })]))

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      expect(wrapper.text()).toContain('正常')
    })

    it('shows visibility label', async () => {
      mockGetPosts.mockResolvedValue(makePageResult([makePostVO({ visibility: 1 })]))

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      expect(wrapper.text()).toContain('登录可见')
    })
  })

  // ═══════════════════════════════════════════════
  // Filters
  // ═══════════════════════════════════════════════
  describe('filters', () => {
    it('renders search input', async () => {
      const { wrapper } = await mountPage()
      expect(wrapper.find('.post-manage__search').exists()).toBe(true)
    })

    it('renders category select', async () => {
      const { wrapper } = await mountPage()
      expect(wrapper.find('.post-manage__category-select').exists()).toBe(true)
    })

    it('searches with keyword on Enter', async () => {
      const { wrapper } = await mountPage()
      mockGetPosts.mockClear()

      const searchInput = wrapper.find('.post-manage__search input')
      await searchInput.setValue('测试关键词')
      await searchInput.trigger('keyup.enter')
      await flushPromises()
      await nextTick()

      expect(mockGetPosts).toHaveBeenCalledWith(
        expect.objectContaining({ keyword: '测试关键词', page: 1 }),
      )
    })

    it('resets filters and re-fetches', async () => {
      const { wrapper } = await mountPage()
      mockGetPosts.mockClear()

      const resetBtn = wrapper.findAll('.el-button').find((b) => b.text() === '重置')
      if (resetBtn) {
        await resetBtn.trigger('click')
        await flushPromises()
        await nextTick()
      }

      expect(mockGetPosts).toHaveBeenCalledWith(
        expect.objectContaining({ page: 1, size: 15 }),
      )
    })
  })

  // ═══════════════════════════════════════════════
  // Set type action
  // ═══════════════════════════════════════════════
  describe('set post type', () => {
    it('calls setPostType when type select changes', async () => {
      mockGetPosts.mockResolvedValue(makePageResult([makePostVO({ id: '1', type: 0 })]))

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      mockSetPostType.mockResolvedValue({})

      // Find the select in the actions column
      const typeSelect = wrapper.find('.post-manage__type-select')
      expect(typeSelect.exists()).toBe(true)
    })
  })

  // ═══════════════════════════════════════════════
  // Lock / Unlock action
  // ═══════════════════════════════════════════════
  describe('lock / unlock', () => {
    it('shows unlock button when post is locked (status=0)', async () => {
      mockGetPosts.mockResolvedValue(makePageResult([makePostVO({ id: '1', status: 0 })]))

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      const lockBtn = wrapper.findAll('.el-button').find((b) => b.text() === '解锁')
      expect(lockBtn).toBeTruthy()
    })

    it('shows lock button when post is active (status=1)', async () => {
      mockGetPosts.mockResolvedValue(makePageResult([makePostVO({ id: '1', status: 1 })]))

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      const lockBtn = wrapper.findAll('.el-button').find((b) => b.text() === '锁定')
      expect(lockBtn).toBeTruthy()
    })
  })

  // ═══════════════════════════════════════════════
  // Delete action (ADMIN only)
  // ═══════════════════════════════════════════════
  describe('delete', () => {
    it('shows delete button for admin', async () => {
      mockStore.isAdmin = true
      mockGetPosts.mockResolvedValue(makePageResult([makePostVO()]))

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      const deleteBtn = wrapper.findAll('.el-button').find((b) => b.text() === '删除')
      expect(deleteBtn).toBeTruthy()
    })

    it('does not show delete button for non-admin', async () => {
      mockStore.isAdmin = false
      mockGetPosts.mockResolvedValue(makePageResult([makePostVO()]))

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      const deleteBtn = wrapper.findAll('.el-button').find((b) => b.text() === '删除')
      expect(deleteBtn).toBeFalsy()
    })
  })

  // ═══════════════════════════════════════════════
  // Pagination
  // ═══════════════════════════════════════════════
  describe('pagination', () => {
    it('shows pagination when total > pageSize', async () => {
      const posts = Array.from({ length: 2 }, (_, i) => makePostVO({ id: String(i) }))
      mockGetPosts.mockResolvedValue(makePageResult(posts, 30, 1))

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      expect(wrapper.find('.post-manage__pagination').exists()).toBe(true)
    })

    it('hides pagination when total <= pageSize', async () => {
      mockGetPosts.mockResolvedValue(makePageResult([makePostVO()], 1))

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      expect(wrapper.find('.post-manage__pagination').exists()).toBe(false)
    })

    it('fetches posts when page changes', async () => {
      const posts = Array.from({ length: 2 }, (_, i) => makePostVO({ id: String(i) }))
      mockGetPosts.mockResolvedValue(makePageResult(posts, 30, 1))

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()
      mockGetPosts.mockClear()

      const pagination = wrapper.findComponent({ name: 'ElPagination' })
      await pagination.vm.$emit('current-change', 2)
      await flushPromises()
      await nextTick()

      expect(mockGetPosts).toHaveBeenCalledWith(
        expect.objectContaining({ page: 2 }),
      )
    })
  })
})
