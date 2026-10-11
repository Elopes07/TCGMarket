# ✅ Conclusão da tarefa — Protótipo dos fluxos de adição e edição da coleção

## Objetivo

Definir e prototipar a experiência de **adicionar uma carta do catálogo à Minha Coleção** e de **editar os dados específicos de um item já existente**, preservando a separação entre as informações globais da carta e os dados de posse do usuário.

## Entregável

- **Protótipo interativo de alta fidelidade:** [`TCGMarket-fluxos-colecao-final.html`](./TCGMarket-fluxos-colecao-final.html)
- **Formato:** arquivo HTML único, com CSS, JavaScript e imagens incorporadas, executável localmente no navegador, **sem instalação de dependências**.
- **Referências de UX/UI:** modal de adição do Catálogo, modal de edição da coleção, modal de dashboard da carta, telas de Minha Coleção e documentação em `docs/design-system/` e `docs/design-system/prototyping/`.

> **Para publicar a evidência no GitHub:** incluir o arquivo HTML no repositório e ajustar o link relativo acima conforme o diretório escolhido. O link só funcionará no GitHub depois do commit do protótipo.

## Fluxos representados

### 1. Adicionar carta à coleção

- Entrada a partir do Catálogo ou dos detalhes da carta, no contexto do protótipo.
- Exibição da carta selecionada com **imagem, nome, TCG, coleção/set, número e raridade** em área de **somente leitura**.
- Formulário dos dados de posse: **quantidade, condição, idioma, observações e favorito**.
- Validação dos campos obrigatórios antes de salvar.
- Confirmação quando a combinação de carta, condição e idioma já corresponder a um agrupamento existente.
- Estados de **carregamento, sucesso e erro**, com opção de nova tentativa em caso de falha.

### 2. Editar item existente

- Abertura com os dados já preenchidos do agrupamento selecionado.
- Alteração apenas dos atributos da posse: **quantidade, condição, idioma, observações e favorito**.
- Aviso explícito de que editar a posse **não altera a carta do catálogo**.
- Confirmação ao tentar sair com alterações não salvas.
- Tratamento do possível **agrupamento** após mudança de condição e/ou idioma.
- Estados de **validação, carregamento, sucesso e erro**, preservando os dados em uma falha simulada.

### 3. Detalhes da carta (contexto do fluxo)

- Modal de **dashboard**, separado dos formulários de posse.
- Identificação da carta, indicadores de preço, anúncios e histórico ilustrativo.
- Ação **Criar anúncio** identificada como fluxo separado, sem abrir modal sobre modal.
- Valores de mercado explicitamente **fictícios**, usados apenas para demonstração visual.

## Regras de negócio e UX aplicadas

| Dados globais da carta (somente leitura) | Dados específicos da posse (editáveis) |
| --- | --- |
| Nome, imagem, TCG, coleção/set, número, raridade | Quantidade, condição, idioma, observações, favorito |

- A edição nunca deve ser interpretada como alteração do registro global `Card`.
- Exemplares da mesma carta podem existir em **condições ou idiomas diferentes**.
- O agrupamento ocorre para a mesma combinação de **usuário + carta + condição + idioma**; a consolidação real deverá ser garantida pelo backend.
- A quantidade corresponde ao **total de exemplares daquele agrupamento**.
- O idioma é informado na **posse** (`CollectionItem`), e não no cadastro global da carta.
- Foi mantido **um modal ativo por vez**, inclusive nas confirmações de descarte e agrupamento.

## Consistência com o Design System

- Uso da identidade roxa do TCGMarket, com destaque para a cor primária `#660366`.
- Mesma composição visual dos modais de referência: **cabeçalho, cartão de identificação à esquerda, conteúdo à direita e ações no rodapé**.
- Estilos coerentes para **inputs, selects, controle de quantidade, textarea, checkbox, badges, alertas e botões**.
- Guia de condições disponível no formulário.
- Layout adaptado para telas menores, com navegação por teclado e ações de fechamento consideradas no protótipo.

## Estados contemplados

- [x] Formulário inicial de adição.
- [x] Formulário de edição preenchido.
- [x] Validação dos campos obrigatórios e da quantidade.
- [x] Carregamento durante o salvamento simulado.
- [x] Feedback de sucesso.
- [x] Feedback de erro e tentativa de recuperação.
- [x] Confirmação para descartar alterações.
- [x] Confirmação de consolidação de agrupamentos.

## Critérios de aceite

- [x] Fluxo de adição está completo **no protótipo**.
- [x] Fluxo de edição está completo **no protótipo**.
- [x] Quantidade está representada.
- [x] Condição está representada.
- [x] Idioma está representado.
- [x] Observações estão contempladas quando aplicável.
- [x] Validações e feedbacks estão representados.
- [x] Diferença entre carta global e posse do usuário está clara.
- [x] Design System foi aplicado como referência visual e de componentes.
- [x] Protótipo está pronto para orientar a implementação.

## Como validar

1. Baixar e abrir o arquivo `.html` em um navegador, sem servidor local.
2. Testar **Adicionar** com quantidade, condição, idioma, observações e favorito.
3. Inserir quantidade inválida para conferir o feedback de validação.
4. Testar a confirmação de agrupamento quando condição e idioma coincidirem com um item existente.
5. Testar **Editar** para conferir dados pré-preenchidos e alterações.
6. Alterar um campo e cancelar para verificar a confirmação de descarte.
7. Simular os estados de **loading, sucesso e erro** e conferir a preservação dos dados.
8. Abrir **Detalhes** para conferir o dashboard separado do formulário de posse.
9. Conferir o layout em desktop e em largura reduzida.

## Observações e próximos passos

- **Esta entrega é de prototipação/UX, não de implementação de backend ou integração com banco de dados.** As gravações, os erros e os preços são simulados.
- Na implementação, reutilizar os componentes existentes do frontend e integrar os fluxos aos modelos, endpoints e validações reais.
- Confirmar as regras de mesclagem/agrupamento, limites de campo e tratamento de concorrência com a modelagem vigente.
- Ajustes visuais e de comportamento pontuais poderão ser refinados durante a programação, sem mudar os fluxos aprovados.
- A ação **Criar anúncio** pertence a um fluxo específico e não faz parte desta tarefa.

---

**Status:** **Prototipação concluída; pronta para implementação e refinamentos durante o desenvolvimento.**
