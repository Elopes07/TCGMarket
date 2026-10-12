import type { IconName } from '@/components/ui/Icon'
import type {
  CardGame,
  ProfileResponse,
  PublicProfileResponse,
} from '@/types/User'

export type ProfileViewData = PublicProfileResponse

export const PROFILE_GAMES = [
  { value: 'POKEMON', label: 'Pokémon', icon: 'crest-pokemon' },
  { value: 'MAGIC_THE_GATHERING', label: 'Magic', icon: 'crest-magic' },
  { value: 'YUGIOH', label: 'Yu-Gi-Oh!', icon: 'crest-yugioh' },
] as const satisfies ReadonlyArray<{
  value: CardGame
  label: string
  icon: IconName
}>

export const BRAZIL_STATES = [
  'AC',
  'AL',
  'AP',
  'AM',
  'BA',
  'CE',
  'DF',
  'ES',
  'GO',
  'MA',
  'MT',
  'MS',
  'MG',
  'PA',
  'PB',
  'PR',
  'PE',
  'PI',
  'RJ',
  'RN',
  'RS',
  'RO',
  'RR',
  'SC',
  'SP',
  'SE',
  'TO',
] as const

const memberSince = new Intl.DateTimeFormat('pt-BR', {
  month: 'short',
  year: 'numeric',
})

export function toViewData(profile: ProfileResponse): ProfileViewData {
  return {
    name: profile.name,
    username: profile.username,
    administrator: profile.role === 'ADMIN',
    registrationDate: profile.registrationDate,
    bio: profile.bio,
    city: profile.city,
    state: profile.state,
    favoriteGames: profile.favoriteGames,
    links: profile.links,
  }
}

export function initials(name: string) {
  const words = name.trim().split(/\s+/).filter(Boolean)
  const first = words[0]?.[0] ?? ''
  const last = words.length > 1 ? (words[words.length - 1]?.[0] ?? '') : ''
  return (first + last).toUpperCase() || '?'
}

export function formatMemberSince(date: string) {
  return `Membro desde ${memberSince.format(new Date(date))}`
}

export function formatLocation(city: string | null, state: string | null) {
  return [city, state].filter(Boolean).join(', ')
}

export function linkLabel(url: string) {
  return url.replace(/^https:\/\//, '').replace(/\/$/, '')
}

export function gameLabel(game: CardGame) {
  return PROFILE_GAMES.find((item) => item.value === game)?.label ?? game
}
