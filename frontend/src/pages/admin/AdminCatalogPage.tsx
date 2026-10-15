import {
  ADMIN_GAMES,
  GAME_LABEL,
  SOURCE_LABEL,
  apiMessage,
} from '@/components/admin/adminFormat'
import { typeLabels } from '@/components/catalog/catalogLabels'
import { Alert } from '@/components/ui/Alert'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { Icon } from '@/components/ui/Icon'
import { Input } from '@/components/ui/Input'
import { Modal } from '@/components/ui/Modal'
import { Select, type SelectOption } from '@/components/ui/Select'
import { useRemoteData } from '@/hooks/useRemoteData'
import {
  hideCatalogCard,
  searchAdminCatalog,
  unhideCatalogCard,
} from '@/services/adminApi'
import { fetchCatalogFilters } from '@/services/catalogApi'
import { useSession } from '@/session'
import type { AdminCatalogCard, AdminCatalogSearch } from '@/types/AdminCatalog'
import type { CatalogFilters } from '@/types/Catalog'
import { pageItems } from '@/utils/pagination'
import { useNavigate, useSearch } from '@tanstack/react-router'
import axios from 'axios'
import { useCallback, useEffect, useId, useState, type ReactNode } from 'react'

const REASON_MIN = 10
const REASON_MAX = 300

const ALL = ''

const VISIBILITY_OPTIONS: SelectOption[] = [
  { value: ALL, label: 'Visíveis e ocultas' },
  { value: 'visible', label: 'Somente visíveis' },
  { value: 'hidden', label: 'Somente ocultas' },
]

const GAME_OPTIONS: SelectOption[] = [
  { value: ALL, label: 'Todos os TCGs' },
  ...ADMIN_GAMES.map((item) => ({ value: item.slug, label: item.label })),
]

const numberFormat = new Intl.NumberFormat('pt-BR')

type Pending = { card: AdminCatalogCard; action: 'hide' | 'unhide' }

