import { GAME_LABEL } from '@/components/admin/adminFormat'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { useRemoteData } from '@/hooks/useRemoteData'
import { fetchAdminCatalogSummary } from '@/services/adminApi'
import type { CardGame } from '@/types/AdminCatalog'
import { Link } from '@tanstack/react-router'

const GAME_BAR: Record<CardGame, string> = {
  POKEMON: 'bg-amber-500',
  YUGIOH: 'bg-primary-700',
  MAGIC_THE_GATHERING: 'bg-slate-700',
}

const numberFormat = new Intl.NumberFormat('pt-BR')
const percentFormat = new Intl.NumberFormat('pt-BR', {
  style: 'percent',
  maximumFractionDigits: 1,
})

export function AdminOverviewPage() {
  const { state, retry } = useRemoteData(
    'admin-catalog-summary',
    fetchAdminCatalogSummary,
  )

  return (
    <Card>
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-neutral-200 px-5 py-4">
        <h2 className="text-lg font-semibold text-neutral-900">
          Catálogo por TCG
        </h2>
        <Link
          to="/admin/catalogo"
          className="text-sm font-semibold text-primary-900 hover:text-primary-800"
        >
          Ver catálogo
        </Link>
      </div>

      {state.status === 'loading' ? (
        <div
          role="status"
          aria-label="Carregando indicadores do catálogo"
          className="grid animate-pulse gap-4 p-5 sm:grid-cols-2"
        >
          <div className="h-20 rounded-control bg-neutral-100" />
          <div className="h-20 rounded-control bg-neutral-100" />
          <div className="h-24 rounded-control bg-neutral-100 sm:col-span-2" />
        </div>
      ) : state.status !== 'ready' ? (
        <div role="alert" className="space-y-3 px-5 py-6 text-sm">
          <p className="text-neutral-700">
            Os indicadores do catálogo estão indisponíveis no momento.
          </p>
          <Button variant="outline" onClick={retry}>
            Tentar novamente
          </Button>
        </div>
      ) : (
        <div className="space-y-6 p-5">
          <dl className="grid gap-4 sm:grid-cols-2">
            <Indicator
              label="Cartas no catálogo"
              value={state.data.totalCards}
            />
            <Indicator
              label="Cartas ocultas do catálogo público"
              value={state.data.hiddenCards}
            />
          </dl>

          {state.data.totalCards === 0 ? (
            <p className="text-sm text-neutral-500">
              Nenhuma carta importada ainda.
            </p>
          ) : (
            <ul
              aria-label="Distribuição de cartas por TCG"
              className="space-y-4"
            >
              {state.data.games.map((item) => {
                const share = item.totalCards / state.data.totalCards
                return (
                  <li key={item.game}>
                    <div className="flex flex-wrap items-baseline justify-between gap-2 text-sm">
                      <span className="font-semibold text-neutral-900">
                        {GAME_LABEL[item.game]}
                      </span>
                      <span className="text-neutral-700">
                        {numberFormat.format(item.totalCards)} cartas ·{' '}
                        {percentFormat.format(share)}
                        {item.hiddenCards > 0
                          ? ` · ${numberFormat.format(item.hiddenCards)} ${item.hiddenCards === 1 ? 'oculta' : 'ocultas'}`
                          : ''}
                      </span>
                    </div>
                    <div
                      aria-hidden="true"
                      className="mt-2 h-2.5 overflow-hidden rounded-full bg-neutral-100"
                    >
                      <div
                        className={`h-full rounded-full ${GAME_BAR[item.game]}`}
                        style={{ width: `${share * 100}%` }}
                      />
                    </div>
                  </li>
                )
              })}
            </ul>
          )}
        </div>
      )}
    </Card>
  )
}

function Indicator({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-control border border-neutral-200 px-4 py-3">
      <dt className="text-xs font-medium text-neutral-500">{label}</dt>
      <dd className="font-display mt-1 text-2xl font-bold text-primary-900">
        {numberFormat.format(value)}
      </dd>
    </div>
  )
}
