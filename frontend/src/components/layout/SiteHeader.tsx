import mark from '@/assets/mark-3cartas-branco.svg'
import { hasRole, useSession } from '@/session'
import { useNavigate } from '@tanstack/react-router'
import { type FormEvent, useState } from 'react'

type Props = { onSoon: (feature: string) => void }

export function SiteHeader({ onSoon: soon }: Props) {
  const navigate = useNavigate()
  const { user, sessionReady, logout } = useSession()
  const [query, setQuery] = useState('')
  const [menuOpen, setMenuOpen] = useState(false)
  const firstName = user?.name.split(' ')[0]

  function search(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (query.trim()) soon('A busca')
  }

  return (
    <header className="sticky top-0 z-20 bg-gradient-to-r from-[#47104f] via-primary-900 to-[#310b43] text-white shadow-md">
      <div className="mx-auto flex min-h-16 w-full max-w-[1600px] items-center gap-3 px-4 py-2 sm:gap-5 sm:px-6 lg:px-8">
        <button
          type="button"
          onClick={() => navigate({ to: '/' })}
          className="flex shrink-0 items-center gap-2 rounded-lg focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-secondary-500"
          aria-label="Ir para a página inicial do TCG Market"
        >
          <img src={mark} alt="" className="h-9 w-auto sm:h-10" />
          <span className="font-display hidden whitespace-nowrap text-base tracking-wide sm:inline lg:text-xl">
            TCG MARKET
          </span>
        </button>

        <form
          onSubmit={search}
          role="search"
          className="mx-auto min-w-0 max-w-[540px] flex-1 lg:ml-10"
        >
          <label className="relative block">
            <span className="sr-only">Buscar carta, anúncio ou usuário</span>
            <SearchIcon />
            <input
              type="search"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Buscar carta, anúncio ou usuário..."
              className="h-10 w-full rounded-xl border border-white/80 bg-white py-2 pr-3 pl-10 text-sm text-neutral-900 shadow-sm outline-none placeholder:text-neutral-500 focus-visible:ring-2 focus-visible:ring-secondary-500 sm:h-11"
            />
          </label>
        </form>

        <div className="flex shrink-0 items-center gap-1 sm:gap-2">
          <button
            type="button"
            onClick={() => soon('O carrinho de compras')}
            className="rounded-full p-2 transition hover:bg-white/15 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-secondary-500"
            aria-label="Carrinho de compras"
            title="Carrinho de compras"
          >
            <CartIcon />
          </button>

          <button
            type="button"
            onClick={() => soon('As notificações')}
            className="rounded-full p-2 transition hover:bg-white/15 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-secondary-500"
            aria-label="Notificações"
            title="Notificações"
          >
            <BellIcon />
          </button>

          {sessionReady && user ? (
            <div className="relative border-l border-white/25 pl-2 sm:pl-3">
              <button
                type="button"
                onClick={() => setMenuOpen((open) => !open)}
                className="flex items-center gap-2 rounded-full py-1 pr-1 pl-1 transition hover:bg-white/10"
                aria-expanded={menuOpen}
                aria-label="Abrir menu do perfil"
              >
                <span className="flex size-9 items-center justify-center rounded-full bg-white text-sm font-bold text-primary-900">
                  {firstName?.[0] ?? 'U'}
                </span>
                <span className="hidden max-w-28 truncate text-sm font-medium lg:inline">
                  {firstName}
                </span>
                <ChevronDownIcon />
              </button>
              {menuOpen && (
                <div className="absolute right-0 mt-2 w-56 rounded-xl border border-neutral-200 bg-white p-3 text-neutral-900 shadow-xl">
                  <p className="px-2 text-sm font-semibold">{user.name}</p>
                  <p className="truncate px-2 pb-2 text-xs text-neutral-500">
                    {user.email}
                  </p>
                  <button
                    type="button"
                    onClick={() => {
                      setMenuOpen(false)
                      navigate({ to: '/perfil' })
                    }}
                    className="w-full rounded-lg px-2 py-2 text-left text-sm font-medium hover:bg-primary-100"
                  >
                    Meu perfil
                  </button>
                  {hasRole(user, 'ADMIN') && (
                    <button
                      type="button"
                      onClick={() => {
                        setMenuOpen(false)
                        navigate({ to: '/admin' })
                      }}
                      className="w-full rounded-lg px-2 py-2 text-left text-sm font-medium hover:bg-primary-100"
                    >
                      Área Administrativa
                    </button>
                  )}
                  <button
                    type="button"
                    onClick={() => {
                      setMenuOpen(false)
                      logout()
                    }}
                    className="w-full rounded-lg px-2 py-2 text-left text-sm font-medium text-primary-900 hover:bg-primary-100"
                  >
                    Sair
                  </button>
                </div>
              )}
            </div>
          ) : (
            <div className="flex items-center gap-2 border-l border-white/25 pl-2 sm:pl-3">
              <button
                type="button"
                onClick={() => navigate({ to: '/cadastro' })}
                disabled={!sessionReady}
                className="hidden text-sm font-semibold hover:text-white/80 disabled:opacity-60 lg:inline"
              >
                Criar conta
              </button>
              <button
                type="button"
                onClick={() => navigate({ to: '/login' })}
                disabled={!sessionReady}
                className="rounded-full bg-white px-3 py-2 text-xs font-semibold text-primary-900 transition hover:bg-primary-100 disabled:opacity-60 sm:px-4 sm:text-sm"
              >
                {sessionReady ? 'Entrar' : '...'}
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  )
}

function SearchIcon() {
  return (
    <svg
      viewBox="0 0 24 24"
      className="pointer-events-none absolute top-1/2 left-3 size-5 -translate-y-1/2 text-primary-500"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      aria-hidden="true"
    >
      <circle cx="10.8" cy="10.8" r="6.5" />
      <path d="m16 16 4.5 4.5" />
    </svg>
  )
}

function CartIcon() {
  return (
    <svg
      viewBox="0 0 24 24"
      className="size-5 sm:size-6"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <circle cx="9" cy="21" r="1" />
      <circle cx="20" cy="21" r="1" />
      <path d="M1 2h3l2.4 12.1a2 2 0 0 0 2 1.6h10a2 2 0 0 0 2-1.6L22 6H5" />
    </svg>
  )
}

function BellIcon() {
  return (
    <svg
      viewBox="0 0 24 24"
      className="size-5 sm:size-6"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M6 16v-5a6 6 0 1 1 12 0v5l1.5 2h-15L6 16Z" />
      <path d="M10 19a2 2 0 0 0 4 0" />
    </svg>
  )
}

function ChevronDownIcon() {
  return (
    <svg
      viewBox="0 0 24 24"
      className="hidden size-4 text-white/80 sm:block"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="m6 9 6 6 6-6" />
    </svg>
  )
}
