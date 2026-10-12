import {
  Avatar,
  ProfileBreadcrumb,
  ProfileShell,
  ProfileSkeleton,
  ProfileStatus,
} from '@/components/profile/ProfileView'
import {
  BRAZIL_STATES,
  PROFILE_GAMES,
} from '@/components/profile/profileFormat'
import { Alert } from '@/components/ui/Alert'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { cn } from '@/components/ui/cn'
import { Input } from '@/components/ui/Input'
import { Modal } from '@/components/ui/Modal'
import { Select } from '@/components/ui/Select'
import { useRemoteData } from '@/hooks/useRemoteData'
import { getOwnProfile, updateOwnProfile } from '@/services/profileApi'
import { useSession } from '@/session'
import type {
  ApiErrorResponse,
  CardGame,
  ProfileResponse,
  UpdateProfileRequest,
} from '@/types/User'
import { useNavigate } from '@tanstack/react-router'
import axios from 'axios'
import { type FormEvent, useId, useState } from 'react'

const MAX_LINKS = 3
const BIO_MAX = 280
const NAME_MAX = 120
const CITY_MAX = 60
const USERNAME_PATTERN = /^[a-z0-9_.]{3,20}$/
const LINK_PATTERN = /^https:\/\/[^\s/?#]+\.[^\s/?#]+(?:[/?#]\S*)?$/

const STATE_OPTIONS = [
  { value: '', label: 'Não informar' },
  ...BRAZIL_STATES.map((uf) => ({ value: uf, label: uf })),
]

type FormState = {
  name: string
  username: string
  bio: string
  city: string
  state: string
  favoriteGames: CardGame[]
  links: string[]
}

type FieldErrors = Record<string, string>

export function ProfileEditPage() {
  const { user } = useSession()
  const { state, retry } = useRemoteData(`me:${user?.id}`, getOwnProfile)

  return (
    <ProfileShell>
      <ProfileBreadcrumb
        items={[
          { label: 'Meu perfil', to: '/perfil' },
          { label: 'Editar perfil' },
        ]}
      />
      {state.status === 'loading' ? <ProfileSkeleton /> : null}
      {state.status === 'notFound' || state.status === 'error' ? (
        <ProfileStatus kind="error" onRetry={retry} />
      ) : null}
      {state.status === 'ready' ? <ProfileForm profile={state.data} /> : null}
    </ProfileShell>
  )
}

function ProfileForm({ profile }: { profile: ProfileResponse }) {
  const navigate = useNavigate()
  const { refreshUser } = useSession()
  const bioId = useId()
  const [initial] = useState(() => toFormState(profile))
  const [form, setForm] = useState(initial)
  const [errors, setErrors] = useState<FieldErrors>({})
  const [feedback, setFeedback] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [confirmCancel, setConfirmCancel] = useState(false)
  const dirty = JSON.stringify(form) !== JSON.stringify(initial)

  function update<K extends keyof FormState>(field: K, value: FormState[K]) {
    setForm((current) => ({ ...current, [field]: value }))
    setErrors((current) => withoutKey(current, field))
  }

  function updateLink(index: number, value: string) {
    setForm((current) => ({
      ...current,
      links: current.links.map((link, i) => (i === index ? value : link)),
    }))
    setErrors((current) =>
      withoutKey(withoutKey(current, `links[${index}]`), 'links'),
    )
  }

  function toggleGame(game: CardGame) {
    update(
      'favoriteGames',
      form.favoriteGames.includes(game)
        ? form.favoriteGames.filter((item) => item !== game)
        : [...form.favoriteGames, game],
    )
  }

  function checkField(field: string) {
    const message = validate(form)[field]
    setErrors((current) =>
      message ? { ...current, [field]: message } : withoutKey(current, field),
    )
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!dirty || saving) return
    setFeedback(null)

    const nextErrors = validate(form)
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) return

    const { request, linkSlots } = toRequest(form)
    setSaving(true)
    try {
      await updateOwnProfile(request)
      await refreshUser()
      void navigate({ to: '/perfil', state: { profileSaved: true } })
    } catch (cause) {
      const apiErrors = fieldErrorsFrom(cause, linkSlots)
      setErrors(apiErrors)
      if (!Object.keys(apiErrors).some(isVisibleField)) {
        setFeedback(
          apiMessage(cause) ??
            'Não foi possível salvar o perfil. Tente novamente.',
        )
      }
      setSaving(false)
    }
  }

  function cancel() {
    if (dirty) {
      setConfirmCancel(true)
      return
    }
    void navigate({ to: '/perfil' })
  }

  return (
    <Card className="mx-auto max-w-3xl p-5 sm:p-7">
      <h1 className="font-display text-2xl font-bold text-primary-900">
        Editar perfil
      </h1>
      <p className="mt-1 text-sm text-neutral-700">
        Essas informações aparecem no seu perfil público. Seu e-mail nunca é
        exibido para outros jogadores.
      </p>

      <form className="mt-6 space-y-6" onSubmit={handleSubmit} noValidate>
        {feedback ? (
          <Alert variant="error" onDismiss={() => setFeedback(null)}>
            {feedback}
          </Alert>
        ) : null}

        <section aria-labelledby="photo-title">
          <h2
            id="photo-title"
            className="mb-2 text-sm font-medium text-neutral-900"
          >
            Foto do perfil
          </h2>
          <div className="flex flex-wrap items-center gap-4">
            <Avatar name={form.name || profile.name} size="md" />
            <div className="space-y-1">
              <div className="flex items-center gap-2">
                <Button variant="outline" disabled>
                  Trocar foto
                </Button>
                <Badge variant="neutral">Em breve</Badge>
              </div>
              <p className="text-xs text-neutral-500">
                JPEG, PNG ou WebP até 2 MB. Sem foto, exibimos suas iniciais.
              </p>
            </div>
          </div>
        </section>

        <div className="grid gap-5 sm:grid-cols-2">
          <Input
            id="name"
            label="Nome de exibição *"
            value={form.name}
            maxLength={NAME_MAX}
            autoComplete="name"
            error={errors.name}
            hint="Entre 2 e 120 caracteres."
            onChange={(event) => update('name', event.target.value)}
            onBlur={() => checkField('name')}
          />
          <Input
            id="username"
            label="Nome de usuário *"
            value={form.username}
            maxLength={20}
            autoComplete="username"
            autoCapitalize="none"
            spellCheck={false}
            icon={<span className="text-sm font-semibold">@</span>}
            error={errors.username}
            hint={
              '3 a 20 caracteres: letras minúsculas, números, "_" e ".". Compõe o link do seu perfil.'
            }
            onChange={(event) =>
              update('username', event.target.value.toLowerCase())
            }
            onBlur={() => checkField('username')}
          />
        </div>

        <div>
          <label
            htmlFor={bioId}
            className="mb-2 block text-sm font-medium text-neutral-900"
          >
            Bio
          </label>
          <textarea
            id={bioId}
            rows={3}
            value={form.bio}
            maxLength={BIO_MAX}
            aria-invalid={errors.bio ? true : undefined}
            aria-describedby={`${bioId}-message`}
            onChange={(event) => update('bio', event.target.value)}
            onBlur={() => checkField('bio')}
            className={cn(
              'w-full resize-y rounded-control border bg-white px-3 py-2 text-sm text-neutral-900 outline-none placeholder:text-neutral-500',
              errors.bio
                ? 'border-error-700 focus:ring-2 focus:ring-error-700'
                : 'border-neutral-300 hover:border-primary-900 focus:border-primary-900 focus:ring-2 focus:ring-secondary-500',
            )}
          />
          <div
            id={`${bioId}-message`}
            className="mt-1 flex justify-between gap-3 text-sm"
          >
            <span
              className={errors.bio ? 'text-error-700' : 'text-neutral-500'}
            >
              {errors.bio ??
                'Conte um pouco sobre você como jogador ou colecionador.'}
            </span>
            <span className="shrink-0 text-neutral-500">
              {form.bio.length}/{BIO_MAX}
            </span>
          </div>
        </div>

        <div className="grid gap-5 sm:grid-cols-2">
          <Input
            id="city"
            label="Cidade"
            value={form.city}
            maxLength={CITY_MAX}
            autoComplete="address-level2"
            error={errors.city}
            hint="Opcional. Nunca pedimos seu endereço."
            onChange={(event) => update('city', event.target.value)}
            onBlur={() => checkField('city')}
          />
          <Select
            id="state"
            label="UF"
            options={STATE_OPTIONS}
            value={form.state}
            onValueChange={(value) => update('state', value)}
            error={errors.state}
          />
        </div>

        <fieldset>
          <legend className="mb-2 text-sm font-medium text-neutral-900">
            TCGs que joga
          </legend>
          <div className="flex flex-wrap gap-2">
            {PROFILE_GAMES.map((game) => {
              const selected = form.favoriteGames.includes(game.value)
              return (
                <label
                  key={game.value}
                  className={cn(
                    'flex cursor-pointer items-center gap-2 rounded-control border px-3 py-2 text-sm font-semibold transition-colors has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-secondary-500',
                    selected
                      ? 'border-primary-900 bg-primary-100 text-primary-900'
                      : 'border-neutral-300 bg-white text-neutral-700 hover:border-primary-900',
                  )}
                >
                  <input
                    type="checkbox"
                    checked={selected}
                    onChange={() => toggleGame(game.value)}
                    className="size-4 accent-primary-900"
                  />
                  {game.label}
                </label>
              )
            })}
          </div>
          {errors.favoriteGames ? (
            <p className="mt-1 text-sm text-error-700">
              {errors.favoriteGames}
            </p>
          ) : null}
        </fieldset>

        <fieldset className="space-y-2">
          <legend className="mb-2 text-sm font-medium text-neutral-900">
            Links (até {MAX_LINKS})
          </legend>
          {form.links.map((link, index) => (
            <Input
              key={index}
              id={`link-${index}`}
              type="url"
              inputMode="url"
              aria-label={`Link ${index + 1}`}
              placeholder="https://"
              value={link}
              maxLength={200}
              error={errors[`links[${index}]`]}
              onChange={(event) => updateLink(index, event.target.value)}
              onBlur={() => checkField(`links[${index}]`)}
            />
          ))}
          {errors.links ? (
            <p className="text-sm text-error-700">{errors.links}</p>
          ) : null}
        </fieldset>

        <section
          aria-labelledby="account-title"
          className="space-y-4 border-t border-neutral-200 pt-5"
        >
          <h2
            id="account-title"
            className="text-xs font-bold tracking-wide text-primary-700 uppercase"
          >
            Dados da conta
          </h2>
          <div className="grid gap-5 sm:grid-cols-2">
            <Input
              id="email"
              label="E-mail"
              value={profile.email}
              readOnly
              hint="A troca de e-mail estará em Configurações (em breve)."
            />
            <div>
              <p className="mb-2 block text-sm font-medium text-neutral-900">
                Nível de acesso
              </p>
              <div className="flex h-10 items-center rounded-control border border-neutral-200 bg-neutral-100 px-3">
                <Badge
                  variant={profile.role === 'ADMIN' ? 'primary' : 'neutral'}
                >
                  {profile.role === 'ADMIN' ? 'Administrador' : 'Jogador'}
                </Badge>
              </div>
              <p className="mt-1 text-sm text-neutral-500">
                Definido pela administração. Não pode ser alterado aqui.
              </p>
            </div>
          </div>
        </section>

        <div className="flex flex-wrap justify-end gap-3 border-t border-neutral-200 pt-5">
          <Button variant="ghost" onClick={cancel} disabled={saving}>
            Cancelar
          </Button>
          <Button type="submit" disabled={!dirty || saving}>
            {saving ? 'Salvando...' : 'Salvar alterações'}
          </Button>
        </div>
      </form>

      <Modal
        open={confirmCancel}
        title="Descartar alterações?"
        onClose={() => setConfirmCancel(false)}
        secondaryAction={{ label: 'Continuar editando' }}
        primaryAction={{
          label: 'Descartar',
          variant: 'danger',
          onClick: () => void navigate({ to: '/perfil' }),
        }}
      >
        <p>As alterações que você fez no perfil não serão salvas.</p>
      </Modal>
    </Card>
  )
}

