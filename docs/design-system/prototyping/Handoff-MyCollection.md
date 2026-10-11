# Handoff UX/UI: Minha Coleção

**Versão:** 1.0  
**Status:** Protótipos principais aprovados; pronto para handoff, sujeito às validações técnicas indicadas  
**Escopo:** Área autenticada **Minha Coleção**  
**Stack prevista:** React + TypeScript · Java Spring Boot · MongoDB  
**Jogos:** Pokémon, Magic: The Gathering, Yu-Gi-Oh!, One Piece e Digimon na identidade visual; a disponibilidade real de cada catálogo deve seguir o escopo de integração aprovado.

---

## 1. Objetivo

Permitir que usuários autenticados visualizem, pesquisem, filtrem, organizem e gerenciem os exemplares de cartas que possuem, distinguindo **dados globais da carta** de **dados da posse do usuário**. O handoff especifica o comportamento visual e funcional dos protótipos de alta fidelidade.

## 2. Entregáveis visuais

1. **Coleção populada:** listagem em cards, estatísticas, pesquisa, filtros, ações e paginação.
2. **Loading:** skeletons dos cards, mantendo a estrutura da página.
3. **Coleção vazia:** ilustração de caixa/cartas, cartas flutuantes translúcidas roxas, convite para adicionar a primeira carta.
4. **Nenhum resultado:** lupa e cartas flutuantes, mensagem contextual e opção de limpar filtros.
5. **Erro de carregamento:** ilustração de alerta, mensagem clara e ação **Tentar novamente**.

**Observação:** A página 404 de rota inexistente é distinta do erro de carregamento de Minha Coleção.

## 3. Layout definitivo — desktop

Ordem vertical obrigatória:

1. **Navbar** com marca, navegação e ações de conta.
2. **Breadcrumb e board/cabeçalho** com título “Minha Coleção”, subtítulo e banner temático.
3. **Minidashboard** com estatísticas da coleção e, **na extremidade direita da mesma faixa**, o botão primário **Adicionar carta**.
4. **Área de conteúdo**, organizada em duas colunas: **filtros na lateral esquerda** e **resultados/estado da coleção à direita**.
5. **Footer** do Design System.

**Regra visual inegociável:** a barra de filtros **não começa antes** do board e do minidashboard. Nunca posicionar filtros à direita no layout desktop aprovado. Preservar os demais conceitos visuais nas cinco telas.

### 3.1 Minidashboard

Indicadores visuais previstos:

- **Cartas na coleção:** total de agrupamentos exibidos como itens de coleção (validar nomenclatura com produto).
- **Exemplares no total:** soma das quantidades de posse.
- **Cartas favoritas:** contagem de favoritos conforme a regra de favoritos existente.
- **TCGs:** quantidade de jogos distintos representados na coleção.
- **Adicionar carta:** ação primária alinhada à direita da faixa.

**Importante:** em falha na consulta de estatísticas, **não apresentar zero como se fosse valor real**. Usar skeleton, travessão ou estado de indisponibilidade. Se somente a listagem falhar, preservar estatísticas previamente carregadas e válidas.

## 4. Design System

- **Primárias:** P1 `#660366`, P2 `#996699`, P3 `#C8A2C8`.
- **Tipografia:** **Cinzel** para títulos principais, seções e destaques; **Plus Jakarta Sans** para navegação, controles e corpo de texto.
- **Componentes:** navbar, breadcrumb, cards, botões primary/secondary/outline/ghost/danger, inputs, selects, checkboxes, badges, modal, alerts e skeletons.
- **Estados:** default, hover, focus, active, disabled, loading e erro.
- **Visual:** superfícies claras e neutras, detalhes roxos, ícones coerentes com a biblioteca adotada, banner fantasioso e cartas decorativas roxas de baixa opacidade nos estados vazios/erro.
- **Acessibilidade:** contraste legível, foco visível, labels e textos acessíveis para ícones, navegação por teclado e respeito a preferência por movimento reduzido.

Aplicar tokens já documentados de espaçamento, borda e sombra; não inventar variações isoladas de componente.

