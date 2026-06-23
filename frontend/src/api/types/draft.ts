/**
 * Post draft view object
 */
export interface PostDraft {
  id: string
  postId?: string
  title?: string
  content?: string
  categoryId?: string
  categoryName?: string
  visibility?: number
  lastSavedAt: string
  createTime: string
  updateTime: string
}

/**
 * Draft save request body (upsert semantics)
 */
export interface DraftSaveRequest {
  title?: string
  content?: string
  categoryId?: string
  visibility?: number
  postId?: string
}
