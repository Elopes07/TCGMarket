export type UserRole = 'USER' | 'ADMIN'

export type CardGame = 'POKEMON' | 'YUGIOH' | 'MAGIC_THE_GATHERING'

export interface UserResponse {
  id: string
  name: string
  username: string | null
  email: string
  role: UserRole
  registrationDate: string
}

export interface ProfileResponse {
  id: string
  name: string
  username: string
  email: string
  role: UserRole
  registrationDate: string
  bio: string | null
  city: string | null
  state: string | null
  favoriteGames: CardGame[]
  links: string[]
}

export interface PublicProfileResponse {
  name: string
  username: string
  administrator: boolean
  registrationDate: string
  bio: string | null
  city: string | null
  state: string | null
  favoriteGames: CardGame[]
  links: string[]
}

export interface UpdateProfileRequest {
  name: string
  username: string
  bio: string | null
  city: string | null
  state: string | null
  favoriteGames: CardGame[]
  links: string[]
}

export interface UpdateUserRoleRequest {
  role: UserRole
}

export interface CreateUserRequest {
  name: string
  email: string
  password: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  token: string
  user: UserResponse
}

export interface ApiFieldError {
  field: string
  message: string
}

export interface ApiErrorResponse {
  timestamp: string
  status: number
  error: string
  message: string
  fields: ApiFieldError[]
}
