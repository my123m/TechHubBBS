import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'

// ---------------------------------------------------------------------------
// Hoisted mock state (vi.mock is hoisted, so these must be hoisted too)
// ---------------------------------------------------------------------------
const {
  mockDraftApi,
} = vi.hoisted(() => ({
  mockDraftApi: {
    getList: vi.fn<(...args: unknown[]) => unknown>(),
    remove: vi.fn<(...args: unknown[]) => unknown>(),
  },
}))

vi.mock('@/api/modules/draft', () => ({ draftApi: mockDraftApi }))

// Mock Element Plus components that use teleport/dialog
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

// Mock LoadingSkeleton
vi.mock('@/components/common/LoadingSkeleton.vue', () => ({
  default: {
    name: 'LoadingSkeleton',
    props: ['variant'],
    template: '<div class="loading-skeleton-stub" :data-variant="variant">Loading...</div>',
  },
}))

// Mock EmptyState
vi.mock('@/components/common/EmptyState.vue', () => ({
  default: {
    name: 'EmptyState',
    props: ['icon', 'title', 'description', 'actionText', 'actionRoute'],
    template: '<div class="empty-state-stub"><h3>{{ title }}</h3><p>{{ description }}</p><router-link v-if="actionText && actionRoute" :to="actionRoute">{{ actionText }}</router-link></div>',
  },
}))

// Mock formatRelativeTime
vi.mock('@/utils/format', () => ({
  formatRelativeTime: vi.fn((d: string) => {
    // Simple deterministic mock: use date string to produce a label
    if (d === '2025-01-01T12:00:00') return '5个月前'
    if (d === '2025-06-15T08:30:00') return '2天前'
    return '刚刚'
  }),
  formatNumber: vi.fn((n: number) => String(n)),
}))

// Mock visibility helpers
vi.mock('@/composables/useVisibility', () => ({
  visibilityLabel: vi.fn((code: number) => {
    switch (code) {
      case 0: return '公开'
      case 1: return '登录可见'
      case 2: return '粉丝可见'
      case 3: return '私密'
      default: return '未知'
    }
  }),
  visibilityIcon: vi.fn(() => 'View'),
  canView: vi.fn<(...args: unknown[]) => unknown>(),
  useVisibility: vi.fn<(...args: unknown[]) => unknown>(),
}))

import DraftPage from '@/pages/draft/DraftPage.vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import type { PostDraft } from '@/api/types'

// ---------------------------------------------------------------------------
// Test Data
// ---------------------------------------------------------------------------
const mockDrafts: PostDraft[] = [
  {
    id: 'd1',
    postId: '100',
    title: '一篇待编辑的帖子',
    content: '# 内容',
    categoryId: '1',
    categoryName: '技术讨论',
    visibility: 0,
    lastSavedAt: '2025-01-01T12:00:00',
    createTime: '2025-01-01T12:00:00',
    updateTime: '2025-01-01T12:00:00',
  },
  {
    id: 'd2',
    postId: undefined,
    title: undefined,
    content: '还没写完...',
    categoryId: '2',
    categoryName: '前端开发',
    visibility: 1,
    lastSavedAt: '2025-06-15T08:30:00',
    createTime: '2025-06-15T08:30:00',
    updateTime: '2025-06-15T08:30:00',
  },
  {
    id: 'd3',
    postId: '200',
    title: '另一篇草稿',
    content: '一些内容',
    categoryId: undefined,
    categoryName: undefined,
    visibility: undefined,
    lastSavedAt: '2025-06-17T09:00:00',
    createTime: '2025-06-17T09:00:00',
    updateTime: '2025-06-17T09:00:00',
  },
]

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------
function createTestRouter() {
  return createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/', name: 'home', component: { template: '<div>home</div>' } },
      { path: '/drafts', name: 'drafts', component: DraftPage },
      { path: '/posts/new', name: 'post-create', component: { template: '<div>new post</div>' } },
      { path: '/posts/:id/edit', name: 'post-edit', component: { template: '<div>edit post</div>' } },
    ],
  })
}

