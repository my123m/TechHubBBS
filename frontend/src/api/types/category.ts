/**
 * Category (id is string — backend Long serialised as string via JacksonConfig)
 */
export interface Category {
  id: string
  name: string
  description: string
  sortOrder: number
  status: number
  postCount: number
  createTime: string
}

/**
 * Category create request (admin)
 */
export interface CategoryCreateRequest {
  name: string
  description?: string
  sortOrder?: number
}

/**
 * Category update request (admin)
 */
export interface CategoryUpdateRequest {
  name?: string
  description?: string
  sortOrder?: number
  status?: number
}

/**
 * Category notice VO
 */
export interface CategoryNoticeVO {
  id: string
  categoryId: string
  title: string
  content: string
  type: number
  authorId: string
  authorName: string
  isPinned: number
  status: number
  createTime: string
  updateTime: string
}

/**
 * Category notice create request
 */
export interface CategoryNoticeCreateRequest {
  title: string
  content: string
  type?: number
  isPinned?: number
}

/**
 * Category notice update request
 */
export interface CategoryNoticeUpdateRequest {
  title?: string
  content?: string
  type?: number
  isPinned?: number
}
