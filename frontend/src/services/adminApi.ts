import { api } from '@/services/api'
import type {
  AdminCatalogCard,
  AdminCatalogPage,
  AdminCatalogSearch,
  AdminCatalogSummary,
  HideCardRequest,
} from '@/types/AdminCatalog'
import type {
  UpdateUserRoleRequest,
  UserResponse,
  UserRole,
} from '@/types/User'

export const ADMIN_CATALOG_PAGE_SIZE = 20

export async function listUsers() {
  const response = await api.get<UserResponse[]>('/api/admin/users')
  return response.data
}

export async function changeUserRole(id: string, role: UserRole) {
  const request: UpdateUserRoleRequest = { role }
  const response = await api.patch<UserResponse>(
    `/api/admin/users/${encodeURIComponent(id)}/role`,
    request,
  )
  return response.data
}

export async function searchAdminCatalog(search: AdminCatalogSearch) {
  const response = await api.get<AdminCatalogPage>('/api/admin/catalog/cards', {
    params: {
      game: search.game,
      q: search.q,
      set: search.set,
      rarity: search.rarity,
      type: search.type,
      visibility: search.visibility,
      page: (search.page ?? 1) - 1,
      size: ADMIN_CATALOG_PAGE_SIZE,
    },
  })
  return response.data
}

export async function fetchAdminCatalogSummary() {
  const response = await api.get<AdminCatalogSummary>(
    '/api/admin/catalog/summary',
  )
  return response.data
}

export async function hideCatalogCard(id: string, reason: string) {
  const request: HideCardRequest = { reason }
  const response = await api.put<AdminCatalogCard>(
    `/api/admin/catalog/cards/${encodeURIComponent(id)}/hidden`,
    request,
  )
  return response.data
}

export async function unhideCatalogCard(id: string) {
  const response = await api.delete<AdminCatalogCard>(
    `/api/admin/catalog/cards/${encodeURIComponent(id)}/hidden`,
  )
  return response.data
}
