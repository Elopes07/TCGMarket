import type { CatalogCardResponse } from '@/types/Catalog'

export type CardGame = CatalogCardResponse['cardGame']

export type CardSource = 'POKEMON_TCG_API' | 'YGOPRODECK' | 'SCRYFALL'

export type CatalogVisibility = 'visible' | 'hidden'

/** Estado da busca na URL do catálogo administrativo. {@link page} começa em 1. */
export interface AdminCatalogSearch {
  game?: string
  q?: string
  set?: string
  rarity?: string
  type?: string
  visibility?: CatalogVisibility
  page?: number
}

export interface AdminCatalogCard {
  id: string
  externalId: string
  source: CardSource
  cardGame: CardGame
  name: string
  setName: string | null
  setCode: string | null
  cardNumber: string | null
  rarity: string | null
  imageUrl: string | null
  hidden: boolean
  hiddenReason: string | null
  hiddenAt: string | null
}

export interface AdminCatalogPage {
  content: AdminCatalogCard[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface AdminCatalogGameSummary {
  game: CardGame
  totalCards: number
  hiddenCards: number
}

export interface AdminCatalogSummary {
  totalCards: number
  hiddenCards: number
  games: AdminCatalogGameSummary[]
}

export interface HideCardRequest {
  reason: string
}
