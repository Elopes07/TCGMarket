import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { cn } from '@/components/ui/cn'
import { Icon } from '@/components/ui/Icon'
import { Link } from '@tanstack/react-router'
import { type ReactNode } from 'react'
import {
  formatLocation,
  formatMemberSince,
  initials,
  linkLabel,
  PROFILE_GAMES,
  type ProfileViewData,
} from './profileFormat'

export function ProfileShell({ children }: { children: ReactNode }) {
  return (
    <main className="mx-auto w-full max-w-6xl space-y-5 px-4 py-6 sm:px-6">
      {children}
    </main>
  )
}

export function ProfileBreadcrumb({
  items,
}: {
  items: Array<{ label: string; to?: '/perfil' }>
}) {
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
        {items.map((item) => (
          <li key={item.label} className="flex items-center gap-1">
            <span aria-hidden="true">›</span>
            {item.to ? (
              <Link to={item.to} className="hover:text-primary-900">
                {item.label}
              </Link>
            ) : (
              <span aria-current="page">{item.label}</span>
            )}
          </li>
        ))}
      </ol>
    </nav>
  )
}

export function Avatar({
  name,
  size = 'lg',
}: {
  name: string
  size?: 'md' | 'lg'
}) {
  return (
    <span
      aria-hidden="true"
      className={cn(
        'font-display flex shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-primary-300 to-primary-700 font-bold text-white ring-4 ring-white shadow-md',
        size === 'lg' ? 'size-24 text-3xl sm:size-28' : 'size-20 text-2xl',
      )}
    >
      {initials(name)}
    </span>
  )
}

export function ProfileHero({
  profile,
  action,
}: {
  profile: ProfileViewData
  action?: ReactNode
}) {
  const location = formatLocation(profile.city, profile.state)
  const games = PROFILE_GAMES.filter((game) =>
    profile.favoriteGames.includes(game.value),
  )

  return (
    <Card className="relative bg-gradient-to-r from-white via-white to-primary-100">
      <div className="relative flex flex-col gap-5 p-5 sm:flex-row sm:items-center sm:p-7">
        <Avatar name={profile.name} />
        <div className="min-w-0 flex-1 space-y-2">
          <div className="flex flex-wrap items-center gap-2">
            <h1 className="font-display text-2xl font-bold break-words text-primary-900 sm:text-3xl">
              {profile.name}
            </h1>
            {profile.administrator ? (
              <Badge variant="primary">Administrador</Badge>
            ) : null}
          </div>
          <ul className="flex flex-wrap gap-x-4 gap-y-1 text-sm text-neutral-700">
            <li>@{profile.username}</li>
            <li className="flex items-center gap-1">
              <CalendarIcon />
              {formatMemberSince(profile.registrationDate)}
            </li>
            {location ? (
              <li className="flex items-center gap-1">
                <PinIcon />
                {location}
              </li>
            ) : null}
          </ul>
          {profile.bio ? (
            <p className="line-clamp-2 max-w-2xl text-sm text-neutral-700">
              {profile.bio}
            </p>
          ) : null}
          {games.length > 0 ? (
            <ul className="flex flex-wrap gap-2" aria-label="TCGs que joga">
              {games.map((game) => (
                <li key={game.value}>
                  <Badge
                    variant="neutral"
                    className="border border-primary-300 bg-white text-primary-900"
                  >
                    {game.label}
                  </Badge>
                </li>
              ))}
            </ul>
          ) : null}
        </div>
        {action ? <div className="sm:self-start">{action}</div> : null}
      </div>
    </Card>
  )
}

export function GamesCard({
  profile,
  title,
  editAction,
}: {
  profile: ProfileViewData
  title: string
  editAction?: ReactNode
}) {
  if (!editAction && profile.favoriteGames.length === 0) return null

  return (
    <Card className="p-5">
      <div className="flex items-center justify-between gap-3">
        <h2 className="text-base font-semibold text-neutral-900">{title}</h2>
        {editAction}
      </div>
      <ul className="mt-4 grid gap-3 sm:grid-cols-3">
        {PROFILE_GAMES.map((game) => {
          const selected = profile.favoriteGames.includes(game.value)
          return (
            <li
              key={game.value}
              className={cn(
                'flex flex-col items-center gap-2 rounded-surface border px-3 py-4 text-sm font-semibold',
                selected
                  ? 'border-primary-900 bg-primary-900 text-white'
                  : 'border-neutral-200 bg-neutral-50 text-neutral-500',
              )}
            >
              <Icon name={game.icon} size={32} />
              {game.label}
              <span className="sr-only">
                {selected ? '(joga)' : '(não joga)'}
              </span>
            </li>
          )
        })}
      </ul>
    </Card>
  )
}

export function FeaturedCollectionCard() {
  return (
    <Card className="p-5">
      <h2 className="text-base font-semibold text-neutral-900">
        Minha coleção em destaque
      </h2>
      <div className="mt-4 flex flex-col items-center gap-2 rounded-surface border border-dashed border-primary-300 bg-primary-100/40 px-4 py-8 text-center">
        <div className="flex gap-2" aria-hidden="true">
          {[0, 1, 2, 3].map((item) => (
            <span
              key={item}
              className="h-12 w-9 rounded-md bg-gradient-to-b from-primary-300 to-primary-100"
            />
          ))}
        </div>
        <p className="text-sm font-semibold text-primary-900">Em breve</p>
        <p className="text-sm text-neutral-700">
          As cartas em destaque aparecem aqui quando a Minha Coleção estiver
          disponível.
        </p>
      </div>
    </Card>
  )
}

