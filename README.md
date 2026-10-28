# pelada-gestao

[![CI](https://github.com/WallaksCardoso/pelada-gestao/actions/workflows/ci.yml/badge.svg)](https://github.com/WallaksCardoso/pelada-gestao/actions/workflows/ci.yml)

API da pelada: cria a lista da semana, recebe os jogadores e sorteia os times Azul e Vermelho.

Começou como uma API só e virou meu laboratório de microsserviços. Hoje são três serviços Spring
Boot e uma interface:

- [spring-jwt-auth](https://github.com/WallaksCardoso/spring-jwt-auth) cadastra os usuários e emite
  o JWT, que esta API valida com a mesma chave;
- esta API guarda listas e jogadores no PostgreSQL e publica um evento no Kafka a cada jogador;
- [email-service](https://github.com/WallaksCardoso/email-service) consome esse evento e manda o
  e-mail de notificação;
- [pelada-gestao-front-web](https://github.com/WallaksCardoso/pelada-gestao-front-web) é a tela, em
  Flutter.

## Subindo tudo

Com Docker:

```bash
docker compose up -d --build
```

O compose monta os outros serviços direto dos repositórios no GitHub e sobe junto o PostgreSQL, o
Kafka e o Mailpit:

| | |
| --- | --- |
| Interface | <http://localhost:3000> |
| Autenticação | <http://localhost:8081> |
| Esta API | <http://localhost:8080> |
| E-mails enviados (Mailpit) | <http://localhost:8025> |

Crie um usuário, uma lista com qualquer e-mail de notificação, cadastre os jogadores e sorteie. Cada
jogador cadastrado aparece como e-mail no Mailpit. `docker compose down -v` derruba tudo e apaga os
dados.

## O sorteio

A lista é embaralhada. Com dois goleiros ou mais, cada time recebe um e os outros vão para a reserva;
com um só, ele cai em um dos times por sorteio. Os jogadores de linha completam os times até o número
por equipe (que conta o goleiro) e o resto fica na reserva. Cada lista só pode ser sorteada uma vez,
e o resultado fica salvo.

Cada usuário só exclui as listas e os jogadores que ele mesmo cadastrou.

## Rotas

Todas pedem `Authorization: Bearer <token>`, menos `/actuator/**`.

| Método | Rota | |
| --- | --- | --- |
| `POST` | `/api/sorteios` | cria uma lista |
| `GET` | `/api/sorteios` | lista todas |
| `GET` | `/api/sorteios/{id}` | detalha uma |
| `DELETE` | `/api/sorteios/{id}` | exclui (só quem criou) |
| `GET` | `/api/sorteios/sortear/{id}` | sorteia os times |
| `GET` | `/api/sorteios/resultado/{id}` | resultado já sorteado |
| `POST` | `/api/jogadores` | cadastra um jogador |
| `GET` | `/api/jogadores` | lista os jogadores |
| `DELETE` | `/api/jogadores/{id}` | exclui (só quem cadastrou) |

A especificação OpenAPI está em `src/main/resources/pelada-gestao.json`.

## Rodando só a API

Java 21, um PostgreSQL e a mesma chave JWT do serviço de autenticação:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/pelada
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
export JWT_SECRET=a-mesma-chave-do-spring-jwt-auth
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

O perfil `local` cria as tabelas. Para publicar os eventos, `PELADA_KAFKA_ENABLED=true` e
`KAFKA_BOOTSTRAP_SERVERS`. As origens do navegador ficam em `CORS_ORIGENS`.

## Publicando

Já rodou no Render (plano gratuito) com o banco no Supabase. Para publicar de novo: um Web Service do
tipo Docker apontando para este repositório, as variáveis acima com a URL JDBC do banco hospedado e o
health check em `/actuator/health`. O mesmo vale para o spring-jwt-auth. No plano gratuito o serviço
dorme quando fica parado e leva quase um minuto para acordar.

## Testes

```bash
./mvnw verify
```

Os do sorteio rodam sem banco: um goleiro por time, o caso de goleiro único, ninguém de fora, sorteio
que não se repete e resultado montado com uma consulta só. Um teste sobe a aplicação inteira com H2.

## Licença

MIT
