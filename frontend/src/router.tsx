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
import { CatalogPage } from '@/pages/CatalogPage'
import { ProfileEditPage } from '@/pages/ProfileEditPage'
import { ProfilePage } from '@/pages/ProfilePage'
import { PublicProfilePage } from '@/pages/PublicProfilePage'
import { NotFound, RootLayout } from '@/routes/RootLayout'
import { parseAdminCatalogSearch } from '@/utils/adminCatalogSearch'
import { parseCatalogSearch } from '@/utils/catalogSearch'
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
