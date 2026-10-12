import { api } from '@/services/api'
import type {
  ProfileResponse,
  PublicProfileResponse,
  UpdateProfileRequest,
} from '@/types/User'

export async function getOwnProfile() {
  const response = await api.get<ProfileResponse>('/api/users/me/profile')
  return response.data
}

export async function updateOwnProfile(request: UpdateProfileRequest) {
  const response = await api.put<ProfileResponse>(
    '/api/users/me/profile',
    request,
  )
  return response.data
}

export async function getPublicProfile(username: string) {
  const response = await api.get<PublicProfileResponse>(
    `/api/users/${encodeURIComponent(username)}/profile`,
  )
  return response.data
}
