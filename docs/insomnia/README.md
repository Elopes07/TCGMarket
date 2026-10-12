# Collection do Insomnia

Arquivo: `tcg-market.insomnia.json`.

No Insomnia, use **Import** e selecione esse arquivo. A variável `base_url` aponta para `http://localhost:8080`. O backend precisa estar rodando.

A collection cobre só o que a API já expõe:

- `GET /api/health`
- `POST /api/users`
- `GET /api/catalog/{game}/cards?q=&set=&rarity=&type=&page=&size=&sort=`
- `GET /api/catalog/{game}/filters`
- `GET /api/catalog/{game}/cards/by-external-id?externalId=`

Os jogos do catálogo são `pokemon`, `yugioh` e `magic`. As rotas consultam só o catálogo interno (MongoDB); o JSON original da Pokémon TCG API, da YGOPRODeck e da Scryfall não aparece nelas.

Na pesquisa, todos os parâmetros são opcionais e podem ser combinados. `set` é o nome da coleção (o mesmo `setName` da resposta), `type` é o tipo de carta do jogo (por exemplo `TRAINER`, `TRAP` ou `CREATURE`), `page` começa em 0, `size` vai de 1 a 50 (padrão 20) e `sort` aceita `name-asc` ou `name-desc`. A resposta vem paginada:

```json
{ "content": [], "page": 0, "size": 20, "totalElements": 0, "totalPages": 0 }
```

`/filters` devolve as opções que existem no catálogo daquele jogo: `sets` (nomes das coleções), `rarities` e `types`.
