import { describe, it, expect, vi, beforeEach } from 'vitest'
import type { R, Category, CategoryCreateRequest, CategoryUpdateRequest } from '@/api/types'

const mockApi = vi.fn<(...args: unknown[]) => unknown>()
vi.mock('@/api', () => ({
  api: mockApi,
}))

const { categoryApi } = await import('../category')

describe('categoryApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getList sends GET /categories', async () => {
    const mockResponse: R<Category[]> = { code: 200, message: 'success', data: [] }
    mockApi.mockResolvedValue(mockResponse)

    const result = await categoryApi.getList()
    expect(mockApi).toHaveBeenCalledWith('/categories')
    expect(result).toEqual(mockResponse)
  })

  describe('admin', () => {
    it('adminGetList sends GET /admin/categories', async () => {
      const mockResponse: R<Category[]> = { code: 200, message: 'success', data: [] }
      mockApi.mockResolvedValue(mockResponse)

      const result = await categoryApi.adminGetList()
      expect(mockApi).toHaveBeenCalledWith('/admin/categories')
      expect(result).toEqual(mockResponse)
    })

    it('create sends POST /admin/categories with body', async () => {
      const body: CategoryCreateRequest = { name: 'Test', description: 'desc', sortOrder: 1 }
      const mockResponse: R<Category> = { code: 200, message: 'ok', data: {} as Category }
      mockApi.mockResolvedValue(mockResponse)

      const result = await categoryApi.create(body)
      expect(mockApi).toHaveBeenCalledWith('/admin/categories', { method: 'POST', body })
      expect(result).toEqual(mockResponse)
    })

    it('update sends PATCH /admin/categories/:id with body', async () => {
      const body: CategoryUpdateRequest = { name: 'Updated' }
      const mockResponse: R<Category> = { code: 200, message: 'ok', data: {} as Category }
      mockApi.mockResolvedValue(mockResponse)

      const result = await categoryApi.update('1', body)
      expect(mockApi).toHaveBeenCalledWith('/admin/categories/1', { method: 'PATCH', body })
      expect(result).toEqual(mockResponse)
    })

    it('toggleStatus sends PATCH /admin/categories/:id/status with body', async () => {
      const mockResponse: R<null> = { code: 200, message: 'ok', data: null }
      mockApi.mockResolvedValue(mockResponse)

      const result = await categoryApi.toggleStatus('1', 0)
      expect(mockApi).toHaveBeenCalledWith('/admin/categories/1/status', { method: 'PATCH', body: { status: 0 } })
      expect(result).toEqual(mockResponse)
    })

    it('delete sends DELETE /admin/categories/:id', async () => {
      const mockResponse: R<null> = { code: 200, message: 'ok', data: null }
      mockApi.mockResolvedValue(mockResponse)

      const result = await categoryApi.delete('1')
      expect(mockApi).toHaveBeenCalledWith('/admin/categories/1', { method: 'DELETE' })
      expect(result).toEqual(mockResponse)
    })
  })
})
