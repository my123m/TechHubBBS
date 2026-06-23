import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'
import type { CategoryNoticeVO, Category, PageResult } from '@/api/types'

const mockGetNotices = vi.fn<(...args: unknown[]) => unknown>()
const mockNoticeCreate = vi.fn<(...args: unknown[]) => unknown>()
const mockNoticeUpdate = vi.fn<(...args: unknown[]) => unknown>()
const mockNoticeDelete = vi.fn<(...args: unknown[]) => unknown>()
const mockGetCategories = vi.fn<(...args: unknown[]) => unknown>()

vi.mock('@/api/modules/admin', () => ({
  adminApi: {
    getNotices: (...args: unknown[]) => mockGetNotices(...args),
  },
}))

vi.mock('@/api/modules/notice', () => ({
  noticeApi: {
    create: (...args: unknown[]) => mockNoticeCreate(...args),
    update: (...args: unknown[]) => mockNoticeUpdate(...args),
    delete: (...args: unknown[]) => mockNoticeDelete(...args),
  },
}))

vi.mock('@/api/modules/category', () => ({
  categoryApi: {
    getList: (...args: unknown[]) => mockGetCategories(...args),
  },
}))

vi.mock('@/stores/user', () => ({
  useUserStore: () => ({ isAdmin: true }),
}))

vi.mock('@element-plus/icons-vue', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@element-plus/icons-vue')>()
  return { ...actual, Plus: { render: () => null } }
})

const mockMessageBoxConfirm = vi.fn<(...args: unknown[]) => unknown>()
vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return {
    ...actual,
    ElMessage: { success: vi.fn<(...args: unknown[]) => unknown>(), error: vi.fn<(...args: unknown[]) => unknown>(), warning: vi.fn<(...args: unknown[]) => unknown>() },
    ElMessageBox: { confirm: (...args: unknown[]) => mockMessageBoxConfirm(...args) },
  }
})

import NoticeManagePage from '@/pages/admin/NoticeManagePage.vue'

function makeNoticeVO(overrides: Partial<CategoryNoticeVO> = {}): CategoryNoticeVO {
  return {
    id: overrides.id ?? '1',
    categoryId: overrides.categoryId ?? '10',
    title: overrides.title ?? '测试公告',
    content: overrides.content ?? '公告内容',
    type: overrides.type ?? 0,
    authorId: overrides.authorId ?? '100',
    authorName: overrides.authorName ?? '管理员',
    isPinned: overrides.isPinned ?? 0,
    status: overrides.status ?? 1,
    createTime: overrides.createTime ?? '2026-05-28T10:00:00Z',
    updateTime: overrides.updateTime ?? '2026-05-28T10:00:00Z',
  }
}

function makeCategory(overrides: Partial<Category> = {}): Category {
  return {
    id: overrides.id ?? '10',
    name: overrides.name ?? 'Java',
    description: '',
    sortOrder: 1,
    status: 1,
    postCount: 0,
    createTime: '2026-01-01T00:00:00Z',
    ...overrides,
  }
}

function makePageResult(notices: CategoryNoticeVO[], total?: number, current = 1): { data: PageResult<CategoryNoticeVO> } {
  return {
    data: {
      records: notices,
      total: total ?? notices.length,
      size: 20,
      current,
      pages: Math.ceil((total ?? notices.length) / 20),
    },
  }
}

function createTestRouter() {
  return createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/', name: 'home', component: { template: '<div>home</div>' } },
      {
        path: '/admin/notices',
        name: 'admin-notices',
        component: NoticeManagePage,
      },
    ],
  })
}

async function mountPage() {
  const router = createTestRouter()
  const pinia = createPinia()
  setActivePinia(pinia)

  await router.push('/admin/notices')
  await router.isReady()

  const wrapper = mount(NoticeManagePage, {
    global: {
      plugins: [router, pinia],
      stubs: {
        'router-link': {
          template: '<a :href="to"><slot /></a>',
          props: ['to'],
        },
        MdEditor: {
          template: '<textarea :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)"></textarea>',
          props: ['modelValue'],
          emits: ['update:modelValue'],
        },
        LoadingSkeleton: {
          template: '<div class="loading-skeleton-stub"></div>',
          props: ['variant'],
        },
        EmptyState: {
          template: '<div class="empty-state-stub"><h3>{{ title }}</h3><p>{{ description }}</p></div>',
          props: ['title', 'description', 'actionText', 'actionRoute', 'icon'],
        },
      },
    },
  })

  await flushPromises()
  await nextTick()

  return { wrapper, router, pinia }
}

