import { Badge } from '@/components/ui/Badge'
import { Icon, type IconName } from '@/components/ui/Icon'
import { ProfileBreadcrumb } from '@/components/profile/ProfileView'
import { Link, Outlet } from '@tanstack/react-router'

const SECTIONS: {
  to: '/admin' | '/admin/usuarios' | '/admin/catalogo'
  label: string
  icon: IconName
}[] = [
  { to: '/admin', label: 'Visão geral', icon: 'grid' },
  { to: '/admin/usuarios', label: 'Usuários', icon: 'user' },
  { to: '/admin/catalogo', label: 'Catálogo', icon: 'search' },
]

const UPCOMING = [
  'Solicitações de carta',
  'Anúncios',
  'Denúncias',
  'Transações',
  'Relatórios',
]

export function AdminLayout() {
  return (
    <main className="mx-auto w-full max-w-6xl space-y-5 px-4 py-6 sm:px-6">
      <ProfileBreadcrumb items={[{ label: 'Área administrativa' }]} />

      <header className="flex items-center gap-4 rounded-surface border border-neutral-200 bg-gradient-to-r from-white via-primary-100/40 to-primary-100 px-6 py-5">
        <span
          aria-hidden="true"
          className="flex size-14 shrink-0 items-center justify-center rounded-surface bg-primary-100 text-primary-900"
        >
          <Icon name="settings" size={32} />
        </span>
        <div>
          <h1 className="font-display text-2xl font-bold text-primary-900 sm:text-3xl">
            Área Administrativa
          </h1>
          <p className="mt-1 text-sm text-neutral-700">
            Gerencie usuários, o catálogo e o funcionamento do TCG Market.
          </p>
        </div>
      </header>

      <div className="grid items-start gap-5 lg:grid-cols-[220px_minmax(0,1fr)]">
        <nav
          aria-label="Seções da área administrativa"
          className="rounded-surface border border-neutral-200 bg-white p-2"
        >
          <ul className="flex gap-1 overflow-x-auto lg:flex-col">
            {SECTIONS.map((section) => (
              <li key={section.to} className="shrink-0">
                <Link
                  to={section.to}
                  activeOptions={{ exact: true, includeSearch: false }}
                  className="flex items-center gap-3 rounded-control px-3 py-2 text-sm font-medium text-neutral-700 hover:bg-primary-100 hover:text-primary-900"
                  activeProps={{
                    className: 'bg-primary-100 font-semibold text-primary-900',
                    'aria-current': 'page',
                  }}
                >
                  <Icon name={section.icon} size={20} />
                  {section.label}
                </Link>
              </li>
            ))}
          </ul>
          <ul className="mt-2 hidden border-t border-neutral-200 pt-2 lg:block">
            {UPCOMING.map((label) => (
              <li
                key={label}
                aria-disabled="true"
                className="flex items-center justify-between gap-2 px-3 py-2 text-sm text-neutral-500"
              >
                {label}
                <Badge variant="neutral">Em breve</Badge>
              </li>
            ))}
          </ul>
        </nav>

        <section className="min-w-0">
          <Outlet />
        </section>
      </div>
    </main>
  )
}
