import type { CardGame, CardSource } from '@/types/AdminCatalog'
import type { ApiErrorResponse } from '@/types/User'
import axios from 'axios'

export const ADMIN_GAMES: { game: CardGame; slug: string; label: string }[] = [
  { game: 'POKEMON', slug: 'pokemon', label: 'Pokémon' },
  { game: 'YUGIOH', slug: 'yugioh', label: 'Yu-Gi-Oh!' },
  { game: 'MAGIC_THE_GATHERING', slug: 'magic', label: 'Magic' },
]

export const GAME_LABEL: Record<CardGame, string> = {
  POKEMON: 'Pokémon',
  YUGIOH: 'Yu-Gi-Oh!',
  MAGIC_THE_GATHERING: 'Magic',
}

export const SOURCE_LABEL: Record<CardSource, string> = {
  POKEMON_TCG_API: 'Pokémon TCG API',
  YGOPRODECK: 'YGOPRODeck',
  SCRYFALL: 'Scryfall',
}

export function apiMessage(cause: unknown) {
  if (axios.isAxiosError<ApiErrorResponse>(cause)) {
    return cause.response?.data?.message ?? null
  }
  return null
}