## 5. Modelos e origem dos dados

### 5.1 Card — dados globais

Exemplos: `id`, `name`, `game`, `edition`, `codeCollection`, `officialRarity`, `imageUrl`, `externalId`, `source` (conforme modelo de catálogo vigente).

### 5.2 CollectionItem — dados de posse

Pertence a um usuário e referencia uma carta do catálogo. Contém os atributos de posse, incluindo **quantidade, condição e `idiom`**.

**Decisão aprovada:** `idiom` é atributo de **`CollectionItem`**, **não de `Ad`**. O anúncio poderá obter o idioma do item associado, sem duplicá-lo como fonte de verdade.

**Atenção à terminologia:** `codeCollection` pertence ao modelo global `Card` conforme o modelo de catálogo conhecido; a identificação de um exemplar/agrupamento de posse deve ser resolvida pelo identificador de `CollectionItem`. Confirmar com a modelagem atual antes de codificar.

### 5.3 Agrupamento

Itens do mesmo usuário são agrupados quando coincidirem:

`userId + cardId + condition + idiom`

Cada agrupamento possui uma **quantidade total** e ocupa **uma posição na listagem**. Duas posses da mesma carta com condição ou idioma diferentes devem permanecer separadas.

Se uma edição resultar em combinação já existente, **somar quantidades e consolidar os registros**. O backend deve executar a operação de maneira consistente, evitando duplicação e perda de dados em concorrência.

## 6. Card visual de item

### Dados globais

- Imagem, nome, jogo, edição/coleção, código da carta e raridade oficial.

### Dados de posse

- **Quantidade**, **condição** e **idioma**, visualmente diferenciados dos dados globais.

### Ações — exatamente quatro

1. **Detalhes:** navegar à página de detalhes da carta, incluindo seção **Minha posse**.
2. **Editar:** abrir **modal na própria página** para alterar atributos permitidos de posse (quantidade, condição, idioma), com validação e feedback.
3. **Remover:** abrir **modal de confirmação** e, após confirmação, **remover o agrupamento inteiro**.
4. **Anunciar:** abrir o **formulário de anúncio** com o item de posse identificado e **seleção da quantidade** a anunciar.

Não substituir uma dessas quatro ações por menu genérico de “mais ações”. O controle de favorito, quando presente, é um controle visual separado das quatro ações do item.

## 7. Interações aprovadas

### 7.1 Detalhes

- Abrir a página de detalhes globais da carta.
- Exibir a seção **Minha posse** contextualizada para o usuário autenticado.
- Preservar a distinção entre dados do catálogo e dados dos agrupamentos do usuário.

### 7.2 Editar

- Abrir modal sem abandonar a página.
- Exibir dados atuais do agrupamento.
- Permitir editar os campos de posse habilitados na implementação.
- Ao salvar, validar valores e atualizar a lista/estatísticas.
- Se houver agrupamento idêntico, consolidar quantidades de forma consistente.
- Cancelar fecha o modal sem persistir alterações; erros devem preservar dados digitados e apresentar feedback.

### 7.3 Remover

- Exigir confirmação explícita de exclusão do **agrupamento inteiro**.
- Cancelar não altera a coleção.
- Após sucesso, atualizar listagem, contadores e paginação; se a página ficar vazia, retornar à página anterior válida quando aplicável.
- **Ponto de validação técnica:** definir tratamento para agrupamentos vinculados a anúncios ativos antes de habilitar exclusão.

### 7.4 Anunciar

- Abrir formulário de anúncio a partir do agrupamento selecionado.
- Permitir informar quantidade a anunciar.
- Validar quantidade positiva e não superior à disponibilidade efetiva.
- **Ponto de validação técnica:** estabelecer regra de reserva/disponibilidade quando existirem anúncios ativos e operações simultâneas.

### 7.5 Adicionar carta

- Botão primário no extremo direito do minidashboard, presente em todos os estados quando a ação estiver disponível.
- Inicia o fluxo de inclusão de carta na coleção. **Rota/modal específico deve ser confirmado com a equipe**; o protótipo não define sozinho a navegação final.