function toFormState(profile: ProfileResponse): FormState {
  const links = [...profile.links]
  while (links.length < MAX_LINKS) links.push('')
  return {
    name: profile.name,
    username: profile.username,
    bio: profile.bio ?? '',
    city: profile.city ?? '',
    state: profile.state ?? '',
    favoriteGames: [...profile.favoriteGames],
    links: links.slice(0, MAX_LINKS),
  }
}

function filledLinks(form: FormState) {
  return form.links
    .map((value, slot) => ({ value: value.trim(), slot }))
    .filter((link) => link.value !== '')
}

// O backend indexa só os links enviados; `linkSlots` traduz de volta para o campo da tela.
function toRequest(form: FormState) {
  const links = filledLinks(form)
  const request: UpdateProfileRequest = {
    name: form.name.trim(),
    username: form.username.trim().toLowerCase(),
    bio: form.bio.trim() || null,
    city: form.city.trim() || null,
    state: form.state || null,
    favoriteGames: form.favoriteGames,
    links: links.map((link) => link.value),
  }
  return { request, linkSlots: links.map((link) => link.slot) }
}

function validate(form: FormState): FieldErrors {
  const errors: FieldErrors = {}
  const name = form.name.trim()
  const username = form.username.trim()

  if (!name) {
    errors.name = 'O nome é obrigatório.'
  } else if (name.length < 2 || name.length > NAME_MAX) {
    errors.name = 'O nome deve ter entre 2 e 120 caracteres.'
  }

  if (!username) {
    errors.username = 'O nome de usuário é obrigatório.'
  } else if (!USERNAME_PATTERN.test(username)) {
    errors.username =
      'Use de 3 a 20 caracteres: letras minúsculas, números, "_" e ".".'
  }

  if (form.bio.trim().length > BIO_MAX) {
    errors.bio = 'A bio deve ter no máximo 280 caracteres.'
  }
  if (form.city.trim().length > CITY_MAX) {
    errors.city = 'A cidade deve ter no máximo 60 caracteres.'
  }

  for (const link of filledLinks(form)) {
    if (link.value.length > 200) {
      errors[`links[${link.slot}]`] =
        'Cada link deve ter no máximo 200 caracteres.'
    } else if (!LINK_PATTERN.test(link.value)) {
      errors[`links[${link.slot}]`] =
        'Informe um link completo começando com https://'
    }
  }

  return errors
}

function fieldErrorsFrom(cause: unknown, linkSlots: number[]): FieldErrors {
  if (!axios.isAxiosError<ApiErrorResponse>(cause)) return {}
  const errors: FieldErrors = {}
  for (const { field, message } of cause.response?.data?.fields ?? []) {
    const link = /^links\[(\d+)\]$/.exec(field)
    const key = link ? `links[${linkSlots[Number(link[1])] ?? link[1]}]` : field
    errors[key] ??= message
  }
  return errors
}

function isVisibleField(field: string) {
  return (
    /^links\[[0-2]\]$/.test(field) ||
    [
      'name',
      'username',
      'bio',
      'city',
      'state',
      'favoriteGames',
      'links',
    ].includes(field)
  )
}

function apiMessage(cause: unknown) {
  if (axios.isAxiosError<ApiErrorResponse>(cause)) {
    return cause.response?.data?.message ?? null
  }
  return null
}

function withoutKey(errors: FieldErrors, key: string) {
  if (!(key in errors)) return errors
  const next = { ...errors }
  delete next[key]
  return next
}
