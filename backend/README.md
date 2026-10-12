# TCC Market - Backend

API REST do projeto TCC Market, desenvolvida com Java e Spring Boot.

## Tecnologias

- Java 25
- Spring Boot 4
- Spring MVC
- Maven
- Maven Wrapper

## Pré-requisitos

- Java 25

Não é necessário instalar o Maven globalmente, pois o projeto utiliza Maven Wrapper.

## Executando o projeto

### Linux / macOS / Git Bash

```bash
./mvnw spring-boot:run
```

## Primeiro administrador

Todo cadastro público nasce como `USER`. Para ter o primeiro `ADMIN`, cadastre a conta normalmente e suba a API com `ADMIN_EMAIL` apontando para o e-mail dela:

```bash
ADMIN_EMAIL=admin@email.com ./mvnw spring-boot:run
```

Na subida, a conta com esse e-mail passa a ser `ADMIN`. Se o e-mail não existir, a API só registra um aviso no log. Depois disso, outros administradores podem ser promovidos pela Área Administrativa (`PATCH /api/admin/users/{id}/role`).

## Perfil do usuário

| Método | Rota | Acesso |
|---|---|---|
| `GET` | `/api/users/me/profile` | Usuário autenticado |
| `PUT` | `/api/users/me/profile` | Usuário autenticado |
| `GET` | `/api/users/{username}/profile` | Público |

O `PUT` aceita só `name`, `username`, `bio`, `city`, `state`, `favoriteGames` e `links`; e-mail, senha e papel enviados no corpo são ignorados. O perfil público nunca devolve e-mail, id nem papel, só o selo `administrator`.

O `username` é gerado a partir do nome no cadastro (`lucas.martins`, `lucas.martins2`, ...). Contas criadas antes do perfil recebem um na próxima subida da API. O usuário pode trocá-lo depois; um nome já em uso devolve `409` no campo `username`.