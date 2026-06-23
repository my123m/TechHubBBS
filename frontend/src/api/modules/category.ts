/**
 * Category API module
 */
import { api } from '@/api'
import type { R, Category, CategoryCreateRequest, CategoryUpdateRequest } from '@/api/types'

export const categoryApi = {
  /** Get enabled categories list */
  getList: () => api<R<Category[]>>('/categories'),

  // ---------------------------------------------------------------------------
  // Admin
  // ---------------------------------------------------------------------------

  /** Admin: get all categories (including disabled) */
  adminGetList: () => api<R<Category[]>>('/admin/categories'),

  /** Admin: create a category */
  create: (data: CategoryCreateRequest) =>
    api<R<Category>>('/admin/categories', { method: 'POST', body: data }),

  /** Admin: update a category */
  update: (id: string, data: CategoryUpdateRequest) =>
    api<R<Category>>(`/admin/categories/${id}`, { method: 'PATCH', body: data }),

  /** Admin: toggle category status */
  toggleStatus: (id: string, status: number) =>
    api<R<null>>(`/admin/categories/${id}/status`, { method: 'PATCH', body: { status } }),

  /** Admin: delete a category */
  delete: (id: string) =>
    api<R<null>>(`/admin/categories/${id}`, { method: 'DELETE' }),
}
