import { useEffect, useState } from 'react'
import { useNavigate, useParams, useSearch } from '@tanstack/react-router'
import { CatalogCard } from '@/components/catalog/CatalogCard'
import { CARD_TYPE_LABELS as typeLabels } from '@/components/catalog/cardDetailsFormat'
import type {
  CatalogFilters,
  CatalogPage as CatalogPageData,
  CatalogSearch,
} from '@/types/Catalog'
import { fetchCatalogFilters, searchCatalogCards } from '@/services/catalogApi'
import { typeLabels } from '@/components/catalog/catalogLabels'
import { pageItems } from '@/utils/pagination'

const gameNames: Record<string, string> = {
  pokemon: 'Pokémon',
  magic: 'Magic: The Gathering',
  yugioh: 'Yu-Gi-Oh!',
  'one-piece': 'One Piece',
  digimon: 'Digimon',
}

const supportedGames = ['pokemon', 'magic', 'yugioh']

const futureFilters = ['Idioma', 'Condição', 'Preço', 'Disponibilidade']

const emptyFilters: CatalogFilters = { sets: [], rarities: [], types: [] }

type FilterKey = 'q' | 'set' | 'rarity' | 'type'

export function CatalogPage() {
  const { game } = useParams({ from: '/site-layout/catalogo/$game' })
  return <CatalogView key={game} game={game} />
}

