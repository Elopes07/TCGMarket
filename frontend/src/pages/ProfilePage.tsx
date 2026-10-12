import {
  AboutCard,
  FeaturedCollectionCard,
  GamesCard,
  PencilIcon,
  ProfileBreadcrumb,
  ProfileHero,
  ProfileShell,
  ProfileSkeleton,
  ProfileStatus,
} from '@/components/profile/ProfileView'
import { toViewData } from '@/components/profile/profileFormat'
import { Alert } from '@/components/ui/Alert'
import { useRemoteData } from '@/hooks/useRemoteData'
import { getOwnProfile } from '@/services/profileApi'
import { useSession } from '@/session'
import { Link, useNavigate, useRouterState } from '@tanstack/react-router'

export function ProfilePage() {
  const navigate = useNavigate()
  const { user } = useSession()
  const saved = useRouterState({
    select: (state) => state.location.state.profileSaved === true,
  })
  const { state, retry } = useRemoteData(`me:${user?.id}`, getOwnProfile)

  function dismissSaved() {
    void navigate({ to: '/perfil', replace: true, state: {} })
  }

  return (
    <ProfileShell>
      <ProfileBreadcrumb items={[{ label: 'Meu perfil' }]} />

      {saved ? (
        <Alert variant="success" onDismiss={dismissSaved}>
          Perfil atualizado com sucesso.
        </Alert>
      ) : null}

      {state.status === 'loading' ? <ProfileSkeleton /> : null}
      {state.status === 'notFound' || state.status === 'error' ? (
        <ProfileStatus kind="error" onRetry={retry} />
      ) : null}
      {state.status === 'ready' ? (
        <>
          <ProfileHero
            profile={toViewData(state.data)}
            action={<EditProfileLink label="Editar perfil" />}
          />
          <div className="grid gap-5 lg:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
            <div className="space-y-5">
              <GamesCard
                profile={toViewData(state.data)}
                title="TCGs que coleciono"
                editAction={<EditProfileLink label="Editar" compact />}
              />
              <FeaturedCollectionCard />
            </div>
            <AboutCard
              profile={toViewData(state.data)}
              email={state.data.email}
              emptyBioAction={<EditProfileLink label="Escrever bio" compact />}
            />
          </div>
          <p className="text-sm text-neutral-500">
            Seu perfil público:{' '}
            <Link
              to="/jogadores/$username"
              params={{ username: state.data.username }}
              className="font-semibold text-primary-900 underline-offset-2 hover:underline"
            >
              /jogadores/{state.data.username}
            </Link>
          </p>
        </>
      ) : null}
    </ProfileShell>
  )
}

function EditProfileLink({
  label,
  compact = false,
}: {
  label: string
  compact?: boolean
}) {
  return (
    <Link
      to="/perfil/editar"
      className={
        compact
          ? 'inline-flex items-center gap-1 text-sm font-semibold text-primary-900 hover:text-primary-800'
          : 'inline-flex h-10 items-center gap-2 rounded-control border border-primary-900 bg-white px-4 text-sm font-semibold text-primary-900 hover:bg-primary-100'
      }
    >
      <PencilIcon />
      {label}
    </Link>
  )
}
