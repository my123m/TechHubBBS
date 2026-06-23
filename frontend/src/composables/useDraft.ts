import { ref, watch, onUnmounted, type Ref } from 'vue'
import { draftApi } from '@/api/modules/draft'
import type { PostDraft } from '@/api/types/draft'

interface DraftData {
  title?: string
  content?: string
  categoryId?: string
  visibility?: number
  postId?: string
}

/**
 * Composable for draft auto-save with dual protection:
 * - 2-second debounce after data change
 * - 30-second interval fallback
 *
 * Auto-cleans timers on unmount and performs a final save.
 */
export function useDraft(postId?: string, disabled?: Ref<boolean>) {
  const currentDraftId = ref<string | null>(null)
  const isDirty = ref(false)
  const lastSavedAt = ref<string | null>(null)
  const saving = ref(false)
  const draftData = ref<DraftData>({})

  let debounceTimer: ReturnType<typeof setTimeout> | null = null
  let intervalTimer: ReturnType<typeof setInterval> | null = null

  /** Check for an existing draft for the current post */
  async function checkDraft(): Promise<PostDraft | null> {
    try {
      const res = await draftApi.check(postId)
      if (res.data) {
        currentDraftId.value = res.data.id
        return res.data
      }
    } catch (e) {
      console.error('Failed to check draft:', e)
    }
    return null
  }

  /** Restore draft content into reactive state */
  async function restoreDraft(id: string): Promise<PostDraft | null> {
    try {
      const res = await draftApi.getDetail(id)
      if (res.data) {
        draftData.value = {
          title: res.data.title,
          content: res.data.content,
          categoryId: res.data.categoryId,
          visibility: res.data.visibility,
          postId: res.data.postId,
        }
        currentDraftId.value = res.data.id
        return res.data
      }
    } catch (e) {
      console.error('Failed to restore draft:', e)
    }
    return null
  }

  /** Save draft if dirty (upsert semantics) */
  async function saveDraft(): Promise<void> {
    if (!isDirty.value || saving.value) return
    saving.value = true
    try {
      const res = await draftApi.save({
        title: draftData.value.title,
        content: draftData.value.content,
        categoryId: draftData.value.categoryId,
        visibility: draftData.value.visibility,
        postId: draftData.value.postId || postId,
      })
      if (res.data?.id) {
        currentDraftId.value = res.data.id
      }
      isDirty.value = false
      lastSavedAt.value = new Date().toISOString()
    } catch (e) {
      console.error('Failed to save draft:', e)
    } finally {
      saving.value = false
    }
  }

  /** Discard a draft by ID (defaults to current draft) */
  async function discardDraft(id?: string): Promise<void> {
    const targetId = id || currentDraftId.value
    if (!targetId) return
    try {
      await draftApi.remove(targetId)
      currentDraftId.value = null
    } catch (e) {
      console.error('Failed to discard draft:', e)
    }
  }

  /** Start auto-save: 2s debounce on data change + 30s interval fallback */
  function startAutoSave(): void {
    // Debounce: 2s after last change
    watch(
      draftData,
      () => {
        if (disabled?.value) return
        isDirty.value = true
        if (debounceTimer) clearTimeout(debounceTimer)
        debounceTimer = setTimeout(() => {
          if (disabled?.value) return
          saveDraft()
        }, 2000)
      },
      { deep: true },
    )

    // Interval: every 30s regardless
    intervalTimer = setInterval(() => {
      if (isDirty.value && !disabled?.value) saveDraft()
    }, 30_000)
  }

  /** Stop auto-save, clear timers, and perform a final save */
  function stopAutoSave(): void {
    if (debounceTimer) {
      clearTimeout(debounceTimer)
      debounceTimer = null
    }
    if (intervalTimer) {
      clearInterval(intervalTimer)
      intervalTimer = null
    }
    if (isDirty.value && !disabled?.value) {
      saveDraft()
    }
  }

  // Cleanup on unmount
  onUnmounted(() => {
    stopAutoSave()
  })

  return {
    currentDraftId,
    isDirty,
    lastSavedAt,
    saving,
    draftData,
    checkDraft,
    restoreDraft,
    saveDraft,
    discardDraft,
    startAutoSave,
    stopAutoSave,
  }
}
