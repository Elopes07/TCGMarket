// Só caminhos do próprio site: `//host` e `/\host` levariam o usuário para fora.
export function isInternalPath(path: string) {
  return (
    path.startsWith('/') && !path.startsWith('//') && !path.startsWith('/\\')
  )
}
