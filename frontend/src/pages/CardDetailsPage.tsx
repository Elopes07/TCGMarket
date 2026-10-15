import { AddToCollectionModal } from '@/components/catalog/AddToCollectionModal'
import {
  attackCost,
  attacks,
  commonRows,
  GAME_INFO,
  gameRows,
  highlightChips,
  imageAlt,
  rulesText,
  setLine,
  sourceRows,
  toCatalogCard,
  type InfoRow,
} from '@/components/catalog/cardDetailsFormat'
import { Alert } from '@/components/ui/Alert'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { cn } from '@/components/ui/cn'
import { Icon } from '@/components/ui/Icon'
import { Modal } from '@/components/ui/Modal'
import { useRemoteData } from '@/hooks/useRemoteData'
import { fetchCatalogCard } from '@/services/catalogApi'
import { useSession } from '@/session'
import type { CatalogCardDetails } from '@/types/Catalog'
import {
  Link,
  useCanGoBack,
  useNavigate,
  useParams,
  useRouter,
  useSearch,
} from '@tanstack/react-router'
import { type ReactNode, useCallback, useState } from 'react'

const ADD_LOGIN_NOTICE = 'Entre na sua conta para adicionar cartas à coleção.'

export function CardDetailsPage() {
  const { id } = useParams({ from: '/site-layout/cartas/$id' })
  return <CardDetailsView key={id} id={id} />
}

function CardDetailsView({ id }: { id: string }) {
  const { adicionar } = useSearch({ from: '/site-layout/cartas/$id' })
  const navigate = useNavigate({ from: '/cartas/$id' })
  const { user, setLoginNotice } = useSession()
  const fetchCard = useCallback(() => fetchCatalogCard(id), [id])
  const { state, retry } = useRemoteData(id, fetchCard)
  const [modalOpen, setModalOpen] = useState(false)

  const card = state.status === 'ready' ? state.data : null
  // Volta do login com a intenção de adicionar: o modal abre sozinho.
  const showModal = Boolean(card && user && (modalOpen || adicionar))

  function addToCollection() {
    if (user) {
      setModalOpen(true)
      return
    }
    setLoginNotice(ADD_LOGIN_NOTICE)
    navigate({
      to: '/login',
      search: { redirect: `/cartas/${encodeURIComponent(id)}?adicionar=1` },
    })
  }

  function closeModal() {
    setModalOpen(false)
    if (adicionar) navigate({ search: {}, replace: true })
  }

  return (
    <main
      className={cn(
        'mx-auto w-full max-w-[1400px] space-y-5 px-4 py-6 sm:px-6',
        card && 'pb-28 sm:pb-6',
      )}
    >
      <Breadcrumb card={card} loading={state.status === 'loading'} />

      {state.status === 'loading' ? <DetailsSkeleton /> : null}
      {state.status === 'notFound' ? <DetailsStatus kind="notFound" /> : null}
      {state.status === 'error' ? (
        <DetailsStatus kind="error" onRetry={retry} />
      ) : null}
      {card ? <Details card={card} onAdd={addToCollection} /> : null}

      {card && showModal ? (
        <AddToCollectionModal card={toCatalogCard(card)} onClose={closeModal} />
      ) : null}
    </main>
  )
}

function Breadcrumb({
  card,
  loading,
}: {
  card: CatalogCardDetails | null
  loading: boolean
}) {
  const game = card ? GAME_INFO[card.cardGame] : null
  return (
    <nav aria-label="Você está em" className="text-xs text-neutral-500">
      <ol className="flex flex-wrap items-center gap-1">
        <li>
          <Link
            to="/"
            aria-label="Início"
            className="inline-flex text-primary-900 hover:text-primary-800"
          >
            <Icon name="home" size={16} />
          </Link>
        </li>
        {card && game ? (
          <>
            <Crumb>
              <Link
                to="/catalogo/$game"
                params={{ game: game.slug }}
                className="hover:text-primary-900"
              >
                {game.name}
              </Link>
            </Crumb>
            {card.setName ? (
              <Crumb className="hidden sm:flex">
                <Link
                  to="/catalogo/$game"
                  params={{ game: game.slug }}
                  search={{ set: card.setName }}
                  className="hover:text-primary-900"
                >
                  {card.setName}
                </Link>
              </Crumb>
            ) : null}
            <Crumb>
              <span aria-current="page" className="text-neutral-700">
                {card.name}
              </span>
            </Crumb>
          </>
        ) : loading ? (
          <Crumb>
            <span aria-current="page">Carregando...</span>
          </Crumb>
        ) : null}
      </ol>
    </nav>
  )
}

