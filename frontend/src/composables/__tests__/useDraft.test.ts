import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { nextTick } from 'vue'
import { useDraft } from '@/composables/useDraft'

// Mock the draft API module
vi.mock('@/api/modules/draft', () => ({
  draftApi: {
    check: vi.fn<(...args: unknown[]) => unknown>(),
    save: vi.fn<(...args: unknown[]) => unknown>(),
    remove: vi.fn<(...args: unknown[]) => unknown>(),
    getDetail: vi.fn<(...args: unknown[]) => unknown>(),
  },
}))

import { draftApi } from '@/api/modules/draft'

describe('useDraft', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  describe('saveDraft', () => {
    it('calls draftApi.save when isDirty is true', async () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
      vi.mocked(draftApi.save).mockResolvedValue({ data: { id: 'draft-1', lastSavedAt: '2025-01-01', createTime: '2025-01-01', updateTime: '2025-01-01' } } as any)

      const { draftData, saveDraft, isDirty, currentDraftId, lastSavedAt } = useDraft()
      draftData.value = { title: 'Test', content: 'Hello' }
      isDirty.value = true

      await saveDraft()

      expect(draftApi.save).toHaveBeenCalledWith({
        title: 'Test',
        content: 'Hello',
        categoryId: undefined,
        visibility: undefined,
        postId: undefined,
      })
      expect(currentDraftId.value).toBe('draft-1')
      expect(isDirty.value).toBe(false)
      expect(lastSavedAt.value).toBeTruthy()
    })

    it('does NOT call save if not dirty', async () => {
      const { saveDraft } = useDraft()
      await saveDraft()
      expect(draftApi.save).not.toHaveBeenCalled()
    })

    it('does NOT call save if already saving', async () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
      ;(draftApi.save as any).mockResolvedValue({ data: { id: 'draft-1', lastSavedAt: '', createTime: '', updateTime: '' } })
      const { draftData, saveDraft, isDirty } = useDraft()
      draftData.value = { title: 'Test' }
      isDirty.value = true

      const firstSave = saveDraft()
      await saveDraft()
      await firstSave

      expect(draftApi.save).toHaveBeenCalledTimes(1)
    })
  })

  describe('auto-save debounce', () => {
    it('triggers save 2s after data change via watch', async () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
      vi.mocked(draftApi.save).mockResolvedValue({ data: { id: 'draft-1', lastSavedAt: '', createTime: '', updateTime: '' } } as any)

      const { draftData, startAutoSave } = useDraft()
      startAutoSave()

      draftData.value = { title: 'New Title', content: 'New Content' }
      await nextTick()

      vi.advanceTimersByTime(2000)
      await Promise.resolve()

      expect(draftApi.save).toHaveBeenCalledTimes(1)
    })

    it('resets debounce timer when data changes twice rapidly', async () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
      vi.mocked(draftApi.save).mockResolvedValue({ data: { id: 'draft-1', lastSavedAt: '', createTime: '', updateTime: '' } } as any)

      const { draftData, startAutoSave } = useDraft()
      startAutoSave()

      draftData.value = { title: 'First' }
      await nextTick()
      vi.advanceTimersByTime(1000)

      draftData.value = { title: 'Second' }
      await nextTick()
      vi.advanceTimersByTime(1000)
      await Promise.resolve()
      expect(draftApi.save).not.toHaveBeenCalled()

      vi.advanceTimersByTime(1000)
      await Promise.resolve()
      expect(draftApi.save).toHaveBeenCalledTimes(1)
    })

    it('does NOT trigger save immediately on data change', async () => {
      const { draftData, startAutoSave } = useDraft()
      startAutoSave()

      draftData.value = { title: 'Changed' }
      await nextTick()

      expect(draftApi.save).not.toHaveBeenCalled()
    })
  })

  describe('auto-save interval', () => {
    it('30s interval triggers save if dirty', async () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
      vi.mocked(draftApi.save).mockResolvedValue({ data: { id: 'draft-int', lastSavedAt: '', createTime: '', updateTime: '' } } as any)

      const { draftData, startAutoSave } = useDraft()
      startAutoSave()

      draftData.value = { title: 'Interval Test' }
      await nextTick()
      vi.advanceTimersByTime(2000)
      await Promise.resolve()
      expect(draftApi.save).toHaveBeenCalledTimes(1)

      vi.mocked(draftApi.save).mockClear()

      draftData.value = { title: 'Interval Test 2' }
      await nextTick()
      vi.advanceTimersByTime(28_000)
      await Promise.resolve()
      expect(draftApi.save).toHaveBeenCalled()
    })
  })

  describe('cleanup on unmount', () => {
    it('stops timers and saves final draft via stopAutoSave', async () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
      vi.mocked(draftApi.save).mockResolvedValue({ data: { id: 'draft-final', lastSavedAt: '', createTime: '', updateTime: '' } } as any)

      const { draftData, startAutoSave, stopAutoSave } = useDraft()
      startAutoSave()

      draftData.value = { title: 'Final Draft' }
      await nextTick()
      vi.advanceTimersByTime(2000)
      await Promise.resolve()
      vi.mocked(draftApi.save).mockClear()

      draftData.value = { title: 'Modified' }
      await nextTick()

      stopAutoSave()
      await Promise.resolve()

      expect(draftApi.save).toHaveBeenCalledTimes(1)

      vi.advanceTimersByTime(30_000)
      await Promise.resolve()
      expect(draftApi.save).toHaveBeenCalledTimes(1)
    })
  })

  describe('discardDraft', () => {
    it('calls draftApi.remove with current draft ID', async () => {
      vi.mocked(draftApi.remove).mockResolvedValue({ code: 200, message: 'ok', data: null })
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
      vi.mocked(draftApi.save).mockResolvedValue({ data: { id: 'draft-discard', lastSavedAt: '', createTime: '', updateTime: '' } } as any)

      const { draftData, currentDraftId, discardDraft, startAutoSave } = useDraft()
      startAutoSave()
      draftData.value = { title: 'To Discard' }
      await nextTick()
      vi.advanceTimersByTime(2000)
      await Promise.resolve()

      expect(currentDraftId.value).toBe('draft-discard')
      await discardDraft()
      expect(draftApi.remove).toHaveBeenCalledWith('draft-discard')
      expect(currentDraftId.value).toBeNull()
    })

    it('allows explicit draft ID to discard', async () => {
      vi.mocked(draftApi.remove).mockResolvedValue({ code: 200, message: 'ok', data: null })

      const { discardDraft } = useDraft()
      await discardDraft('explicit-draft-id')
      expect(draftApi.remove).toHaveBeenCalledWith('explicit-draft-id')
    })

    it('does nothing if no draft ID', async () => {
      const { discardDraft } = useDraft()
      await discardDraft()
      expect(draftApi.remove).not.toHaveBeenCalled()
    })
  })

  describe('checkDraft', () => {
    it('calls draftApi.check with postId', async () => {
      const mockDraft = { data: { id: 'existing-draft', lastSavedAt: '', createTime: '', updateTime: '' } }
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
      vi.mocked(draftApi.check).mockResolvedValue(mockDraft as any)

      const { checkDraft, currentDraftId } = useDraft('post-123')
      const result = await checkDraft()

      expect(draftApi.check).toHaveBeenCalledWith('post-123')
      expect(result).toBe(mockDraft.data)
      expect(currentDraftId.value).toBe('existing-draft')
    })

    it('returns null when check fails', async () => {
      vi.mocked(draftApi.check).mockRejectedValue(new Error('Network error'))

      const { checkDraft, currentDraftId } = useDraft()
      const result = await checkDraft()

      expect(result).toBeNull()
      expect(currentDraftId.value).toBeNull()
    })
  })

  describe('restoreDraft', () => {
    it('populates draftData from API response', async () => {
      const mockDraft = {
        data: {
          id: 'draft-restore',
          title: 'Restored Title',
          content: 'Restored Content',
          categoryId: '1',
          visibility: 2,
          postId: 'post-456',
          lastSavedAt: '',
          createTime: '',
          updateTime: '',
        },
      }
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
      vi.mocked(draftApi.getDetail).mockResolvedValue(mockDraft as any)

      const { restoreDraft, draftData, currentDraftId } = useDraft()
      const result = await restoreDraft('draft-restore')

      expect(draftApi.getDetail).toHaveBeenCalledWith('draft-restore')
      expect(draftData.value.title).toBe('Restored Title')
      expect(draftData.value.content).toBe('Restored Content')
      expect(draftData.value.categoryId).toBe('1')
      expect(draftData.value.visibility).toBe(2)
      expect(draftData.value.postId).toBe('post-456')
      expect(currentDraftId.value).toBe('draft-restore')
      expect(result).toBe(mockDraft.data)
    })

    it('returns null when getDetail fails', async () => {
      vi.mocked(draftApi.getDetail).mockRejectedValue(new Error('Not found'))

      const { restoreDraft } = useDraft()
      const result = await restoreDraft('missing-draft')

      expect(result).toBeNull()
    })
  })

  describe('state initialization', () => {
    it('initializes with correct default state', () => {
      const { currentDraftId, isDirty, lastSavedAt, saving, draftData } = useDraft()

      expect(currentDraftId.value).toBeNull()
      expect(isDirty.value).toBe(false)
      expect(lastSavedAt.value).toBeNull()
      expect(saving.value).toBe(false)
      expect(draftData.value).toEqual({})
    })
  })
})
