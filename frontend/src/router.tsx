import { RequireAuth } from '@/components/auth/RequireAuth'
import { SiteLayout } from '@/components/layout/SiteLayout'
import { AdminPage } from '@/pages/AdminPage'
import { DesignSystemPage } from '@/pages/DesignSystemPage'
import { HomePage } from '@/pages/HomePage'
import { LoginPage } from '@/pages/LoginPage'
import { RegisterPage } from '@/pages/RegisterPage'
import { CatalogPage } from '@/pages/CatalogPage'
import { NotFound, RootLayout } from '@/routes/RootLayout'
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
  getParentRoute: () => rootRoute,
  path: '/admin',
  component: () => (
    <RequireAuth role="ADMIN">
      <AdminPage />
    </RequireAuth>
  ),
})

const designSystemRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/design-system',
  component: DesignSystemPage,
})

const routeTree = rootRoute.addChildren([
  siteLayoutRoute.addChildren([indexRoute, catalogRoute]),
  registerRoute,
  loginRoute,
  adminRoute,
  designSystemRoute,
])

export const router = createRouter({ routeTree })

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router
  }
}