function CatalogView({ game }: { game: string }) {
  const search = useSearch({ from: '/site-layout/catalogo/$game' })
  const navigate = useNavigate({ from: '/catalogo/$game' })
  const supportedGame = supportedGames.includes(game)
  const { q, set, rarity, type, sort, page } = search
  const currentPage = page ?? 1

  const [searchInput, setSearchInput] = useState(q ?? '')
  const [syncedQuery, setSyncedQuery] = useState(q)
  const [filtersOpen, setFiltersOpen] = useState(false)
  const [filters, setFilters] = useState<CatalogFilters | null>(null)
  const [attempt, setAttempt] = useState(0)
  const [result, setResult] = useState<{
    key: string
    data: CatalogPageData | null
  } | null>(null)

  if (q !== syncedQuery) {
    setSyncedQuery(q)
    setSearchInput(q ?? '')
  }

  function updateSearch(patch: Partial<CatalogSearch>) {
    navigate({ search: (prev) => ({ ...prev, ...patch, page: undefined }) })
  }

  useEffect(() => {
    const next = searchInput.trim() ? searchInput : undefined
    if (next === q) return
    const timer = window.setTimeout(
      () =>
        navigate({
          search: (prev) => ({ ...prev, q: next, page: undefined }),
          replace: true,
        }),
      350,
    )
    return () => window.clearTimeout(timer)
  }, [searchInput, q, navigate])

  useEffect(() => {
    if (!supportedGame) return
    let active = true
    fetchCatalogFilters(game)
      .then((data) => {
        if (active) setFilters(data)
      })
      .catch(() => {
        if (active) setFilters(emptyFilters)
      })
    return () => {
      active = false
    }
  }, [game, supportedGame])

  const requestKey = JSON.stringify([q, set, rarity, type, sort, page, attempt])

  useEffect(() => {
    if (!supportedGame) return
    let active = true
    searchCatalogCards(game, { q, set, rarity, type, sort, page })
      .then((data) => {
        if (active) setResult({ key: requestKey, data })
      })
      .catch(() => {
        if (active) setResult({ key: requestKey, data: null })
      })
    return () => {
      active = false
    }
  }, [game, supportedGame, requestKey, q, set, rarity, type, sort, page])

  const isLoading = supportedGame && result?.key !== requestKey
  const data = isLoading ? null : result?.data
  const loadError = supportedGame && !isLoading && !data
  const totalPages = data?.totalPages ?? 0
  const outOfRange = Boolean(
    data && data.totalElements > 0 && !data.content.length,
  )

  const options = filters ?? emptyFilters
  const setOptions = withSelected(
    options.sets.map((item) => ({ value: item, label: item })),
    set,
  )
  const rarityOptions = withSelected(
    options.rarities.map((item) => ({ value: item, label: item })),
    rarity,
  )
  const typeOptions = withSelected(
    options.types.map((item) => ({
      value: item,
      label: typeLabels[item] ?? item,
    })),
    type,
  )

  const activeFilters: { key: FilterKey; label: string }[] = []
  if (q) activeFilters.push({ key: 'q', label: `Busca: “${q.trim()}”` })
  if (set) activeFilters.push({ key: 'set', label: `Coleção: ${set}` })
  if (rarity)
    activeFilters.push({ key: 'rarity', label: `Raridade: ${rarity}` })
  if (type)
    activeFilters.push({
      key: 'type',
      label: `Tipo: ${typeLabels[type] ?? type}`,
    })
  const hasFilters = activeFilters.length > 0

  function clearFilters() {
    navigate({ search: (prev) => ({ sort: prev.sort }) })
  }

  function goToPage(number: number) {
    navigate({
      search: (prev) => ({ ...prev, page: number > 1 ? number : undefined }),
    })
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const gameName = gameNames[game] ?? game
  const total = data?.totalElements ?? 0

  return (
    <main className="min-h-screen bg-[#FAFBFE] pb-12 text-[#171443]">
      {/* Header global e footer continuam fora do escopo desta página. */}
      <div className="mx-auto w-full max-w-[1600px] px-4 pt-4 sm:px-6 xl:px-[38px]">
        <header className="relative mb-4 flex min-h-[116px] items-center overflow-hidden rounded-xl border border-[#eeeafb] bg-gradient-to-r from-[#f8f7ff] via-[#f2edff] to-[#e5d6fb] px-6 py-5 sm:px-8">
          <div
            aria-hidden="true"
            className="pointer-events-none absolute -right-12 -top-24 size-64 rounded-full border-[32px] border-white/40"
          />
          <div
            aria-hidden="true"
            className="pointer-events-none absolute right-[14%] top-0 size-32 rounded-full bg-primary-100/70 blur-2xl"
          />
          <div className="relative flex items-center gap-4">
            <span
              aria-hidden="true"
              className="flex size-14 shrink-0 items-center justify-center rounded-full border-4 border-[#25135c] bg-white text-3xl shadow-sm"
            >
              {game === 'pokemon' ? '◉' : '✦'}
            </span>
            <div>
              <h1 className="font-display text-2xl font-bold leading-tight text-[#21154d] sm:text-4xl">
                {gameName}
              </h1>
              <p className="mt-1 text-xs text-[#292057] sm:text-sm">
                Explore todas as cartas de {gameName}.
              </p>
            </div>
          </div>
        </header>

        <div className="mb-3 lg:hidden">
          <button
            type="button"
            onClick={() => setFiltersOpen((value) => !value)}
            aria-expanded={filtersOpen}
            className="w-full rounded-lg border border-[#dedcf3] bg-white px-4 py-3 text-left text-sm font-semibold text-primary-900"
          >
            {filtersOpen ? 'Ocultar filtros' : 'Filtros'}
            {hasFilters ? ` (${activeFilters.length})` : ''}
          </button>
        </div>

        <div className="grid items-start gap-4 lg:grid-cols-[280px_minmax(0,1fr)] xl:grid-cols-[335px_minmax(0,1fr)] xl:gap-[26px]">
          <aside
            className={`${filtersOpen ? 'block' : 'hidden'} rounded-xl border border-[#e6e5f3] bg-white p-4 shadow-sm lg:block lg:min-h-[590px]`}
            aria-label="Filtros do catálogo"
          >
            <div className="mb-4 flex items-center justify-between gap-2">
              <h2 className="font-display text-lg font-bold">Filtros</h2>
              {hasFilters && (
                <button
                  type="button"
                  onClick={clearFilters}
                  className="text-xs font-semibold text-primary-900 underline underline-offset-2"
                >
                  Limpar filtros
                </button>
              )}
            </div>

            <label
              htmlFor="catalog-search"
              className="mb-2 block text-xs font-semibold"
            >
              Buscar carta
            </label>
            <input
              id="catalog-search"
              type="search"
              value={searchInput}
              onChange={(event) => setSearchInput(event.target.value)}
              placeholder="Buscar carta no catálogo..."
              className="mb-4 w-full rounded-lg border border-[#e1def5] bg-white px-3 py-3 text-sm outline-none focus:border-primary-900 focus:ring-2 focus:ring-primary-100"
            />

            <FilterGroup
              id="catalog-set"
              title="Coleção"
              allLabel="Todas as coleções"
              value={set}
              options={setOptions}
              onChange={(value) => updateSearch({ set: value })}
            />
            <FilterGroup
              id="catalog-rarity"
              title="Raridade"
              allLabel="Todas as raridades"
              value={rarity}
              options={rarityOptions}
              onChange={(value) => updateSearch({ rarity: value })}
            />
            <FilterGroup
              id="catalog-type"
              title="Tipo"
              allLabel="Todos os tipos"
              value={type}
              options={typeOptions}
              onChange={(value) => updateSearch({ type: value })}
            />

            {futureFilters.map((filter) => (
              <div
                key={filter}
                className="mb-2 flex items-center justify-between rounded-lg border border-[#e5e2f4] px-3 py-3 text-sm"
                aria-label={`${filter}: em desenvolvimento`}
              >
                <span className="font-semibold">{filter}</span>
                <span className="text-[10px] text-neutral-500">Em breve</span>
              </div>
            ))}
          </aside>

          <section className="min-w-0" aria-label="Resultados do catálogo">
            <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
              <p className="text-sm font-semibold" aria-live="polite">
                {isLoading ? (
                  'Buscando cartas...'
                ) : data ? (
                  <>
                    <span className="underline decoration-primary-900 underline-offset-2">
                      {total.toLocaleString('pt-BR')}
                    </span>{' '}
                    {total === 1 ? 'carta encontrada' : 'cartas encontradas'}
                  </>
                ) : null}
              </p>
              <label
                className="flex items-center gap-2 text-xs text-[#514b7e]"
                htmlFor="catalog-sort"
              >
                <span className="hidden sm:inline">Ordenar por:</span>
                <select
                  id="catalog-sort"
                  value={sort ?? 'name-asc'}
                  onChange={(event) =>
                    updateSearch({
                      sort:
                        event.target.value === 'name-desc'
                          ? 'name-desc'
                          : undefined,
                    })
                  }
                  className="rounded-lg border border-[#e1def5] bg-white px-3 py-2 text-xs text-[#171443] sm:text-sm"
                >
                  <option value="name-asc">Nome: A–Z</option>
                  <option value="name-desc">Nome: Z–A</option>
                </select>
              </label>
            </div>

            {hasFilters && (
              <ul
                aria-label="Filtros ativos"
                className="mb-3 flex flex-wrap items-center gap-2"
              >
                {activeFilters.map((filter) => (
                  <li key={filter.key}>
                    <button
                      type="button"
                      onClick={() => updateSearch({ [filter.key]: undefined })}
                      aria-label={`Remover filtro ${filter.label}`}
                      className="inline-flex items-center gap-1.5 rounded-full bg-primary-100 px-3 py-1 text-xs font-semibold text-primary-900 hover:bg-primary-100/70"
                    >
                      {filter.label}
                      <span aria-hidden="true">×</span>
                    </button>
                  </li>
                ))}
                <li>
                  <button
                    type="button"
                    onClick={clearFilters}
                    className="text-xs font-semibold text-primary-900 underline underline-offset-2"
                  >
                    Limpar filtros
                  </button>
                </li>
              </ul>
            )}

            {!supportedGame ? (
              <EmptyState
                title="Catálogo em breve"
                message={`O catálogo de ${gameName} ainda não está disponível.`}
              />
            ) : isLoading ? (
              <div
                role="status"
                aria-label="Carregando cartas"
                className="grid grid-cols-2 gap-2.5 sm:grid-cols-3 lg:grid-cols-3 xl:grid-cols-5"
              >
                {Array.from({ length: 10 }, (_, index) => (
                  <div
                    key={index}
                    className="animate-pulse overflow-hidden rounded-xl border border-[#eeeafb] bg-white"
                  >
                    <div className="aspect-[4/5] bg-neutral-100" />
                    <div className="space-y-2 p-3.5">
                      <div className="h-3.5 w-3/4 rounded bg-neutral-100" />
                      <div className="h-3 w-1/2 rounded bg-neutral-100" />
                      <div className="h-5 w-16 rounded-full bg-neutral-100" />
                    </div>
                  </div>
                ))}
              </div>
            ) : loadError ? (
              <div
                role="alert"
                className="rounded-xl border border-red-200 bg-white px-6 py-12 text-center"
              >
                <h3 className="font-display text-lg font-bold text-red-700">
                  Não foi possível carregar as cartas
                </h3>
                <p className="mt-2 text-sm text-neutral-500">
                  Ocorreu um problema ao buscar o catálogo. Tente novamente.
                </p>
                <button
                  type="button"
                  onClick={() => setAttempt((value) => value + 1)}
                  className="mt-5 rounded-lg bg-primary-900 px-5 py-2.5 text-sm font-semibold text-white"
                >
                  Tentar novamente
                </button>
              </div>
            ) : outOfRange ? (
              <EmptyState
                title="Página não encontrada"
                message="Essa página não existe para a busca atual."
                action={{
                  label: 'Ir para a primeira página',
                  onClick: () => goToPage(1),
                }}
              />
            ) : !data?.content.length ? (
              <EmptyState
                title="Nenhuma carta encontrada"
                message="Não encontramos cartas correspondentes à sua busca ou aos filtros selecionados."
                action={
                  hasFilters
                    ? { label: 'Limpar filtros', onClick: clearFilters }
                    : undefined
                }
              />
            ) : (
              <div className="grid grid-cols-2 gap-2.5 sm:grid-cols-3 lg:grid-cols-3 xl:grid-cols-5">
                {data.content.map((card) => (
                  <CatalogCard key={card.id} card={card} />
                ))}
              </div>
            )}

            {data && !outOfRange && totalPages > 1 && (
              <nav
                aria-label="Paginação do catálogo"
                className="mt-7 flex flex-wrap items-center justify-center gap-2"
              >
                <button
                  type="button"
                  disabled={currentPage === 1}
                  onClick={() => goToPage(currentPage - 1)}
                  className="rounded-lg border border-[#e1def5] bg-white px-3 py-2 text-sm disabled:cursor-not-allowed disabled:opacity-40"
                >
                  Anterior
                </button>
                {pageItems(currentPage, totalPages).map((item, index) =>
                  item === 'gap' ? (
                    <span
                      key={`gap-${index}`}
                      aria-hidden="true"
                      className="px-1 text-sm text-neutral-500"
                    >
                      …
                    </span>
                  ) : (
                    <button
                      key={item}
                      type="button"
                      aria-current={item === currentPage ? 'page' : undefined}
                      aria-label={`Página ${item}`}
                      onClick={() => goToPage(item)}
                      className={`size-9 rounded-lg text-sm font-semibold ${item === currentPage ? 'bg-primary-900 text-white' : 'border border-[#e1def5] bg-white'}`}
                    >
                      {item}
                    </button>
                  ),
                )}
                <button
                  type="button"
                  disabled={currentPage >= totalPages}
                  onClick={() => goToPage(currentPage + 1)}
                  className="rounded-lg border border-[#e1def5] bg-white px-3 py-2 text-sm disabled:cursor-not-allowed disabled:opacity-40"
                >
                  Próxima
                </button>
              </nav>
            )}
          </section>
        </div>
      </div>
    </main>
  )
}

type Option = { value: string; label: string }

function FilterGroup({
  id,
  title,
  allLabel,
  value,
  options,
  onChange,
}: {
  id: string
  title: string
  allLabel: string
  value: string | undefined
  options: Option[]
  onChange: (value: string | undefined) => void
}) {
  return (
    <details className="mb-2 rounded-lg border border-[#e5e2f4]" open>
      <summary className="cursor-pointer px-3 py-3 text-sm font-semibold">
        {title}
        {value ? ` (1)` : ''}
      </summary>
      <div className="border-t border-[#eeeafb] px-3 pb-3 pt-2">
        <label htmlFor={id} className="sr-only">
          Filtrar por {title.toLowerCase()}
        </label>
        <select
          id={id}
          value={value ?? ''}
          onChange={(event) => onChange(event.target.value || undefined)}
          disabled={!options.length && !value}
          className="w-full rounded-lg border border-[#e5e2f4] bg-white px-2 py-2 text-sm disabled:cursor-not-allowed disabled:opacity-60"
        >
          <option value="">{allLabel}</option>
          {options.map((item) => (
            <option key={item.value} value={item.value}>
              {item.label}
            </option>
          ))}
        </select>
      </div>
    </details>
  )
}

function EmptyState({
  title,
  message,
  action,
}: {
  title: string
  message: string
  action?: { label: string; onClick: () => void }
}) {
  return (
    <div className="rounded-xl border border-dashed border-[#dcd8ed] bg-white px-6 py-16 text-center">
      <h3 className="font-display text-lg font-bold">{title}</h3>
      <p className="mt-2 text-sm text-neutral-500">{message}</p>
      {action && (
        <button
          type="button"
          onClick={action.onClick}
          className="mt-5 rounded-lg bg-primary-900 px-5 py-2.5 text-sm font-semibold text-white"
        >
          {action.label}
        </button>
      )}
    </div>
  )
}

function withSelected(options: Option[], selected: string | undefined) {
  if (!selected || options.some((item) => item.value === selected))
    return options
  return [{ value: selected, label: selected }, ...options]
}
