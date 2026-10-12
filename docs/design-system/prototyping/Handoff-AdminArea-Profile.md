# Handoff UX/UI: Área Administrativa e Perfil do Usuário

**Versão:** 1.0  
**Status:** Protótipo e handoff prontos para desenvolvimento; itens marcados como **A validar** dependem de decisão técnica nas issues de implementação  
**Escopo:** Perfil do usuário (próprio e público) e Área Administrativa (`/admin`)  
**Issues relacionadas:** #48 (este protótipo), #55 (perfil), #56 (catálogo administrativo), #130 (suspensão e moderação), #131 (avatar), #132 (dashboard)  
**Stack prevista:** React + TypeScript · Java Spring Boot · MongoDB · Cloudflare R2 (avatar)  
**Jogos com catálogo:** Pokémon, Yu-Gi-Oh! e Magic: The Gathering. One Piece e Digimon aparecem na identidade visual, mas não têm catálogo nem integração.

---

## 1. Objetivo

Definir a experiência do **perfil do usuário**, entendido como a identidade do jogador na comunidade, e da **Área Administrativa**, entendida como o espaço onde administradores cuidam das pessoas e do conteúdo publicado na plataforma.

O TCG Market é uma loja de cartas e também uma comunidade de jogadores. O perfil precisa refletir isso, e o administrador existe para manter essa comunidade saudável, não para editar o catálogo vindo das APIs.

## 2. Decisões de escopo

### 2.1 Decididas

- **TCGs fixos:** a plataforma trabalha só com Pokémon, Yu-Gi-Oh! e Magic. O administrador **não** cadastra, edita ou remove TCGs.
- **Catálogo vem das APIs:** cartas e sets são populados pelas integrações (Pokémon TCG API, YGOPRODeck, Scryfall). O administrador **não** edita dados de cartas importadas.
- **Perfil público:** usuários podem ver o perfil uns dos outros.
- **Privacidade:** e-mail nunca é público; localização é opcional e limitada a cidade/UF.
- **E-mail e senha:** a troca de e-mail e de senha **não** faz parte da edição de perfil comum. Fica em "Configurações", como funcionalidade futura.
- **Avatar:** upload de imagem com armazenamento no **Cloudflare R2** (ver seção 6.4).

