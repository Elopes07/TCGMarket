import type { CatalogSearch } from '@/types/Catalog'

export function parseCatalogSearch(
  search: Record<string, unknown>,
): CatalogSearch {
  const page = Number(search.page)
  return {
    q: text(search.q),
    set: text(search.set),
    rarity: text(search.rarity),
    type: text(search.type),
    sort: search.sort === 'name-desc' ? 'name-desc' : undefined,
    page: Number.isInteger(page) && page > 1 ? page : undefined,
  }
}

function text(value: unknown) {
  if (typeof value === 'number') return String(value)
  return typeof value === 'string' && value.trim() ? value : undefined
}