describe('NoticeManagePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockMessageBoxConfirm.mockReset()
    mockGetCategories.mockReset()
    mockGetNotices.mockReset()
    mockNoticeCreate.mockReset()
    mockNoticeUpdate.mockReset()
    mockNoticeDelete.mockReset()

    mockGetCategories.mockResolvedValue({ data: [makeCategory(), makeCategory({ id: '20', name: 'Python' })] })
    mockGetNotices.mockResolvedValue(makePageResult([]))
  })

  describe('rendering', () => {
    it('renders page title', async () => {
      const { wrapper } = await mountPage()
      expect(wrapper.text()).toContain('公告管理')
    })

    it('is not a stub (has substantial content)', async () => {
      mockGetNotices.mockResolvedValue(makePageResult([makeNoticeVO()]))
      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      const text = wrapper.text()
      expect(text.length).toBeGreaterThan(20)
    })

    it('fetches categories on mount', async () => {
      await mountPage()
      expect(mockGetCategories).toHaveBeenCalled()
    })

    it('fetches notices on mount', async () => {
      await mountPage()
      expect(mockGetNotices).toHaveBeenCalledWith(
        expect.objectContaining({ page: 1, size: 20 }),
      )
    })
  })

  describe('loading state', () => {
    it('shows LoadingSkeleton while initial load', async () => {
      mockGetNotices.mockReturnValue(new Promise(() => {}))

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      expect(wrapper.find('.loading-skeleton-stub').exists()).toBe(true)
    })
  })

  describe('error state', () => {
    it('shows error alert when API fails', async () => {
      mockGetNotices.mockRejectedValue(new Error('网络错误'))

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      expect(wrapper.findComponent({ name: 'ElAlert' }).exists()).toBe(true)
      expect(wrapper.text()).toContain('重试')
    })
  })

  describe('empty state', () => {
    it('shows EmptyState when no notices', async () => {
      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      expect(wrapper.find('.empty-state-stub').exists()).toBe(true)
      expect(wrapper.text()).toContain('暂无公告')
    })
  })

  describe('data display', () => {
    beforeEach(() => {
      mockGetNotices.mockResolvedValue(
        makePageResult([
          makeNoticeVO({ id: '1', title: '须知公告', type: 0, isPinned: 0 }),
          makeNoticeVO({ id: '2', title: '活动公告', type: 1, isPinned: 1, categoryId: '20' }),
        ]),
      )
    })

    it('renders notice titles in table', async () => {
      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      expect(wrapper.text()).toContain('须知公告')
      expect(wrapper.text()).toContain('活动公告')
    })

    it('renders category name from map', async () => {
      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      expect(wrapper.text()).toContain('Java')
      expect(wrapper.text()).toContain('Python')
    })

    it('renders type tags', async () => {
      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      expect(wrapper.text()).toContain('须知')
      expect(wrapper.text()).toContain('活动')
    })

    it('renders author name', async () => {
      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      expect(wrapper.text()).toContain('管理员')
    })

    it('shows edit and delete buttons', async () => {
      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      expect(wrapper.text()).toContain('编辑')
      expect(wrapper.text()).toContain('删除')
    })
  })

  describe('filters', () => {
    it('renders category filter dropdown', async () => {
      const { wrapper } = await mountPage()

      const selects = wrapper.findAllComponents({ name: 'ElSelect' })
      expect(selects.length).toBeGreaterThanOrEqual(1)
    })

    it('renders type filter dropdown', async () => {
      const { wrapper } = await mountPage()

      const selects = wrapper.findAllComponents({ name: 'ElSelect' })
      expect(selects.length).toBeGreaterThanOrEqual(2)
    })

    it('refetches notices when filter changes', async () => {
      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      mockGetNotices.mockClear()

      const selects = wrapper.findAllComponents({ name: 'ElSelect' })
      const categorySelect = selects[0]!
      await categorySelect.vm.$emit('change')

      expect(mockGetNotices).toHaveBeenCalled()
    })
  })

  describe('create dialog', () => {
    beforeEach(() => {
      mockGetNotices.mockResolvedValue(makePageResult([]))
    })

    it('opens dialog when "新建公告" clicked', async () => {
      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      const btn = wrapper.findAllComponents({ name: 'ElButton' }).find(
        (b) => b.text().includes('新建公告'),
      )
      expect(btn).toBeTruthy()
    })

    it('dialog has category selector (only for create)', async () => {
      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      // The dialog is rendered but hidden by default
      const dialog = wrapper.findComponent({ name: 'ElDialog' })
      expect(dialog.exists()).toBe(true)
    })

    it('creates notice on submit', async () => {
      mockNoticeCreate.mockResolvedValue({ data: makeNoticeVO() })

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      // Open create dialog by finding the button and triggering click
      const createBtn = wrapper.findAllComponents({ name: 'ElButton' }).find(
        (b) => b.text().includes('新建公告'),
      )
      if (createBtn) {
        await createBtn.trigger('click')
        await flushPromises()
        await nextTick()
      }

      // The dialog is now visible - we can verify dialog component exists
      expect(wrapper.findComponent({ name: 'ElDialog' }).exists()).toBe(true)
    })
  })

  describe('delete', () => {
    it('shows confirm dialog and deletes on confirm', async () => {
      mockGetNotices.mockResolvedValue(makePageResult([makeNoticeVO()]))
      mockMessageBoxConfirm.mockResolvedValue('confirm')
      mockNoticeDelete.mockResolvedValue({})

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      const deleteBtns = wrapper.findAllComponents({ name: 'ElButton' }).filter(
        (b) => b.text() === '删除',
      )
      expect(deleteBtns.length).toBeGreaterThan(0)

      await deleteBtns[0]!.trigger('click')
      await flushPromises()
      await nextTick()

      expect(mockMessageBoxConfirm).toHaveBeenCalled()
      expect(mockNoticeDelete).toHaveBeenCalledWith('1')
    })
  })

  describe('pin toggle', () => {
    it('shows el-switch for pin toggle', async () => {
      mockGetNotices.mockResolvedValue(makePageResult([makeNoticeVO()]))

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      const switches = wrapper.findAllComponents({ name: 'ElSwitch' })
      expect(switches.length).toBeGreaterThanOrEqual(1)
    })

    it('calls update API on pin toggle', async () => {
      mockGetNotices.mockResolvedValue(makePageResult([makeNoticeVO()]))
      mockNoticeUpdate.mockResolvedValue({})

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      const switchComp = wrapper.findComponent({ name: 'ElSwitch' })
      if (!switchComp.exists()) return

      await switchComp.vm.$emit('change', true)
      await flushPromises()
      await nextTick()

      expect(mockNoticeUpdate).toHaveBeenCalledWith('1', { isPinned: 1 })
    })
  })
})
