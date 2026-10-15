import { ADMIN_GAMES } from '@/components/admin/adminFormat'
import type { AdminCatalogSearch } from '@/types/AdminCatalog'

export function parseAdminCatalogSearch(
  search: Record<string, unknown>,
): AdminCatalogSearch {
  const page = Number(search.page)
  const game = ADMIN_GAMES.some((item) => item.slug === search.game)
    ? (search.game as string)
    : undefined
  return {
    game,
    q: text(search.q),
    set: game ? text(search.set) : undefined,
    rarity: game ? text(search.rarity) : undefined,
    type: game ? text(search.type) : undefined,
    visibility:
      search.visibility === 'visible' || search.visibility === 'hidden'
        ? search.visibility
        : undefined,
    page: Number.isInteger(page) && page > 1 ? page : undefined,
  }
}

function text(value: unknown) {
  if (typeof value === 'number') return String(value)
  return typeof value === 'string' && value.trim() ? value : undefined
}