export function AdminCatalogPage() {
  const search = useSearch({ from: '/site-layout/admin/catalogo' })
  const navigate = useNavigate({ from: '/admin/catalogo' })
  const { refreshUser } = useSession()
  const { game, q, set, rarity, type, visibility, page } = search
  const currentPage = page ?? 1

  const [searchInput, setSearchInput] = useState(q ?? '')
  const [syncedQuery, setSyncedQuery] = useState(q)
  const [filters, setFilters] = useState<{
    game: string
    data: CatalogFilters
  } | null>(null)
  const [updated, setUpdated] = useState<Record<string, AdminCatalogCard>>({})
  const [pending, setPending] = useState<Pending | null>(null)
  const [reason, setReason] = useState('')
  const [reasonError, setReasonError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  if (q !== syncedQuery) {
    setSyncedQuery(q)
    setSearchInput(q ?? '')
  }

  const fetchPage = useCallback(
    () => searchAdminCatalog({ game, q, set, rarity, type, visibility, page }),
    [game, q, set, rarity, type, visibility, page],
  )
  const { state, retry } = useRemoteData(
    JSON.stringify([game, q, set, rarity, type, visibility, page]),
    fetchPage,
  )

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
    if (!game) return
    let active = true
    fetchCatalogFilters(game)
      .then((data) => {
        if (active) setFilters({ game, data })
      })
      .catch(() => {
        if (active)
          setFilters({ game, data: { sets: [], rarities: [], types: [] } })
      })
    return () => {
      active = false
    }
  }, [game])

  function updateSearch(patch: Partial<AdminCatalogSearch>) {
    navigate({ search: (prev) => ({ ...prev, ...patch, page: undefined }) })
  }

  function goToPage(number: number) {
    navigate({
      search: (prev) => ({ ...prev, page: number > 1 ? number : undefined }),
    })
  }

  function open(card: AdminCatalogCard, action: Pending['action']) {
    setPending({ card, action })
    setReason('')
    setReasonError(null)
  }

  function close() {
    if (!saving) setPending(null)
  }

  async function confirm() {
    if (!pending || saving) return
    const trimmed = reason.trim()
    if (
      pending.action === 'hide' &&
      (trimmed.length < REASON_MIN || trimmed.length > REASON_MAX)
    ) {
      setReasonError(
        `O motivo deve ter entre ${REASON_MIN} e ${REASON_MAX} caracteres.`,
      )
      return
    }
    setSaving(true)
    setError(null)
    setSuccess(null)
    try {
      const card =
        pending.action === 'hide'
          ? await hideCatalogCard(pending.card.id, trimmed)
          : await unhideCatalogCard(pending.card.id)
      setUpdated((current) => ({ ...current, [card.id]: card }))
      setSuccess(
        card.hidden
          ? `${card.name} foi ocultada do catálogo público.`
          : `${card.name} voltou ao catálogo público.`,
      )
      setPending(null)
    } catch (cause) {
      if (axios.isAxiosError(cause) && cause.response?.status === 403) {
        void refreshUser()
      }
      const message =
        apiMessage(cause) ??
        (pending.action === 'hide'
          ? 'Não foi possível ocultar a carta.'
          : 'Não foi possível reexibir a carta.')
      if (pending.action === 'hide') {
        setReasonError(message)
      } else {
        setError(message)
        setPending(null)
      }
    } finally {
      setSaving(false)
    }
  }

  const options = filters && filters.game === game ? filters.data : null
  const data = state.status === 'ready' ? state.data : null
  const rows = data?.content.map((card) => updated[card.id] ?? card) ?? []
  const outOfRange = Boolean(
    data && data.totalElements > 0 && !data.content.length,
  )
  const hasFilters = Boolean(q || game || set || rarity || type || visibility)

  return (
    <Card>
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-neutral-200 px-5 py-4">
        <h2 className="text-lg font-semibold text-neutral-900">Catálogo</h2>
        <p className="text-sm text-neutral-500" aria-live="polite">
          {data
            ? `${numberFormat.format(data.totalElements)} ${data.totalElements === 1 ? 'carta encontrada' : 'cartas encontradas'}`
            : null}
        </p>
      </div>

      <div className="space-y-4 p-5">
        <Alert variant="info">
          As cartas vêm das APIs oficiais (Pokémon TCG API, YGOPRODeck e
          Scryfall) e não são editadas aqui. Você pode ocultar uma carta com
          dado ou imagem problemáticos.
        </Alert>

        {error ? (
          <Alert variant="error" onDismiss={() => setError(null)}>
            {error}
          </Alert>
        ) : null}
        {success ? (
          <Alert variant="success" onDismiss={() => setSuccess(null)}>
            {success}
          </Alert>
        ) : null}

        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-6">
          <Input
            variant="search"
            type="search"
            className="sm:col-span-2 xl:col-span-2"
            aria-label="Pesquisar pelo nome da carta"
            value={searchInput}
            onChange={(event) => setSearchInput(event.target.value)}
            placeholder="Pesquisar pelo nome da carta"
          />
          <FilterSelect
            label="TCG"
            options={GAME_OPTIONS}
            value={game ?? ALL}
            onValueChange={(value) =>
              updateSearch({
                game: value || undefined,
                set: undefined,
                rarity: undefined,
                type: undefined,
              })
            }
          />
          <FilterSelect
            label="Coleção"
            disabled={!game}
            options={withAll(
              'Todas as coleções',
              options?.sets.map((item) => ({ value: item, label: item })),
              set,
            )}
            value={set ?? ALL}
            onValueChange={(value) => updateSearch({ set: value || undefined })}
          />
          <FilterSelect
            label="Raridade"
            disabled={!game}
            options={withAll(
              'Todas as raridades',
              options?.rarities.map((item) => ({ value: item, label: item })),
              rarity,
            )}
            value={rarity ?? ALL}
            onValueChange={(value) =>
              updateSearch({ rarity: value || undefined })
            }
          />
          <FilterSelect
            label="Catálogo público"
            options={VISIBILITY_OPTIONS}
            value={visibility ?? ALL}
            onValueChange={(value) =>
              updateSearch({
                visibility:
                  value === 'visible' || value === 'hidden' ? value : undefined,
              })
            }
          />
          {game ? (
            <FilterSelect
              label="Tipo"
              className="sm:col-span-2 xl:col-span-2"
              options={withAll(
                'Todos os tipos',
                options?.types.map((item) => ({
                  value: item,
                  label: typeLabels[item] ?? item,
                })),
                type,
              )}
              value={type ?? ALL}
              onValueChange={(value) =>
                updateSearch({ type: value || undefined })
              }
            />
          ) : null}
        </div>

        {hasFilters ? (
          <button
            type="button"
            onClick={() => navigate({ search: {} })}
            className="text-xs font-semibold text-primary-900 underline underline-offset-2"
          >
            Limpar filtros
          </button>
        ) : null}

        {state.status === 'loading' ? (
          <div
            role="status"
            aria-label="Carregando cartas"
            className="animate-pulse space-y-2"
          >
            {Array.from({ length: 6 }, (_, index) => (
              <div
                key={index}
                className="h-14 rounded-control bg-neutral-100"
              />
            ))}
          </div>
        ) : state.status !== 'ready' ? (
          <div role="alert" className="space-y-3 py-6 text-center text-sm">
            <p className="text-neutral-700">
              Não foi possível carregar o catálogo.
            </p>
            <Button variant="outline" onClick={retry}>
              Tentar novamente
            </Button>
          </div>
        ) : outOfRange ? (
          <div className="space-y-3 py-6 text-center text-sm">
            <p className="text-neutral-700">
              Essa página não existe para a busca atual.
            </p>
            <Button variant="outline" onClick={() => goToPage(1)}>
              Ir para a primeira página
            </Button>
          </div>
        ) : rows.length === 0 ? (
          <p className="py-6 text-center text-sm text-neutral-500">
            Nenhuma carta encontrada para a busca ou os filtros selecionados.
          </p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[720px] text-left text-sm">
              <thead className="text-xs font-semibold tracking-wide text-neutral-500 uppercase">
                <tr className="border-b border-neutral-200">
                  <th scope="col" className="py-2 pr-3">
                    Carta
                  </th>
                  <th scope="col" className="px-3 py-2">
                    TCG
                  </th>
                  <th scope="col" className="px-3 py-2">
                    Raridade
                  </th>
                  <th scope="col" className="px-3 py-2">
                    Origem
                  </th>
                  <th scope="col" className="px-3 py-2">
                    Catálogo público
                  </th>
                  <th scope="col" className="py-2 pl-3">
                    <span className="sr-only">Ações</span>
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-200">
                {rows.map((card) => (
                  <tr key={card.id}>
                    <td className="py-3 pr-3">
                      <div className="flex items-center gap-3">
                        <CardThumb card={card} />
                        <div className="min-w-0">
                          <p className="truncate font-semibold text-neutral-900">
                            {card.name}
                          </p>
                          <p className="truncate text-xs text-neutral-500">
                            {card.setName}
                          </p>
                        </div>
                      </div>
                    </td>
                    <td className="px-3 py-3 whitespace-nowrap text-neutral-700">
                      {GAME_LABEL[card.cardGame]}
                    </td>
                    <td className="px-3 py-3 text-neutral-700">
                      {card.rarity ?? 'Não informada'}
                    </td>
                    <td className="px-3 py-3">
                      <div className="flex flex-col items-start gap-1">
                        <Badge variant="secondary">
                          {SOURCE_LABEL[card.source]}
                        </Badge>
                        <code
                          title={card.externalId}
                          className="max-w-40 truncate rounded bg-neutral-100 px-1.5 py-0.5 text-xs text-neutral-700"
                        >
                          {card.externalId}
                        </code>
                      </div>
                    </td>
                    <td className="px-3 py-3">
                      {card.hidden ? (
                        <span title={card.hiddenReason ?? undefined}>
                          <Badge variant="warning">Oculta</Badge>
                        </span>
                      ) : (
                        <Badge variant="success">Visível</Badge>
                      )}
                    </td>
                    <td className="py-3 pl-3 text-right">
                      {card.hidden ? (
                        <Button
                          variant="outline"
                          onClick={() => open(card, 'unhide')}
                          aria-label={`Reexibir ${cardLabel(card)}`}
                        >
                          Reexibir
                        </Button>
                      ) : (
                        <Button
                          variant="ghost"
                          onClick={() => open(card, 'hide')}
                          aria-label={`Ocultar ${cardLabel(card)}`}
                        >
                          Ocultar
                        </Button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {data && !outOfRange && data.totalElements > 0 ? (
          <div className="flex flex-wrap items-center justify-between gap-3 pt-2">
            <p className="text-xs text-neutral-500">
              {`Exibindo ${numberFormat.format(data.page * data.size + 1)}–${numberFormat.format(data.page * data.size + rows.length)} de ${numberFormat.format(data.totalElements)} cartas`}
            </p>
            {data.totalPages > 1 ? (
              <nav
                aria-label="Paginação do catálogo administrativo"
                className="flex flex-wrap items-center gap-1"
              >
                <PageButton
                  label="Página anterior"
                  disabled={currentPage === 1}
                  onClick={() => goToPage(currentPage - 1)}
                >
                  ‹
                </PageButton>
                {pageItems(currentPage, data.totalPages).map((item, index) =>
                  item === 'gap' ? (
                    <span
                      key={`gap-${index}`}
                      aria-hidden="true"
                      className="px-1 text-sm text-neutral-500"
                    >
                      …
                    </span>
                  ) : (
                    <PageButton
                      key={item}
                      label={`Página ${item}`}
                      current={item === currentPage}
                      onClick={() => goToPage(item)}
                    >
                      {item}
                    </PageButton>
                  ),
                )}
                <PageButton
                  label="Próxima página"
                  disabled={currentPage >= data.totalPages}
                  onClick={() => goToPage(currentPage + 1)}
                >
                  ›
                </PageButton>
              </nav>
            ) : null}
          </div>
        ) : null}
      </div>

      <Modal
        open={pending?.action === 'hide'}
        title="Ocultar carta do catálogo"
        onClose={close}
        secondaryAction={{ label: 'Cancelar' }}
        primaryAction={{
          label: saving ? 'Ocultando...' : 'Ocultar carta',
          onClick: () => void confirm(),
        }}
      >
        {pending ? (
          <>
            <p className="text-sm">
              A carta some do catálogo público, mas continua válida para
              coleções e anúncios existentes.
            </p>
            <div className="flex items-center gap-3 rounded-control border border-neutral-200 p-3">
              <CardThumb card={pending.card} />
              <div className="min-w-0 text-sm">
                <p className="truncate font-semibold text-neutral-900">
                  {pending.card.name}
                </p>
                <p className="truncate text-xs text-neutral-500">
                  {pending.card.setName} · {SOURCE_LABEL[pending.card.source]} ·{' '}
                  <code>{pending.card.externalId}</code>
                </p>
              </div>
            </div>
            <p className="flex gap-2 rounded-control bg-primary-100/60 p-3 text-xs text-primary-900">
              <Icon name="info" size={16} />
              Os dados importados não são alterados. A ocultação continua
              valendo mesmo se a carta for importada de novo.
            </p>
            <label
              htmlFor="hide-reason"
              className="block pt-2 text-sm font-semibold text-neutral-900"
            >
              Motivo <span className="text-error-700">*</span>
            </label>
            <textarea
              id="hide-reason"
              value={reason}
              maxLength={REASON_MAX}
              rows={3}
              onChange={(event) => {
                setReason(event.target.value)
                setReasonError(null)
              }}
              aria-invalid={reasonError ? true : undefined}
              aria-describedby="hide-reason-hint"
              placeholder="Ex.: imagem da carta está trocada na API."
              className="w-full rounded-control border border-neutral-300 px-3 py-2 text-sm text-neutral-900 outline-none placeholder:text-neutral-500 hover:border-primary-900 focus:border-primary-900 focus:ring-2 focus:ring-secondary-500 aria-invalid:border-error-700"
            />
            <div
              id="hide-reason-hint"
              className="flex justify-between gap-3 text-xs"
            >
              <span
                className={reasonError ? 'text-error-700' : 'text-neutral-500'}
              >
                {reasonError ??
                  `Entre ${REASON_MIN} e ${REASON_MAX} caracteres.`}
              </span>
              <span className="text-neutral-500">
                {reason.trim().length}/{REASON_MAX}
              </span>
            </div>
          </>
        ) : null}
      </Modal>

      <Modal
        open={pending?.action === 'unhide'}
        title="Reexibir carta"
        onClose={close}
        secondaryAction={{ label: 'Cancelar' }}
        primaryAction={{
          label: saving ? 'Reexibindo...' : 'Reexibir',
          onClick: () => void confirm(),
        }}
      >
        {pending ? (
          <>
            <p>
              <strong>{pending.card.name}</strong> volta a aparecer no catálogo
              público.
            </p>
            {pending.card.hiddenReason ? (
              <p className="text-sm text-neutral-500">
                Motivo da ocultação: {pending.card.hiddenReason}
              </p>
            ) : null}
          </>
        ) : null}
      </Modal>
    </Card>
  )
}

function cardLabel(card: AdminCatalogCard) {
  return card.setName ? `${card.name} (${card.setName})` : card.name
}

function CardThumb({ card }: { card: AdminCatalogCard }) {
  return card.imageUrl ? (
    <img
      src={card.imageUrl}
      alt=""
      loading="lazy"
      className="h-11 w-8 shrink-0 rounded object-cover"
    />
  ) : (
    <span
      aria-hidden="true"
      className="font-display flex h-11 w-8 shrink-0 items-center justify-center rounded bg-gradient-to-br from-primary-300 to-primary-700 text-xs font-bold text-white"
    >
      {card.name.charAt(0)}
    </span>
  )
}

function PageButton({
  label,
  current = false,
  disabled = false,
  onClick,
  children,
}: {
  label: string
  current?: boolean
  disabled?: boolean
  onClick: () => void
  children: ReactNode
}) {
  return (
    <button
      type="button"
      aria-label={label}
      aria-current={current ? 'page' : undefined}
      disabled={disabled}
      onClick={onClick}
      className={`size-8 rounded-control text-sm font-semibold disabled:cursor-not-allowed disabled:opacity-40 ${current ? 'bg-primary-900 text-white' : 'border border-neutral-200 bg-white text-neutral-700 hover:border-primary-900'}`}
    >
      {children}
    </button>
  )
}

function FilterSelect({
  label,
  className,
  ...props
}: {
  label: string
  className?: string
  options: SelectOption[]
  value: string
  disabled?: boolean
  onValueChange: (value: string) => void
}) {
  const id = useId()
  return (
    <div className={className}>
      <label htmlFor={id} className="sr-only">
        {label}
      </label>
      <Select id={id} variant="filter" {...props} />
    </div>
  )
}

function withAll(
  allLabel: string,
  items: SelectOption[] | undefined,
  selected: string | undefined,
): SelectOption[] {
  const list = items ?? []
  const extra =
    selected && !list.some((item) => item.value === selected)
      ? [{ value: selected, label: selected }]
      : []
  return [{ value: ALL, label: allLabel }, ...extra, ...list]
}