function Crumb({
  children,
  className,
}: {
  children: ReactNode
  className?: string
}) {
  return (
    <li className={cn('flex items-center gap-1', className)}>
      <span aria-hidden="true">›</span>
      {children}
    </li>
  )
}

function Details({
  card,
  onAdd,
}: {
  card: CatalogCardDetails
  onAdd: () => void
}) {
  const game = GAME_INFO[card.cardGame]
  const line = setLine(card)
  const chips = highlightChips(card)
  const info = commonRows(card)

  return (
    <>
      <div className="grid items-start gap-6 md:grid-cols-[260px_minmax(0,1fr)] xl:grid-cols-[300px_minmax(0,1fr)_300px]">
        <CardImage card={card} />

        <section className="min-w-0 space-y-4" aria-labelledby="card-name">
          <div className="space-y-2">
            <Badge variant="primary">{game.name}</Badge>
            <h1
              id="card-name"
              className="font-display line-clamp-2 text-3xl leading-tight font-bold break-words text-neutral-900 sm:text-4xl"
            >
              {card.name}
            </h1>
            {line ? <p className="text-sm text-neutral-700">{line}</p> : null}
          </div>

          {card.rarity || chips.length ? (
            <ul className="flex flex-wrap gap-2" aria-label="Destaques">
              {card.rarity ? (
                <li>
                  <Badge variant="rare">{card.rarity}</Badge>
                </li>
              ) : null}
              {chips.map((chip) => (
                <li key={chip}>
                  <Badge variant="neutral">{chip}</Badge>
                </li>
              ))}
            </ul>
          ) : null}

          {info.length ? (
            <Card className="px-4">
              <InfoList rows={info} />
            </Card>
          ) : null}

          <InfoTabs card={card} />
        </section>

        <aside className="hidden sm:block md:col-start-2 xl:sticky xl:top-4 xl:col-start-3 xl:row-start-1">
          <Card className="space-y-3 p-4">
            <AddButton onClick={onAdd} />
          </Card>
        </aside>
      </div>

      <div className="fixed inset-x-0 bottom-0 z-20 border-t border-neutral-200 bg-white p-3 shadow-[0_-4px_12px_rgba(0,0,0,0.06)] sm:hidden">
        <AddButton onClick={onAdd} />
      </div>
    </>
  )
}

function AddButton({
  onClick,
  disabled,
}: {
  onClick?: () => void
  disabled?: boolean
}) {
  return (
    <Button
      className="h-12 w-full"
      icon={<Icon name="plus" size={20} />}
      onClick={onClick}
      disabled={disabled}
    >
      Adicionar à minha coleção
    </Button>
  )
}

