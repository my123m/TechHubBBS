/**
 * Admin API module
 *
 * All endpoints require ADMIN role.
 */
import { api } from '@/api'
import type {
  R,
  AdminStatisticsData,
  UserProfileVO,
  PostVO,
  PageResult,
  CategoryNoticeVO,
  AdminCommentItem,
} from '@/api/types'

export const adminApi = {
  // ---------------------------------------------------------------------------
  // Dashboard
  // ---------------------------------------------------------------------------

  /** Get platform statistics */
  getStatistics: () => api<R<AdminStatisticsData>>('/admin/statistics'),

  // ---------------------------------------------------------------------------
  // Users
  // ---------------------------------------------------------------------------

  /** Get paginated user list */
  getUsers: (params: { page?: number; size?: number; keyword?: string }) =>
    api<R<PageResult<UserProfileVO>>>('/admin/users', { query: params }),

  /** Ban or unban a user */
  banUser: (id: string, ban: boolean) =>
    api<R<null>>(`/admin/users/${id}/ban`, { method: 'PATCH', query: { ban } }),

  /** Set a user's role */
  setRole: (id: string, role: string) =>
    api<R<null>>(`/admin/users/${id}/role`, { method: 'PATCH', query: { role } }),

  // ---------------------------------------------------------------------------
  // Posts
  // ---------------------------------------------------------------------------

  /** Get paginated post list (admin sees all, regardless of visibility) */
  getPosts: (params: { page?: number; size?: number; keyword?: string; categoryId?: string }) =>
    api<R<PageResult<PostVO>>>('/admin/posts', { query: params }),

  /** Set post type (e.g. normal, pinned, essence) */
  setPostType: (id: string, type: number) =>
    api<R<null>>(`/admin/posts/${id}/type`, { method: 'PATCH', query: { type } }),

  /** Lock a post (prevent new comments) */
  lockPost: (id: string) =>
    api<R<null>>(`/admin/posts/${id}/lock`, { method: 'PATCH' }),

  /** Delete (soft-delete) a post */
  deletePost: (id: string) =>
    api<R<null>>(`/admin/posts/${id}`, { method: 'DELETE' }),

  // ---------------------------------------------------------------------------
  // Notices
  // ---------------------------------------------------------------------------

  /** Get paginated notice list (admin view) */
  getNotices: (params: { page?: number; size?: number; categoryId?: string; type?: string }) =>
    api<R<PageResult<CategoryNoticeVO>>>('/admin/notices', { query: params }),

  // ---------------------------------------------------------------------------
  // Divine comments
  // ---------------------------------------------------------------------------

  /** Get paginated comment list for admin management */
  getComments: (params: { page?: number; size?: number; keyword?: string }) =>
    api<R<PageResult<AdminCommentItem>>>('/admin/comments', { query: params }),

  /** Set or unset a comment as divine */
  setDivine: (id: string, divine: boolean) =>
    api<R<null>>(`/admin/comments/${id}/divine`, { method: 'PATCH', query: { divine } }),
}
