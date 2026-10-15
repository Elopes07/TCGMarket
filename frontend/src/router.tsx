import { AdminLayout } from '@/components/admin/AdminLayout'
import { RequireAuth } from '@/components/auth/RequireAuth'
import { SiteLayout } from '@/components/layout/SiteLayout'
import { AdminCatalogPage } from '@/pages/admin/AdminCatalogPage'
import { AdminOverviewPage } from '@/pages/admin/AdminOverviewPage'
import { AdminUsersPage } from '@/pages/admin/AdminUsersPage'
import { DesignSystemPage } from '@/pages/DesignSystemPage'
import { HomePage } from '@/pages/HomePage'
import { LoginPage } from '@/pages/LoginPage'
import { RegisterPage } from '@/pages/RegisterPage'
import { CardDetailsPage } from '@/pages/CardDetailsPage'
import { CatalogPage } from '@/pages/CatalogPage'
import { ProfileEditPage } from '@/pages/ProfileEditPage'
import { ProfilePage } from '@/pages/ProfilePage'
import { PublicProfilePage } from '@/pages/PublicProfilePage'
import { NotFound, RootLayout } from '@/routes/RootLayout'
import { parseAdminCatalogSearch } from '@/utils/adminCatalogSearch'
import { parseCatalogSearch } from '@/utils/catalogSearch'
import { isInternalPath } from '@/utils/redirect'
import {
  createRootRoute,
  createRoute,
  createRouter,
} from '@tanstack/react-router'

const rootRoute = createRootRoute({
  component: RootLayout,
  notFoundComponent: NotFound,
})

const siteLayoutRoute = createRoute({
  getParentRoute: () => rootRoute,
  id: 'site-layout',
  component: SiteLayout,
})

const indexRoute = createRoute({
  getParentRoute: () => siteLayoutRoute,
  path: '/',
  component: HomePage,
})

const catalogRoute = createRoute({
  getParentRoute: () => siteLayoutRoute,
  path: '/catalogo/$game',
  validateSearch: parseCatalogSearch,
  component: CatalogPage,
})

const cardDetailsRoute = createRoute({
  getParentRoute: () => siteLayoutRoute,
  path: '/cartas/$id',
  validateSearch: (search: Record<string, unknown>): { adicionar?: true } =>
    search.adicionar === 1 ||
    search.adicionar === '1' ||
    search.adicionar === true
      ? { adicionar: true }
      : {},
  component: CardDetailsPage,
})

const profileRoute = createRoute({
  getParentRoute: () => siteLayoutRoute,
  path: '/perfil',
  component: () => (
    <RequireAuth>
      <ProfilePage />
    </RequireAuth>
  ),
})

const profileEditRoute = createRoute({
  getParentRoute: () => siteLayoutRoute,
  path: '/perfil/editar',
  component: () => (
    <RequireAuth>
      <ProfileEditPage />
    </RequireAuth>
  ),
})

const publicProfileRoute = createRoute({
  getParentRoute: () => siteLayoutRoute,
  path: '/jogadores/$username',
  component: PublicProfilePage,
})

const registerRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/cadastro',
  component: RegisterPage,
})

const loginRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/login',
  validateSearch: (search: Record<string, unknown>): { redirect?: string } =>
    typeof search.redirect === 'string' && isInternalPath(search.redirect)
      ? { redirect: search.redirect }
      : {},
  component: LoginPage,
})

const adminRoute = createRoute({
  getParentRoute: () => siteLayoutRoute,
  path: '/admin',
  component: () => (
    <RequireAuth role="ADMIN">
      <AdminLayout />
    </RequireAuth>
  ),
})

const adminOverviewRoute = createRoute({
  getParentRoute: () => adminRoute,
  path: '/',
  component: AdminOverviewPage,
})

const adminUsersRoute = createRoute({
  getParentRoute: () => adminRoute,
  path: 'usuarios',
  component: AdminUsersPage,
})

const adminCatalogRoute = createRoute({
  getParentRoute: () => adminRoute,
  path: 'catalogo',
  validateSearch: parseAdminCatalogSearch,
  component: AdminCatalogPage,
})

const designSystemRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/design-system',
  component: DesignSystemPage,
})

const routeTree = rootRoute.addChildren([
  siteLayoutRoute.addChildren([
    indexRoute,
    catalogRoute,
    cardDetailsRoute,
    profileRoute,
    profileEditRoute,
    publicProfileRoute,
    adminRoute.addChildren([
      adminOverviewRoute,
      adminUsersRoute,
      adminCatalogRoute,
    ]),
  ]),
  registerRoute,
  loginRoute,
  designSystemRoute,
])

export const router = createRouter({ routeTree })

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router
  }
}

declare module '@tanstack/history' {
  interface HistoryState {
    profileSaved?: boolean
  }
}
