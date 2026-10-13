import { apiMessage } from '@/components/admin/adminFormat'
import { Alert } from '@/components/ui/Alert'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { Modal } from '@/components/ui/Modal'
import { changeUserRole, listUsers } from '@/services/adminApi'
import { useSession } from '@/session'
import type { UserResponse, UserRole } from '@/types/User'
import axios from 'axios'
import { useCallback, useEffect, useState } from 'react'

type PendingChange = { user: UserResponse; role: UserRole }

const ROLE_LABEL: Record<UserRole, string> = {
  USER: 'Usuário',
  ADMIN: 'Administrador',
}

const dateFormat = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short' })

export function AdminUsersPage() {
  const { user: current, refreshUser } = useSession()
  const [users, setUsers] = useState<UserResponse[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [pending, setPending] = useState<PendingChange | null>(null)
  const [saving, setSaving] = useState(false)

  const handleError = useCallback(
    (cause: unknown, fallback: string) => {
      if (axios.isAxiosError(cause) && cause.response?.status === 403) {
        void refreshUser()
      }
      setError(apiMessage(cause) ?? fallback)
    },
    [refreshUser],
  )

  useEffect(() => {
    let active = true
    listUsers()
      .then((result) => {
        if (active) {
          setUsers(result)
        }
      })
      .catch((cause: unknown) => {
        if (active) {
          handleError(cause, 'Não foi possível carregar os usuários.')
        }
      })
    return () => {
      active = false
    }
  }, [handleError])

  async function confirmChange() {
    if (!pending || saving) {
      return
    }
    setSaving(true)
    setError(null)
    setSuccess(null)
    try {
      const updated = await changeUserRole(pending.user.id, pending.role)
      setUsers(
        (list) =>
          list?.map((item) => (item.id === updated.id ? updated : item)) ??
          null,
      )
      setSuccess(
        `${updated.name} agora é ${ROLE_LABEL[updated.role].toLowerCase()}.`,
      )
    } catch (cause) {
      handleError(cause, 'Não foi possível alterar o perfil.')
    } finally {
      setSaving(false)
      setPending(null)
    }
  }

  return (
    <div className="space-y-4">
      {error ? (
        <Alert variant="error" onDismiss={() => setError(null)}>
          {error}
        </Alert>
      ) : null}
      {success ? (
        <Alert variant="success" onDismiss={() => setSuccess(null)}>
          {success}
        </Alert>
      ) : null}

      <Card>
        <div className="border-b border-neutral-200 px-5 py-4">
          <h2 className="text-lg font-semibold text-neutral-900">
            Gerenciamento de usuários
          </h2>
        </div>

        {users === null ? (
          <p className="px-5 py-6 text-sm text-neutral-500">
            {error ? 'Lista indisponível.' : 'Carregando usuários...'}
          </p>
        ) : users.length === 0 ? (
          <p className="px-5 py-6 text-sm text-neutral-500">
            Nenhum usuário cadastrado.
          </p>
        ) : (
          <ul className="divide-y divide-neutral-200">
            {users.map((item) => {
              const isSelf = item.id === current?.id
              const nextRole: UserRole =
                item.role === 'ADMIN' ? 'USER' : 'ADMIN'
              return (
                <li
                  key={item.id}
                  className="flex flex-wrap items-center justify-between gap-3 px-5 py-4"
                >
                  <div className="min-w-0">
                    <p className="truncate text-sm font-semibold text-neutral-900">
                      {item.name}
                      {isSelf ? (
                        <span className="ml-2 text-xs font-normal text-neutral-500">
                          (você)
                        </span>
                      ) : null}
                    </p>
                    <p className="truncate text-xs text-neutral-500">
                      {item.email} · desde{' '}
                      {dateFormat.format(new Date(item.registrationDate))}
                    </p>
                  </div>
                  <div className="flex items-center gap-3">
                    <Badge
                      variant={item.role === 'ADMIN' ? 'primary' : 'neutral'}
                    >
                      {ROLE_LABEL[item.role]}
                    </Badge>
                    <Button
                      variant="outline"
                      disabled={isSelf || saving}
                      title={
                        isSelf
                          ? 'Você não pode alterar o próprio perfil.'
                          : undefined
                      }
                      onClick={() => setPending({ user: item, role: nextRole })}
                    >
                      {nextRole === 'ADMIN'
                        ? 'Tornar administrador'
                        : 'Tornar usuário'}
                    </Button>
                  </div>
                </li>
              )
            })}
          </ul>
        )}
      </Card>

      <Modal
        open={pending !== null}
        title="Alterar perfil"
        onClose={() => (saving ? undefined : setPending(null))}
        secondaryAction={{ label: 'Cancelar' }}
        primaryAction={{
          label: saving ? 'Salvando...' : 'Confirmar',
          onClick: () => void confirmChange(),
        }}
      >
        {pending ? (
          <p>
            Alterar o perfil de <strong>{pending.user.name}</strong> para{' '}
            <strong>{ROLE_LABEL[pending.role].toLowerCase()}</strong>?
          </p>
        ) : null}
      </Modal>
    </div>
  )
}
