/**
 * User API module — profile, posts, follow
 */
import { api } from '@/api'
import type { R, UserProfileVO, UserDetailVO, UserUpdateRequest, FollowStatusVO, PostVO, PageResult } from '@/api/types'

export const userApi = {
  /** Get current user full profile (includes email) */
  getMe: () => api<R<UserDetailVO>>('/users/me'),

  /** Update current user profile (partial) */
  updateMe: (data: UserUpdateRequest) => api<R<null>>('/users/me', { method: 'PATCH', body: data }),

  /** Get public profile by user ID */
  getById: (id: string) => api<R<UserProfileVO>>(`/users/${id}`),

  /** Get paginated posts by user ID */
  getUserPosts: (id: string, params?: { page?: number; size?: number; sort?: string }) =>
    api<R<PageResult<PostVO>>>(`/users/${id}/posts`, { query: params }),

  /** Follow a user */
  follow: (id: string) => api<R<FollowStatusVO>>(`/users/${id}/follow`, { method: 'POST' }),

  /** Unfollow a user */
  unfollow: (id: string) => api<R<FollowStatusVO>>(`/users/${id}/follow`, { method: 'DELETE' }),

  /** Check follow status */
  checkFollow: (id: string) => api<R<FollowStatusVO>>(`/users/${id}/follow`),

  /** Get paginated followers of a user */
  getFollowers: (userId: string, params?: { page?: number; size?: number }) =>
    api<R<PageResult<UserProfileVO>>>(`/users/${userId}/followers`, { query: params }),

  /** Get paginated followings of a user */
  getFollowings: (userId: string, params?: { page?: number; size?: number }) =>
    api<R<PageResult<UserProfileVO>>>(`/users/${userId}/followings`, { query: params }),
}