function CardImage({ card }: { card: CatalogCardDetails }) {
  const [failed, setFailed] = useState(false)
  const [zoomed, setZoomed] = useState(false)
  const available = Boolean(card.imageUrl) && !failed
  const alt = imageAlt(card)

  return (
    <div className="mx-auto w-full max-w-[300px] space-y-2 md:row-span-2 xl:row-span-1">
      <div className="aspect-[63/88] overflow-hidden rounded-surface border border-neutral-200 bg-white shadow-sm">
        {available ? (
          <img
            src={card.imageUrl ?? undefined}
            alt={alt}
            onError={() => setFailed(true)}
            className="h-full w-full object-contain"
          />
        ) : (
          <div
            role="img"
            aria-label={`${alt}: imagem indisponível`}
            className="flex h-full w-full flex-col items-center justify-center gap-2 bg-neutral-100 text-neutral-500"
          >
            <span aria-hidden="true" className="text-5xl">
              🃏
            </span>
            <span className="text-sm">Imagem indisponível</span>
          </div>
        )}
      </div>
      {available ? (
        <Button
          variant="ghost"
          className="w-full"
          onClick={() => setZoomed(true)}
        >
          Ver imagem completa
        </Button>
      ) : null}
      <Modal open={zoomed} title={card.name} onClose={() => setZoomed(false)}>
        <img
          src={card.imageUrl ?? undefined}
          alt={alt}
          className="mx-auto max-h-[75dvh] w-full object-contain"
        />
      </Modal>
    </div>
  )
}

function InfoList({ rows }: { rows: InfoRow[] }) {
  return (
    <dl className="divide-y divide-neutral-100 text-sm">
      {rows.map((row) => (
        <div key={row.label} className="flex justify-between gap-4 py-2.5">
          <dt className="text-neutral-500">{row.label}</dt>
          <dd className="text-right font-semibold break-words text-neutral-900">
            {row.value}
          </dd>
        </div>
      ))}
    </dl>
  )
}

type TabId = 'details' | 'more'

function InfoTabs({ card }: { card: CatalogCardDetails }) {
  const details = gameRows(card)
  const cardAttacks = attacks(card)
  const rules = rulesText(card)
  const source = sourceRows(card)
  const printings = card.otherPrintings

  const tabs: Array<{ id: TabId; label: string }> = []
  if (details.length || cardAttacks.length || rules)
    tabs.push({ id: 'details', label: 'Detalhes' })
  if (source.length || printings.length)
    tabs.push({ id: 'more', label: 'Mais informações' })

  const [selected, setSelected] = useState<TabId | null>(null)
  if (!tabs.length) return null
  const current = tabs.find((tab) => tab.id === selected) ?? tabs[0]

  return (
    <Card>
      {tabs.length > 1 ? (
        <div
          role="tablist"
          aria-label="Informações da carta"
          className="flex overflow-x-auto border-b border-neutral-200"
        >
          {tabs.map((tab) => (
            <button
              key={tab.id}
              type="button"
              role="tab"
              id={`tab-${tab.id}`}
              aria-selected={tab.id === current.id}
              aria-controls={`panel-${tab.id}`}
              onClick={() => setSelected(tab.id)}
              className={cn(
                'shrink-0 cursor-pointer border-b-2 px-4 py-3 text-sm',
                tab.id === current.id
                  ? 'border-primary-900 font-semibold text-primary-900'
                  : 'border-transparent text-neutral-700 hover:text-primary-900',
              )}
            >
              {tab.label}
            </button>
          ))}
        </div>
      ) : (
        <h2 className="border-b border-neutral-200 px-4 py-3 text-sm font-semibold text-primary-900">
          {current.label}
        </h2>
      )}

      <div
        role={tabs.length > 1 ? 'tabpanel' : undefined}
        id={`panel-${current.id}`}
        aria-labelledby={tabs.length > 1 ? `tab-${current.id}` : undefined}
        className="space-y-4 px-4 py-2"
      >
        {current.id === 'details' ? (
          <>
            {details.length ? <InfoList rows={details} /> : null}
            {cardAttacks.length ? (
              <div className="pb-2">
                <h3 className="mb-2 text-sm font-semibold text-neutral-900">
                  Ataques
                </h3>
                <ul className="space-y-2">
                  {cardAttacks.map((attack, index) => {
                    const cost = attackCost(attack)
                    return (
                      <li
                        key={`${attack.name}-${index}`}
                        className="flex items-start justify-between gap-4 rounded-control bg-neutral-50 px-3 py-2 text-sm"
                      >
                        <span>
                          <span className="font-semibold text-neutral-900">
                            {attack.name}
                          </span>
                          {cost ? (
                            <span className="block text-xs text-neutral-500">
                              Custo: {cost}
                            </span>
                          ) : null}
                        </span>
                        {attack.damage ? (
                          <span className="font-display font-bold text-primary-900">
                            {attack.damage}
                          </span>
                        ) : null}
                      </li>
                    )
                  })}
                </ul>
              </div>
            ) : null}
            {rules ? (
              <p className="pb-3 text-sm whitespace-pre-line text-neutral-700">
                {rules}
              </p>
            ) : null}
          </>
        ) : (
          <>
            {source.length ? <InfoList rows={source} /> : null}
            {printings.length ? (
              <div className="pb-3">
                <h3 className="mb-2 text-sm font-semibold text-neutral-900">
                  Também aparece em
                </h3>
                <ul className="space-y-1 text-sm">
                  {printings.map((printing) => (
                    <li key={printing.id}>
                      <Link
                        to="/cartas/$id"
                        params={{ id: printing.id }}
                        className="text-secondary-700 underline underline-offset-2 hover:text-primary-900"
                      >
                        {printing.setName ?? 'Coleção não informada'}
                        {printing.cardNumber
                          ? ` • Nº ${printing.cardNumber}`
                          : ''}
                      </Link>
                      {printing.rarity ? (
                        <span className="text-neutral-500">
                          {' '}
                          · {printing.rarity}
                        </span>
                      ) : null}
                    </li>
                  ))}
                </ul>
              </div>
            ) : null}
          </>
        )}
      </div>
    </Card>
  )
}