## 8. Pesquisa, filtros e ordenação

### 8.1 Barra lateral esquerda

- Pesquisa na própria coleção.
- Filtro por **TCG**.
- **Coleção/Set**.
- **Raridade**.
- **Condição** (atributo da posse).
- **Idioma** (atributo da posse).
- Controle **Mais filtros** se previsto no Design System.
- Ações **Aplicar filtros** e **Limpar filtros**.

Os filtros são combináveis. Quando filtros são aplicados ou limpos, retornar à primeira página. Os filtros devem afetar o conjunto consultado **antes** da paginação.

### 8.2 Ordenação

- Controle “Ordenar por” acima da listagem, à direita.
- Opção inicial ilustrada: **Mais recentes**.
- Outras opções e respectivos campos de backend devem ser validados no contrato da API.
- Usar ordenação estável com desempate para evitar duplicidade ou saltos entre páginas.

## 9. Paginação e desempenho

- **Máximo de 20 agrupamentos por página**.
- Paginação realizada **no backend**, após pesquisa, filtros e ordenação.
- Retornar registros da página, total de agrupamentos encontrados, página atual e quantidade de páginas (ou metadados equivalentes).
- Um agrupamento com vários exemplares conta como **um resultado**, não como vários cards.
- Exibir texto de intervalo (ex.: “Exibindo 1–20 de 127 itens”) e navegação entre páginas.
- Ao alterar busca, filtro ou ordenação, reiniciar na página 1.
- Não carregar toda a coleção no frontend para paginar localmente.

**Contrato ilustrativo, não definitivo:** `GET /collection-items?page=0&size=20&game=...&condition=...&idiom=...&sort=...`. Nome da rota, índice inicial e parâmetros devem ser aprovados pela equipe backend.

## 10. Estados da interface

### 10.1 Carregamento

- Preservar navbar, board, minidashboard e estrutura com filtros à esquerda.
- Exibir **skeletons dos cards** na área de resultados à direita.
- Usar loading próprio para estatísticas quando necessário.
- Evitar exibir resultados antigos como se fossem da nova consulta.

### 10.2 Coleção vazia

**Condição:** usuário não possui nenhum agrupamento de posse.

- Estatísticas zeradas **somente se confirmadas pela API**.
- Ilustração central com cartas flutuantes roxas quase transparentes.
- Mensagem “Sua coleção está vazia”.
- Ações **Adicionar primeira carta** e **Explorar catálogo**.
- Manter filtros à esquerda, abaixo do minidashboard, conforme protótipo aprovado.

### 10.3 Nenhum resultado

**Condição:** a coleção pode possuir itens, mas a pesquisa/filtros atuais retornam zero.

- Preservar estatísticas gerais válidas da coleção.
- Ilustração de lupa com cartas roxas flutuantes.
- Mensagem “Nenhum resultado encontrado”.
- Ações **Limpar filtros** e **Explorar catálogo**.
- Não confundir com coleção vazia.

### 10.4 Erro de carregamento

**Condição:** falha na consulta dos itens da coleção.

- Preservar o layout geral e a barra de filtros à esquerda.
- Exibir ilustração de alerta e texto “Não foi possível carregar sua coleção”.
- Ações **Tentar novamente** e **Voltar para o início**.
- O retry deve repetir a consulta com os parâmetros atuais quando aplicável.
- Estatísticas devem exibir valores reais válidos ou indisponibilidade, **não zeros fictícios**.
- Diferenciar claramente este erro da página 404.

## 11. Responsividade — orientação para implementação

- **Desktop:** board e minidashboard em largura total; filtros à esquerda, resultados à direita.
- **Tablet:** reduzir colunas de cards e reorganizar estatísticas sem inverter a ordem visual.
- **Mobile:** manter título/banner e resumo antes da listagem; filtros podem ser acessados por botão que abre painel expansível ou drawer, **sujeito a validação visual**; cards e paginação devem permanecer utilizáveis.
- Preservar acesso às quatro ações, com rótulos acessíveis e áreas de toque adequadas.