async function mountPage() {
  const router = createTestRouter()
  const pinia = createPinia()
  setActivePinia(pinia)

  await router.push('/drafts')
  await router.isReady()

  const wrapper = mount(DraftPage, {
    global: {
      plugins: [router, pinia],
      stubs: {
        teleport: true,
      },
    },
  })

  await flushPromises()
  return { wrapper, router }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------
describe('DraftPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockDraftApi.getList.mockResolvedValue({ data: mockDrafts })
    mockDraftApi.remove.mockResolvedValue({ data: null })
  })

  // ========================================================================
  // LOADING STATE
  // ========================================================================
  describe('loading state', () => {
    it('shows skeleton loader while fetching drafts', async () => {
      let resolveFn!: (value: unknown) => void
      mockDraftApi.getList.mockReturnValue(new Promise((resolve) => { resolveFn = resolve }))

      const { wrapper } = await mountPage()
      expect(wrapper.find('.loading-skeleton-stub').exists()).toBe(true)

      resolveFn({ data: mockDrafts })
      await flushPromises()

      expect(wrapper.find('.loading-skeleton-stub').exists()).toBe(false)
    })
  })

  // ========================================================================
  // ERROR STATE
  // ========================================================================
  describe('error state', () => {
    it('shows error alert when fetch fails', async () => {
      mockDraftApi.getList.mockRejectedValue(new Error('Network Error'))
      const { wrapper } = await mountPage()
      expect(wrapper.find('.draft-page__alert').exists()).toBe(true)
      expect(wrapper.text()).toContain('加载草稿失败')
    })

    it('retry button triggers a fresh fetch', async () => {
      mockDraftApi.getList.mockRejectedValue(new Error('Fail first'))
      const { wrapper } = await mountPage()
      expect(wrapper.find('.draft-page__alert').exists()).toBe(true)

      // Reset mock and click retry
      mockDraftApi.getList.mockResolvedValue({ data: mockDrafts })
      const retryBtn = wrapper.find('.draft-page__alert .el-button')
      expect(retryBtn.exists()).toBe(true)
      await retryBtn.trigger('click')
      await flushPromises()

      expect(mockDraftApi.getList).toHaveBeenCalledTimes(2)
      expect(wrapper.find('.draft-page__alert').exists()).toBe(false)
    })
  })

  // ========================================================================
  // EMPTY STATE
  // ========================================================================
  describe('empty state', () => {
    it('shows empty state when no drafts exist', async () => {
      mockDraftApi.getList.mockResolvedValue({ data: [] })
      const { wrapper } = await mountPage()
      expect(wrapper.find('.empty-state-stub').exists()).toBe(true)
      expect(wrapper.text()).toContain('暂无草稿')
    })

    it('has a "去写帖子" link to /posts/new', async () => {
      mockDraftApi.getList.mockResolvedValue({ data: [] })
      const { wrapper } = await mountPage()
      const link = wrapper.find('.empty-state-stub')
      expect(link.exists()).toBe(true)
      expect(link.text()).toContain('去写帖子')
    })
  })

  // ========================================================================
  // LIST RENDERING
  // ========================================================================
  describe('list rendering', () => {
    it('renders draft count in header', async () => {
      const { wrapper } = await mountPage()
      expect(wrapper.text()).toContain('共 3 篇草稿')
    })

    it('renders all draft cards', async () => {
      const { wrapper } = await mountPage()
      const cards = wrapper.findAll('.draft-card')
      expect(cards.length).toBe(3)
    })

    it('renders draft title', async () => {
      const { wrapper } = await mountPage()
      expect(wrapper.text()).toContain('一篇待编辑的帖子')
    })

    it('shows "无标题" when title is empty', async () => {
      const { wrapper } = await mountPage()
      expect(wrapper.text()).toContain('无标题')
    })

    it('renders category name when available', async () => {
      const { wrapper } = await mountPage()
      expect(wrapper.text()).toContain('技术讨论')
      expect(wrapper.text()).toContain('前端开发')
    })

    it('renders visibility label', async () => {
      const { wrapper } = await mountPage()
      expect(wrapper.text()).toContain('公开')
      expect(wrapper.text()).toContain('登录可见')
    })

    it('renders relative time for lastSavedAt', async () => {
      const { wrapper } = await mountPage()
      expect(wrapper.text()).toContain('5个月前')
      expect(wrapper.text()).toContain('2天前')
    })

    it('renders "继续编辑" and "删除" buttons on each card', async () => {
      const { wrapper } = await mountPage()
      const cards = wrapper.findAll('.draft-card')
      for (const card of cards) {
        expect(card.text()).toContain('继续编辑')
        expect(card.text()).toContain('删除')
      }
    })
  })

  // ========================================================================
  // DELETE FLOW
  // ========================================================================
  describe('delete flow', () => {
    it('calls ElMessageBox.confirm then draftApi.remove on delete click', async () => {
      const { wrapper } = await mountPage()

      // Find the first draft card's delete button
      const firstCard = wrapper.findAll('.draft-card')[0]!
      const deleteBtn = firstCard.find('.draft-card__actions .el-button--danger')
      expect(deleteBtn.exists()).toBe(true)

      await deleteBtn.trigger('click')
      await flushPromises()

      expect(ElMessageBox.confirm).toHaveBeenCalledWith(
        '确定要删除该草稿吗？删除后无法恢复。',
        '删除草稿',
        expect.objectContaining({
          confirmButtonText: '删除',
          cancelButtonText: '取消',
          type: 'warning',
        }),
      )
      expect(mockDraftApi.remove).toHaveBeenCalledWith('d1')
      expect(ElMessage.success).toHaveBeenCalledWith('草稿已删除')
    })

    it('refreshes list after successful delete', async () => {
      const { wrapper } = await mountPage()
      // Initially 3 drafts
      expect(wrapper.findAll('.draft-card').length).toBe(3)

      // After delete, API returns 2 drafts
      mockDraftApi.getList.mockResolvedValue({
        data: mockDrafts.slice(1),
      })

      const firstCard = wrapper.findAll('.draft-card')[0]!
      const deleteBtn = firstCard.find('.draft-card__actions .el-button--danger')
      await deleteBtn.trigger('click')
      await flushPromises()

      expect(mockDraftApi.getList).toHaveBeenCalledTimes(2)
    })

    it('does nothing when user cancels delete confirmation', async () => {
      ;(ElMessageBox.confirm as ReturnType<typeof vi.fn>).mockRejectedValueOnce('cancel')
      const { wrapper } = await mountPage()

      const firstCard = wrapper.findAll('.draft-card')[0]!
      const deleteBtn = firstCard.find('.draft-card__actions .el-button--danger')
      await deleteBtn.trigger('click')
      await flushPromises()

      // remove should NOT be called on cancel
      expect(mockDraftApi.remove).not.toHaveBeenCalled()
    })
  })

  // ========================================================================
  // NAVIGATION ON CONTINUE
  // ========================================================================
  describe('navigation on continue', () => {
    it('navigates to /posts/:id/edit when draft has postId', async () => {
      const { wrapper, router } = await mountPage()

      // First draft (d1) has postId=100 → should go to /posts/100/edit
      const firstCard = wrapper.findAll('.draft-card')[0]!
      const continueBtn = firstCard.find('.draft-card__actions .el-button--primary')
      await continueBtn.trigger('click')
      await flushPromises()

      expect(router.currentRoute.value.path).toBe('/posts/100/edit')
    })

    it('navigates to /posts/new when draft has no postId', async () => {
      const { wrapper, router } = await mountPage()

      // Second draft (d2) has no postId → should go to /posts/new
      const secondCard = wrapper.findAll('.draft-card')[1]!
      const continueBtn = secondCard.find('.draft-card__actions .el-button--primary')
      await continueBtn.trigger('click')
      await flushPromises()

      expect(router.currentRoute.value.path).toBe('/posts/new')
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
      expect(text).toContain('草稿箱')
    })

    it('fetches drafts on mount', async () => {
      await mountPage()
      expect(mockDraftApi.getList).toHaveBeenCalledTimes(1)
    })
  })
})
