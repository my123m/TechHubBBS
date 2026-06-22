import { describe, it, expect, vi, beforeEach } from 'vitest'
import type { R, UserDetailVO, UserProfileVO, UserUpdateRequest, FollowStatusVO, PageResult, PostVO } from '@/api/types'

const mockApi = vi.fn<(...args: unknown[]) => unknown>()
vi.mock('@/api', () => ({
  api: mockApi,
}))

const { userApi } = await import('../user')

describe('userApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getMe sends GET /users/me', async () => {
    mockApi.mockResolvedValue({} as R<UserDetailVO>)
    await userApi.getMe()
    expect(mockApi).toHaveBeenCalledWith('/users/me')
  })

  it('updateMe sends PATCH /users/me with body', async () => {
    const data: UserUpdateRequest = { bio: 'new bio' }
    mockApi.mockResolvedValue({} as R<null>)
    await userApi.updateMe(data)
    expect(mockApi).toHaveBeenCalledWith('/users/me', { method: 'PATCH', body: data })
  })

  it('getById sends GET /users/{id}', async () => {
    mockApi.mockResolvedValue({} as R<UserProfileVO>)
    await userApi.getById('42')
    expect(mockApi).toHaveBeenCalledWith('/users/42')
  })

  it('getUserPosts sends GET /users/{id}/posts with query params', async () => {
    mockApi.mockResolvedValue({} as R<PageResult<PostVO>>)
    await userApi.getUserPosts('42', { page: 1, size: 10 })
    expect(mockApi).toHaveBeenCalledWith('/users/42/posts', { query: { page: 1, size: 10 } })
  })

  it('getUserPosts sends GET without params when omitted', async () => {
    mockApi.mockResolvedValue({} as R<PageResult<PostVO>>)
    await userApi.getUserPosts('42')
    expect(mockApi).toHaveBeenCalledWith('/users/42/posts', { query: undefined })
  })

  it('follow sends POST /users/{id}/follow', async () => {
    mockApi.mockResolvedValue({} as R<FollowStatusVO>)
    await userApi.follow('7')
    expect(mockApi).toHaveBeenCalledWith('/users/7/follow', { method: 'POST' })
  })

  it('unfollow sends DELETE /users/{id}/follow', async () => {
    mockApi.mockResolvedValue({} as R<FollowStatusVO>)
    await userApi.unfollow('7')
    expect(mockApi).toHaveBeenCalledWith('/users/7/follow', { method: 'DELETE' })
  })

  it('checkFollow sends GET /users/{id}/follow', async () => {
    mockApi.mockResolvedValue({} as R<FollowStatusVO>)
    await userApi.checkFollow('7')
    expect(mockApi).toHaveBeenCalledWith('/users/7/follow')
  })
})
