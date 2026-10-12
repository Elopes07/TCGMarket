import {
  AboutCard,
  GamesCard,
  ProfileBreadcrumb,
  ProfileHero,
  ProfileShell,
  ProfileSkeleton,
  ProfileStatus,
} from '@/components/profile/ProfileView'
import type { ProfileViewData } from '@/components/profile/profileFormat'
import { useRemoteData } from '@/hooks/useRemoteData'
import { getPublicProfile } from '@/services/profileApi'
import { useParams } from '@tanstack/react-router'
import { useCallback } from 'react'

export function PublicProfilePage() {
  const { username } = useParams({ from: '/site-layout/jogadores/$username' })
  const fetchProfile = useCallback(() => getPublicProfile(username), [username])
  const { state, retry } = useRemoteData(username, fetchProfile)

  return (
    <ProfileShell>
      <ProfileBreadcrumb items={[{ label: `@${username}` }]} />

      {state.status === 'loading' ? <ProfileSkeleton /> : null}
      {state.status === 'notFound' ? <ProfileStatus kind="notFound" /> : null}
      {state.status === 'error' ? (
        <ProfileStatus kind="error" onRetry={retry} />
      ) : null}
      {state.status === 'ready' ? (
        <>
          <ProfileHero profile={state.data} />
          <PublicDetails profile={state.data} />
        </>
      ) : null}
    </ProfileShell>
  )
}

function PublicDetails({ profile }: { profile: ProfileViewData }) {
  const hasGames = profile.favoriteGames.length > 0
  return (
    <div
      className={
        hasGames
          ? 'grid gap-5 lg:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]'
          : 'max-w-xl'
      }
    >
      {hasGames ? <GamesCard profile={profile} title="TCGs que joga" /> : null}
      <AboutCard profile={profile} />
    </div>
  )
}