export function AboutCard({
  profile,
  email,
  emptyBioAction,
}: {
  profile: ProfileViewData
  email?: string
  emptyBioAction?: ReactNode
}) {
  const location = formatLocation(profile.city, profile.state)

  return (
    <Card className="p-5">
      <h2 className="text-base font-semibold text-neutral-900">Sobre mim</h2>
      {profile.bio ? (
        <p className="mt-3 text-sm break-words whitespace-pre-line text-neutral-700">
          {profile.bio}
        </p>
      ) : emptyBioAction ? (
        <div className="mt-3 rounded-control bg-neutral-100 p-3 text-sm text-neutral-700">
          <p>Conte um pouco sobre você para outros jogadores.</p>
          <div className="mt-2">{emptyBioAction}</div>
        </div>
      ) : null}
      <ul className="mt-4 space-y-2 text-sm text-neutral-700">
        {location ? (
          <li className="flex items-center gap-2">
            <PinIcon />
            {location}
          </li>
        ) : null}
        <li className="flex items-center gap-2">
          <CalendarIcon />
          {formatMemberSince(profile.registrationDate)}
        </li>
        {profile.links.map((url) => (
          <li key={url} className="flex min-w-0 items-center gap-2">
            <LinkIcon />
            <a
              href={url}
              target="_blank"
              rel="noopener noreferrer nofollow ugc"
              className="truncate text-primary-900 underline underline-offset-2 hover:text-primary-800"
            >
              {linkLabel(url)}
            </a>
          </li>
        ))}
        {email ? (
          <li className="flex min-w-0 items-center gap-2">
            <LockIcon />
            <span className="truncate">{email}</span>
            <span className="shrink-0 text-xs text-neutral-500">
              (visível só para você)
            </span>
          </li>
        ) : null}
      </ul>
    </Card>
  )
}

export function ProfileSkeleton() {
  return (
    <div aria-busy="true" aria-live="polite" className="space-y-5">
      <span className="sr-only">Carregando perfil...</span>
      <Card className="flex items-center gap-5 p-7">
        <span className="size-24 animate-pulse rounded-full bg-neutral-200 sm:size-28" />
        <div className="flex-1 space-y-3">
          <span className="block h-7 w-1/3 animate-pulse rounded bg-neutral-200" />
          <span className="block h-4 w-1/2 animate-pulse rounded bg-neutral-200" />
          <span className="block h-4 w-2/3 animate-pulse rounded bg-neutral-200" />
        </div>
      </Card>
      <div className="grid gap-5 lg:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
        <div className="h-40 animate-pulse rounded-surface border border-neutral-200 bg-neutral-100" />
        <div className="h-40 animate-pulse rounded-surface border border-neutral-200 bg-neutral-100" />
      </div>
    </div>
  )
}

export function ProfileStatus({
  kind,
  onRetry,
}: {
  kind: 'notFound' | 'error'
  onRetry?: () => void
}) {
  const notFound = kind === 'notFound'
  return (
    <Card className="flex flex-col items-center gap-3 px-6 py-14 text-center">
      <span className="flex size-14 items-center justify-center rounded-full bg-primary-100 text-primary-900">
        <Icon name={notFound ? 'user' : 'alert'} size={24} />
      </span>
      <h1 className="font-display text-2xl font-bold text-primary-900">
        {notFound ? 'Perfil não encontrado' : 'Não foi possível carregar'}
      </h1>
      <p className="max-w-md text-sm text-neutral-700">
        {notFound
          ? 'Confira o nome de usuário no endereço ou volte para o início.'
          : 'Tivemos um problema ao buscar este perfil. Tente novamente em instantes.'}
      </p>
      {notFound ? (
        <Link
          to="/"
          className="mt-2 inline-flex h-10 items-center rounded-control bg-primary-900 px-4 text-sm font-semibold text-white hover:bg-primary-800"
        >
          Voltar para o início
        </Link>
      ) : (
        <Button className="mt-2" onClick={onRetry}>
          Tentar novamente
        </Button>
      )}
    </Card>
  )
}

function SvgIcon({ children }: { children: ReactNode }) {
  return (
    <svg
      viewBox="0 0 24 24"
      className="size-4 shrink-0 text-neutral-500"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {children}
    </svg>
  )
}

function CalendarIcon() {
  return (
    <SvgIcon>
      <rect x="4" y="5" width="16" height="15" rx="2" />
      <path d="M8 3v4M16 3v4M4 10h16" />
    </SvgIcon>
  )
}

function PinIcon() {
  return (
    <SvgIcon>
      <path d="M12 21s-6-5.4-6-11a6 6 0 0 1 12 0c0 5.6-6 11-6 11Z" />
      <circle cx="12" cy="10" r="2.2" />
    </SvgIcon>
  )
}

function LinkIcon() {
  return (
    <SvgIcon>
      <path d="M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1" />
      <path d="M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1" />
    </SvgIcon>
  )
}

function LockIcon() {
  return (
    <SvgIcon>
      <rect x="5" y="11" width="14" height="10" rx="2" />
      <path d="M8 11V8a4 4 0 0 1 8 0v3" />
    </SvgIcon>
  )
}

export function PencilIcon() {
  return (
    <svg
      viewBox="0 0 24 24"
      className="size-4"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M4 20h4L19 9l-4-4L4 16v4Z" />
      <path d="m13.5 6.5 4 4" />
    </svg>
  )
}
