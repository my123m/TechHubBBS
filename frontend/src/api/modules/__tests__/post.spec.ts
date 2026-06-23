import { describe, it, expect, vi, beforeEach } from 'vitest'
import type { R, PostVO, PostListParams, PostCreateRequest, PostUpdateRequest, PageResult } from '@/api/types'

const mockApi = vi.fn<(...args: unknown[]) => unknown>()
vi.mock('@/api', () => ({
  api: mockApi,
}))

const { postApi } = await import('../post')

describe('postApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getList sends GET /posts with query', async () => {
    const params: PostListParams = { page: 1, size: 20, categoryId: '3' }
    mockApi.mockResolvedValue({} as R<PageResult<PostVO>>)
    await postApi.getList(params)
    expect(mockApi).toHaveBeenCalledWith('/posts', { query: params })
  })

  it('getList sends GET /posts without params when omitted', async () => {
    mockApi.mockResolvedValue({} as R<PageResult<PostVO>>)
    await postApi.getList()
    expect(mockApi).toHaveBeenCalledWith('/posts', { query: undefined })
  })

  it('getDetail sends GET /posts/{id}', async () => {
    mockApi.mockResolvedValue({} as R<PostVO>)
    await postApi.getDetail('p1')
    expect(mockApi).toHaveBeenCalledWith('/posts/p1')
  })

  it('create sends POST /posts with body', async () => {
    const data: PostCreateRequest = { title: 'Hello', content: 'World', categoryId: '1', visibility: 0 }
    mockApi.mockResolvedValue({} as R<PostVO>)
    await postApi.create(data)
    expect(mockApi).toHaveBeenCalledWith('/posts', { method: 'POST', body: data })
  })

  it('update sends PATCH /posts/{id} with body', async () => {
    const data: PostUpdateRequest = { title: 'Updated' }
    mockApi.mockResolvedValue({} as R<PostVO>)
    await postApi.update('p1', data)
    expect(mockApi).toHaveBeenCalledWith('/posts/p1', { method: 'PATCH', body: data })
  })

  it('remove sends DELETE /posts/{id}', async () => {
    mockApi.mockResolvedValue({} as R<null>)
    await postApi.remove('p1')
    expect(mockApi).toHaveBeenCalledWith('/posts/p1', { method: 'DELETE' })
  })

  it('like sends POST /posts/{id}/like', async () => {
    mockApi.mockResolvedValue({} as R<null>)
    await postApi.like('p1')
    expect(mockApi).toHaveBeenCalledWith('/posts/p1/likes', { method: 'POST' })
  })

  it('unlike sends DELETE /posts/{id}/like', async () => {
    mockApi.mockResolvedValue({} as R<null>)
    await postApi.unlike('p1')
    expect(mockApi).toHaveBeenCalledWith('/posts/p1/likes', { method: 'DELETE' })
  })

  it('favorite sends POST /posts/{id}/favorite', async () => {
    mockApi.mockResolvedValue({} as R<null>)
    await postApi.favorite('p1')
    expect(mockApi).toHaveBeenCalledWith('/posts/p1/favorites', { method: 'POST' })
  })

  it('unfavorite sends DELETE /posts/{id}/favorite', async () => {
    mockApi.mockResolvedValue({} as R<null>)
    await postApi.unfavorite('p1')
    expect(mockApi).toHaveBeenCalledWith('/posts/p1/favorites', { method: 'DELETE' })
  })
})
