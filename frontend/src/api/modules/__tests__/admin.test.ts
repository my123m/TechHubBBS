import { describe, it, expect, vi, beforeEach } from 'vitest'
import { api } from '@/api'
import { adminApi } from '@/api/modules/admin'

vi.mock('@/api', () => ({
  api: vi.fn<(...args: unknown[]) => unknown>(),
}))

describe('adminApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  // ---- Dashboard ----

  it('getStatistics calls GET /admin/statistics', () => {
    adminApi.getStatistics()
    expect(api).toHaveBeenCalledWith('/admin/statistics')
  })

  // ---- Users ----

  it('getUsers calls GET /admin/users with query', () => {
    adminApi.getUsers({ page: 1, size: 20, keyword: 'test' })
    expect(api).toHaveBeenCalledWith('/admin/users', {
      query: { page: 1, size: 20, keyword: 'test' },
    })
  })

  it('banUser calls PATCH /admin/users/{id}/ban with query', () => {
    adminApi.banUser('user-1', true)
    expect(api).toHaveBeenCalledWith('/admin/users/user-1/ban', {
      method: 'PATCH',
      query: { ban: true },
    })
  })

  it('setRole calls PATCH /admin/users/{id}/role with query', () => {
    adminApi.setRole('user-1', 'MODERATOR')
    expect(api).toHaveBeenCalledWith('/admin/users/user-1/role', {
      method: 'PATCH',
      query: { role: 'MODERATOR' },
    })
  })

  // ---- Posts ----

  it('getPosts calls GET /admin/posts with query', () => {
    adminApi.getPosts({ page: 1, size: 20, keyword: 'hello', categoryId: '2' })
    expect(api).toHaveBeenCalledWith('/admin/posts', {
      query: { page: 1, size: 20, keyword: 'hello', categoryId: '2' },
    })
  })

  it('setPostType calls PATCH /admin/posts/{id}/type with query', () => {
    adminApi.setPostType('post-1', 1)
    expect(api).toHaveBeenCalledWith('/admin/posts/post-1/type', {
      method: 'PATCH',
      query: { type: 1 },
    })
  })

  it('lockPost calls PATCH /admin/posts/{id}/lock', () => {
    adminApi.lockPost('post-1')
    expect(api).toHaveBeenCalledWith('/admin/posts/post-1/lock', {
      method: 'PATCH',
    })
  })

  it('deletePost calls DELETE /admin/posts/{id}', () => {
    adminApi.deletePost('post-1')
    expect(api).toHaveBeenCalledWith('/admin/posts/post-1', {
      method: 'DELETE',
    })
  })

  // ---- Notices ----

  it('getNotices calls GET /admin/notices with query', () => {
    adminApi.getNotices({ page: 1, size: 20, categoryId: 'cat-1', type: 'NOTICE' })
    expect(api).toHaveBeenCalledWith('/admin/notices', {
      query: { page: 1, size: 20, categoryId: 'cat-1', type: 'NOTICE' },
    })
  })

  // ---- Divine ----

  it('getComments calls GET /admin/comments with query', () => {
    adminApi.getComments({ page: 1, size: 20, keyword: 'test' })
    expect(api).toHaveBeenCalledWith('/admin/comments', {
      query: { page: 1, size: 20, keyword: 'test' },
    })
  })

  it('getComments calls GET /admin/comments without keyword when not provided', () => {
    adminApi.getComments({ page: 1, size: 20 })
    expect(api).toHaveBeenCalledWith('/admin/comments', {
      query: { page: 1, size: 20 },
    })
  })

  it('setDivine calls PATCH /admin/comments/{id}/divine with query', () => {
    adminApi.setDivine('comment-1', true)
    expect(api).toHaveBeenCalledWith('/admin/comments/comment-1/divine', {
      method: 'PATCH',
      query: { divine: true },
    })
  })
})
