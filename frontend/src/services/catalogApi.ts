import { api } from '@/services/api'
import type {
  CatalogCard,
  CatalogCardResponse,
  CatalogFilters,
  CatalogPage,
  CatalogPageResponse,
  CatalogSearch,
} from '@/types/Catalog'

export const CATALOG_PAGE_SIZE = 20

export async function searchCatalogCards(
  game: string,
  search: CatalogSearch,
): Promise<CatalogPage> {
  const response = await api.get<CatalogPageResponse>(
    `/api/catalog/${encodeURIComponent(game)}/cards`,
    {
      params: {
        q: search.q,
        set: search.set,
        rarity: search.rarity,
        type: search.type,
        sort: search.sort,
        page: (search.page ?? 1) - 1,
        size: CATALOG_PAGE_SIZE,
      },
    },
  )
  return {
    ...response.data,
    content: response.data.content.map((card) => toCatalogCard(card, game)),
  }
}

export async function fetchCatalogFilters(
  game: string,
): Promise<CatalogFilters> {
  const response = await api.get<CatalogFilters>(
    `/api/catalog/${encodeURIComponent(game)}/filters`,
  )
  return response.data
}

function toCatalogCard(card: CatalogCardResponse, game: string): CatalogCard {
  return {
    id: `${card.cardGame}:${card.externalId}`,
    name: card.name,
    game: game,
    edition: card.setName ?? '',
    cardNumber: card.cardNumber ?? '',
    officialRarity: card.rarity || 'Não informada',
    imageUrl: card.imageUrl || '',
  }
}
