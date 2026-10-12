export interface CatalogCard {
  id: string
  name: string
  game: string
  edition: string
  cardNumber: string
  officialRarity: string
  imageUrl: string
}

export type CatalogSort = 'name-asc' | 'name-desc'

/** Estado da busca na URL do catálogo. {@link page} começa em 1. */
export interface CatalogSearch {
  q?: string
  set?: string
  rarity?: string
  type?: string
  sort?: CatalogSort
  page?: number
}

export interface CatalogPage {
  content: CatalogCard[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface CatalogPageResponse {
  content: CatalogCardResponse[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface CatalogFilters {
  sets: string[]
  rarities: string[]
  types: string[]
}

export interface CatalogCardResponse {
  externalId: string
  conceptualId: string
  name: string
  cardGame: 'POKEMON' | 'YUGIOH' | 'MAGIC_THE_GATHERING'
  setName: string | null
  setCode: string | null
  cardNumber: string | null
  rarity: string | null
  imageUrl: string | null
}