- **Catálogo na área administrativa:** consulta somente leitura e ocultação de cartas (#56).

### 2.2 A validar com o grupo

- **Solicitação de carta (`CardRequest`):** a classe já está no diagrama de classes. Para cartas ausentes no catálogo, o usuário pede a inclusão e o administrador aprova ou recusa, em vez de cadastrar cartas manualmente.
- **Ocultar carta:** permitir que o administrador oculte do catálogo uma carta com dado ou imagem problemáticos, sem alterar o documento importado.

## 3. Entregáveis visuais

**Perfil**

1. **Meu perfil:** visualização do próprio perfil com o botão **Editar perfil**.
2. **Editar perfil:** formulário com validação, mais os estados de salvando, sucesso e erro.
3. **Perfil de outro jogador:** a mesma estrutura em modo somente leitura, sem ações de edição.
4. **Estados do perfil:** loading (skeleton), perfil não encontrado e erro de carregamento.

**Área Administrativa**

5. **Visão geral (dashboard):** indicadores com variação mensal, gráfico de atividade, distribuição de TCGs, ações rápidas, últimos usuários, anúncios recentes e últimas atividades.
6. **Usuários:** listagem, pesquisa e detalhes, com as ações de papel e suspensão.
7. **Moderação de perfil:** ação de limpar conteúdo de um perfil, com confirmação.
8. **Catálogo:** consulta somente leitura das cartas.
9. **Seções futuras:** itens de menu marcados como **Em breve**.
10. **Acesso negado:** tela exibida a um `USER` que tenta acessar `/admin`.

### 3.1 Arquivos do protótipo

- **Protótipo navegável:** `TCGMarket-prototipo-admin-perfil.html`. Abra pelo repositório (o logo usa caminho relativo) e use a barra inferior para alternar entre as telas e os estados. Cada estado também tem endereço próprio (por exemplo, `#admin-suspender`), e o parâmetro `?captura` esconde a barra.
- **Capturas (desktop, 1440 px):**

| Tela / estado | Arquivo |
|---|---|
| Meu perfil | `prototype-profile.png` |
| Perfil salvo com sucesso | `prototype-profile-saved.png` |
| Editar perfil | `prototype-profile-edit.png` |
| Editar perfil com erro de validação | `prototype-profile-edit-validation.png` |
| Editar perfil salvando | `prototype-profile-edit-saving.png` |
| Perfil de outro jogador | `prototype-profile-public.png` |
| Perfil carregando | `prototype-profile-loading.png` |
| Perfil não encontrado | `prototype-profile-notFound.png` |
| Erro ao carregar perfil | `prototype-profile-error.png` |
| Dashboard completa | `prototype-admin-dashboard.png` |
| Dashboard antes do marketplace | `prototype-admin-dashboard-noMarketplace.png` |
| Dashboard carregando | `prototype-admin-dashboard-loading.png` |
| Dashboard com erro em um bloco | `prototype-admin-dashboard-blockError.png` |
| Usuários | `prototype-admin-users.png` |
| Detalhe do usuário | `prototype-admin-user-detail.png` |
| Modal de suspensão | `prototype-admin-user-suspend.png` |
| Modal de limpar perfil | `prototype-admin-user-cleanProfile.png` |
| Modal de promoção | `prototype-admin-user-promote.png` |
| Catálogo administrativo | `prototype-admin-catalog.png` |
| Modal de ocultar carta | `prototype-admin-catalog-hide.png` |
| Acesso negado | `prototype-admin-accessDenied.png` |

Os dados exibidos no protótipo são ilustrativos.

### 3.2 Wireframes de referência

Os wireframes de referência são `wireframe-perfil.png` e `wireframe-areaAdministrativa.png`. A dashboard administrativa mantém a estrutura do wireframe (seção 7.2). O restante do escopo foi reduzido; os elementos que ficam fora estão listados nas seções 6.6 e 7.7.

## 4. Navegação e separação USER/ADMIN

| Rota | Tela | Acesso |
|---|---|---|
| `/perfil` | Meu perfil (com edição) | USER / ADMIN |
| `/u/{username}` | Perfil público de um jogador | Visitante / USER / ADMIN |
| `/admin` | Área Administrativa | ADMIN |

- O item **Área Administrativa** do menu do usuário só aparece para `ADMIN`. A proteção real continua no backend (`/api/admin/**` exige `ROLE_ADMIN`).
- O administrador também é um jogador: ele tem perfil, coleção e anúncios, e edita a própria conta em `/perfil` como qualquer usuário. A Área Administrativa **não** é o lugar para editar a própria conta.
- O perfil pode exibir o selo **Administrador**, mas o papel nunca aparece como campo editável, nem no próprio perfil.
- **Rota pública `/u/{username}`:** nome definitivo **a validar**. Depende do campo `username` (seção 6.2).

## 5. Design System

Seguir os tokens e componentes já documentados em `docs/design-system/` (cores P1 `#660366`, P2 `#996699`, P3 `#C8A2C8`; Cinzel nos títulos e Plus Jakarta Sans no corpo; botões, inputs, selects, badges, modal, alerts e skeletons). Não criar variações isoladas de componente.

- Ações destrutivas ou sensíveis (suspender, limpar perfil, rebaixar administrador) usam o botão **danger** e sempre passam por modal de confirmação.
- Situação da conta usa badge: **Ativa** (verde), **Suspensa** (vermelho).
- Papel usa badge: **Administrador** (roxo P1), **Jogador** (neutro).

## 6. Perfil do usuário

### 6.1 Meu perfil — layout

Ordem vertical, com base no `wireframe-perfil.png`:

1. **Navbar** e **breadcrumb** ("Perfil do usuário").
2. **Cabeçalho do perfil:** avatar, nome de exibição, `@username`, "Membro desde", bio curta, TCGs que joga e o botão **Editar perfil** à direita. Banner temático ao fundo.
3. **Conteúdo em duas colunas:**
   - **Esquerda:** "TCGs que coleciono" e, quando a #51 estiver pronta, "Minha coleção em destaque".
   - **Direita:** card **Sobre mim**, com bio completa, cidade/UF, "Membro desde" e links.
4. **Footer** do Design System.

O menu lateral do wireframe (Minha coleção, Meus anúncios, Minhas compras etc.) fica fora deste escopo, porque essas áreas ainda não existem ou já têm rota própria. **A validar:** manter só "Visão geral", "Editar perfil" e "Configurações (em breve)".

### 6.2 Campos do perfil

| Campo | Obrigatório | Regras | Visível no perfil público |
|---|---|---|---|
| Nome de exibição (`name`) | Sim | 2 a 60 caracteres | Sim |
| `username` | Sim | 3 a 20 caracteres; letras minúsculas, números, `_` e `.`; único sem diferenciar maiúsculas | Sim |
| Bio | Não | Até 280 caracteres; texto puro | Sim |
| Cidade/UF | Não | Cidade em texto (até 60) e UF de uma lista | Sim |
| TCGs que joga | Não | Subconjunto de `POKEMON`, `YUGIOH`, `MAGIC_THE_GATHERING` | Sim |
| Links | Não | Até 3 URLs `https://`, até 200 caracteres cada | Sim |
| Avatar | Não | Ver seção 6.4 | Sim |
| Membro desde (`registrationDate`) | — | Somente leitura | Sim |
| E-mail | — | Somente leitura no próprio perfil | **Não** |
| Papel (`role`) | — | Nunca editável; exibido como selo | Só o selo "Administrador" |

**Pontos de validação técnica**

- Usuários já cadastrados não têm `username`. Definir uma geração automática na migração (por exemplo, a partir do nome, com sufixo numérico) ou exigir o preenchimento no primeiro acesso ao perfil.
- Definir se o `username` pode ser trocado e com que frequência, já que ele compõe a URL pública.
- O backend deve aceitar **somente** os campos editáveis desta tabela. Campos como `role`, `email`, `passwordHash` e `registrationDate` enviados na requisição devem ser ignorados ou rejeitados (#55).

### 6.3 Editar perfil

- Abre a partir de **Editar perfil**. Pode ser uma página (`/perfil/editar`) ou um painel na própria página. **A validar** no protótipo.
- Campos preenchidos com os valores atuais; contador de caracteres na bio.
- Validação no cliente ao sair do campo e no envio. O backend repete todas as validações.
- `username` em uso: mensagem no próprio campo ("Este nome de usuário já está em uso").
- **Salvar** fica desabilitado sem alterações e mostra loading durante o envio.
- Sucesso: alert de sucesso e retorno à visualização com os dados atualizados.
- Erro: preservar o que foi digitado e mostrar o motivo. Erro de campo aparece no campo; erro geral aparece em alert no topo do formulário.
- **Cancelar** descarta as alterações; se houver alterações, pedir confirmação.

### 6.4 Avatar e Cloudflare R2

- Sem avatar, exibir as **iniciais** do nome sobre fundo P2 ou P3.
- Upload aceita JPEG, PNG e WebP com até 2 MB. Pré-visualização circular antes de salvar.
- Ações: **Trocar foto** e **Remover foto**.
- **Armazenamento:** bucket no Cloudflare R2 (compatível com a API do S3, sem custo de saída). Fluxo proposto:
  1. O frontend pede ao backend uma URL de upload assinada e de curta duração.
  2. O frontend envia o arquivo direto ao R2.
  3. O backend confirma o upload, valida tipo e tamanho e grava no usuário apenas a chave do objeto.
  4. Ao trocar ou remover a foto, o backend apaga o objeto anterior.
- Credenciais do R2 ficam em variáveis de ambiente (HML e produção), nunca no repositório nem no frontend.
- O upload é implementado na #131. Até lá, a #55 entrega o perfil com as iniciais e o botão **Trocar foto** fica desabilitado com a indicação "em breve".

### 6.5 Perfil de outro jogador

- Mesma estrutura do item 6.1, sem **Editar perfil** e sem e-mail.
- Exibe apenas os campos marcados como visíveis na tabela 6.2.
- Perfil inexistente ou de conta suspensa: estado "Perfil não encontrado", sem revelar o motivo.
- **Futuro:** ações sociais como "Ver coleção" e "Propor troca" quando essas funcionalidades existirem.

### 6.6 Fora do escopo agora

Elementos do wireframe que dependem de funcionalidades futuras e não entram no protótipo funcional:

- Estatísticas de coleção, anúncios, compras e vendas (dependem da #51 e do marketplace).
- Conquistas.
- Indicador de "online".

Podem aparecer no protótipo como seções ocultas ou com estado "em breve", sem números fictícios.

### 6.7 Estados

- **Loading:** skeleton do cabeçalho e dos cards, mantendo a estrutura.
- **Perfil não encontrado:** ilustração, mensagem e ação **Voltar para o início**.
- **Erro de carregamento:** mensagem e **Tentar novamente**; diferente da página 404.
- **Campos vazios:** no próprio perfil, convite para preencher ("Conte um pouco sobre você"); no perfil público, a seção vazia não aparece.

## 7. Área Administrativa

### 7.1 Layout

Base no `wireframe-areaAdministrativa.png`:

1. **Navbar**, **breadcrumb** e cabeçalho "Área Administrativa" com o subtítulo "Gerencie usuários e o funcionamento do TCG Market."
2. **Menu lateral à esquerda** com as seções abaixo; seções futuras aparecem desabilitadas com o badge **Em breve**.
3. **Conteúdo da seção** à direita.
4. **Footer**.

| Seção | Situação |
|---|---|
| Visão geral | Neste escopo |
| Usuários | Neste escopo (lista e papel já existem) |
| Catálogo | Neste escopo, somente leitura |
| Solicitações de carta | Em breve (**a validar**) |
| Anúncios | Em breve |
| Denúncias / Moderação de anúncios | Em breve |
| Transações e Relatórios | Em breve |

### 7.2 Visão geral (dashboard)

A dashboard com indicadores e gráficos é **exigência do projeto** e segue a estrutura do `wireframe-areaAdministrativa.png`. O protótipo representa a dashboard completa; cada bloco indica de onde vem o dado e a partir de quando ele existe. A implementação está na #132.

**Layout**

1. **Faixa de indicadores (4 cards):** valor atual e variação em relação ao mês anterior (seta e percentual).
2. **Linha principal:**
   - **Visão geral de atividade:** gráfico de linhas por dia, com seletor de período (7, 30 e 90 dias) e legenda por série.
   - **Distribuição de TCGs:** gráfico de rosca com o total de cartas no centro e percentual por TCG.
   - **Ações rápidas:** atalhos para Usuários, Catálogo e seções futuras.
3. **Linha inferior:**
   - **Últimos usuários cadastrados** (5), com papel, situação e link "Ver todos".
   - **Anúncios mais recentes** (5), com link "Ver todos".
   - **Últimas atividades** (até 6): cadastros, suspensões, cartas ocultadas, promoções de papel e, futuramente, anúncios, vendas e denúncias.

**Origem dos dados**

| Bloco | Dado | Disponível |
|---|---|---|
| Indicador | Usuários cadastrados | Agora |
| Indicador | Cartas no catálogo | Agora |
| Indicador | Anúncios ativos | Quando houver anúncios |
| Indicador | Vendas realizadas | Quando houver vendas |
| Atividade | Série "Usuários novos" | Agora (`registrationDate`) |
| Atividade | Séries "Anúncios criados" e "Vendas realizadas" | Quando houver anúncios e vendas |
| Distribuição | Cartas por TCG (Pokémon, Yu-Gi-Oh!, Magic) | Agora |
| Últimos usuários | Cadastros recentes | Agora |
| Anúncios recentes | Anúncios | Quando houver anúncios |
| Últimas atividades | Registro de ações administrativas e cadastros | Agora (#130) |

**Regras**

- Bloco cujo dado ainda não existe exibe um estado vazio ("Disponível quando o marketplace estiver ativo"), **nunca dados fictícios**. No gráfico de atividade, só aparecem as séries disponíveis.
- A distribuição de TCGs mostra apenas os três jogos com catálogo; One Piece e Digimon do wireframe não entram.
- A variação percentual compara o mês corrente com o anterior. Sem base de comparação (mês anterior zerado), exibir "—" em vez de percentual.
- Loading com skeleton em cada bloco, de forma independente: a falha de um bloco não derruba os outros.
- Em falha, mostrar indisponibilidade no bloco e a ação **Tentar novamente**; **nunca zero fictício**.
- Os gráficos precisam de alternativa acessível (valores em texto ou tabela para leitores de tela) e não podem depender só da cor para distinguir as séries.

### 7.3 Usuários

**Listagem**

- Colunas: avatar e nome, `@username`, e-mail, data de cadastro, papel, situação e ações.
- Pesquisa por nome, `username` ou e-mail. Filtros por papel e por situação.
- Paginação no backend, até 20 por página.

**Ações por usuário**

- **Ver detalhes:** painel ou página com os dados da conta, o histórico de ações administrativas sobre ela e o link para o perfil público.
- **Promover a administrador** / **Rebaixar para jogador:** já existe (`PATCH /api/admin/users/{id}/role`). Exige confirmação.
- **Suspender conta:** modal com **motivo obrigatório** (10 a 300 caracteres). A conta suspensa não consegue usar a API, e o perfil público deixa de aparecer.
- **Reativar conta:** modal de confirmação.

**Regras**

- O administrador não altera o próprio papel nem suspende a si mesmo (as ações aparecem desabilitadas, com tooltip explicando).
- Suspender ou rebaixar outro administrador **a validar**: proposta é permitir, desde que reste pelo menos um administrador ativo.
- Toda ação administrativa registra **quem fez, sobre quem, o quê, o motivo e quando**.
- A suspensão vale na próxima requisição: o filtro do JWT já busca o usuário no MongoDB a cada chamada, então basta verificar a situação da conta ali.
- Exclusão definitiva de conta **não** faz parte da área administrativa.

### 7.4 Moderação de perfil

Consequência do perfil público: o administrador precisa conseguir remover conteúdo impróprio.

- No detalhe do usuário, ação **Limpar conteúdo do perfil**, com checkboxes: bio, links, avatar e cidade/UF.
- Motivo obrigatório; ação registrada como em 7.3.
- Os campos voltam ao padrão (vazio ou iniciais). O nome e o `username` só podem ser alterados pelo próprio usuário; em caso de nome ofensivo, a ação é suspender a conta.
- **Futuro:** denúncia de perfil feita por usuários, alimentando uma fila de moderação.

### 7.5 Catálogo (somente leitura)

- Reaproveita a pesquisa, os filtros (TCG, coleção, raridade, tipo) e a paginação do catálogo público (#45).
- Cada carta exibe a **origem** (por exemplo, "Importada da Scryfall") e o identificador externo.
- Sem criar, editar ou excluir cartas, sets ou TCGs.
- Ação **Ocultar do catálogo** / **Reexibir**, com motivo (#56). A carta oculta some do catálogo público, mas continua existindo para coleções e anúncios que já a referenciam. Esse estado deve ficar fora do documento importado, porque a reimportação substitui o documento inteiro.

### 7.6 Acesso negado

- `USER` que acessa `/admin`: tela "Acesso restrito", sem revelar a estrutura da área, com a ação **Voltar para o início**.
- Visitante não autenticado: redirecionar ao login.

### 7.7 Fora do escopo agora

- Edição de cartas importadas, cadastro de TCGs e sets.
- Seções de Transações e Relatórios (o menu as exibe como "Em breve").
- Ver ou trocar a senha ou o e-mail de outro usuário.

## 8. Contratos ilustrativos (não definitivos)

| Método | Rota | Uso | Situação |
|---|---|---|---|
| `GET` | `/api/users/me/profile` | Meu perfil | Novo (#55) |
| `PATCH` | `/api/users/me/profile` | Editar campos permitidos | Novo (#55) |
| `GET` | `/api/users/{username}/profile` | Perfil público, sem e-mail e sem papel bruto | Novo (#55) |
| `POST` | `/api/users/me/avatar/upload-url` | URL assinada do R2 | Novo (issue de avatar) |
| `GET` | `/api/admin/users` | Listar usuários | Existe; precisa de pesquisa e paginação |
| `PATCH` | `/api/admin/users/{id}/role` | Alterar papel | Existe |
| `PATCH` | `/api/admin/users/{id}/status` | Suspender/reativar com motivo | Novo |
| `POST` | `/api/admin/users/{id}/profile/moderation` | Limpar campos do perfil | Novo |
| `GET` | `/api/admin/overview` | Indicadores, variação mensal e distribuição de TCGs | Novo (#132) |
| `GET` | `/api/admin/activity?days=30` | Séries diárias do gráfico de atividade | Novo (#132) |
| `GET` | `/api/admin/events?limit=6` | Últimas atividades | Novo (#130) |

Nomes de rotas e formato das respostas devem ser aprovados pela equipe de backend.

## 9. Segurança

- Toda rota `/api/admin/**` valida `ADMIN` no backend; esconder itens no frontend não substitui essa verificação.
- O perfil consultado em `/me` é sempre o do usuário autenticado; o ID nunca vem da requisição.
- Respostas públicas não incluem e-mail, `passwordHash`, situação da conta nem dados de auditoria.
- Bio, nome e links são exibidos como texto, nunca como HTML. Links abrem com `rel="noopener noreferrer"` e só aceitam `https://`.
- Upload de avatar valida tipo e tamanho no backend, não só no frontend.

## 10. Responsividade

- **Desktop:** menu lateral à esquerda e conteúdo à direita, nas duas áreas.
- **Tablet:** indicadores em duas colunas; a tabela de usuários pode esconder as colunas de e-mail e data.
- **Mobile:** o menu lateral vira abas horizontais roláveis ou um drawer (**a validar visualmente**); a tabela de usuários vira lista de cards; o cabeçalho do perfil empilha avatar, nome e botão.

## 11. Pontos de validação técnica

1. Estratégia de `username` para contas já existentes.
2. Onde guardar o estado "oculta" de uma carta sem que a reimportação o apague.
3. Regra para suspender e rebaixar outros administradores.
4. Modelo do registro de ações administrativas (collection própria, por exemplo `admin_actions`).

## 12. Issues relacionadas

- **#48:** este documento e os protótipos das telas da seção 3.
- **#55:** perfil próprio e público, com os campos da seção 6.2.
- **#56:** consulta administrativa do catálogo e ocultação de cartas (seção 7.5).
- **#130:** suspensão de contas, moderação de perfil e registro de ações administrativas (seções 7.3 e 7.4).
- **#131:** infraestrutura do Cloudflare R2 e upload de avatar (seção 6.4).
- **#132:** dashboard da área administrativa com indicadores e gráficos (seção 7.2).
- **Futuro:** solicitações de carta (`CardRequest`) e moderação de anúncios e denúncias (RF18).