function DetailsSkeleton() {
  return (
    <div
      aria-busy="true"
      aria-live="polite"
      className="grid items-start gap-6 md:grid-cols-[260px_minmax(0,1fr)] xl:grid-cols-[300px_minmax(0,1fr)_300px]"
    >
      <span className="sr-only">Carregando carta...</span>
      <div className="mx-auto aspect-[63/88] w-full max-w-[300px] animate-pulse rounded-surface bg-neutral-200 md:row-span-2 xl:row-span-1" />
      <div className="space-y-3">
        <span className="block h-6 w-24 animate-pulse rounded-full bg-neutral-200" />
        <span className="block h-9 w-2/3 animate-pulse rounded bg-neutral-200" />
        <span className="block h-4 w-1/3 animate-pulse rounded bg-neutral-200" />
        <div className="flex gap-2">
          {[0, 1, 2].map((item) => (
            <span
              key={item}
              className="h-6 w-16 animate-pulse rounded-full bg-neutral-200"
            />
          ))}
        </div>
        <span className="block h-32 animate-pulse rounded-surface bg-neutral-100" />
      </div>
      <Card className="p-4 md:col-start-2 xl:col-start-3 xl:row-start-1">
        <AddButton disabled />
      </Card>
    </div>
  )
}

function DetailsStatus({
  kind,
  onRetry,
}: {
  kind: 'notFound' | 'error'
  onRetry?: () => void
}) {
  const router = useRouter()
  const navigate = useNavigate()
  const canGoBack = useCanGoBack()
  const notFound = kind === 'notFound'

  function backToCatalog() {
    if (canGoBack) router.history.back()
    else navigate({ to: '/' })
  }

  return (
    <div className="mx-auto max-w-xl space-y-4 py-10">
      <Alert
        variant={notFound ? 'neutral' : 'error'}
        title={
          notFound
            ? 'Carta não encontrada'
            : 'Não foi possível carregar a carta'
        }
      >
        {notFound
          ? 'Esta carta não existe ou foi removida do catálogo.'
          : 'Ocorreu um problema ao buscar as informações desta carta. Tente novamente.'}
      </Alert>
      <div className="flex flex-wrap justify-center gap-3">
        {notFound ? null : <Button onClick={onRetry}>Tentar novamente</Button>}
        <Button variant="outline" onClick={backToCatalog}>
          Voltar ao catálogo
        </Button>
      </div>
    </div>
  )
}
