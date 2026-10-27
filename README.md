# Pelada Gestão

API REST para organizar a pelada: cria sorteios, recebe a lista de jogadores e sorteia dois times
equilibrando os goleiros.

[![CI](https://github.com/WallaksCardoso/pelada-gestao/actions/workflows/ci.yml/badge.svg)](https://github.com/WallaksCardoso/pelada-gestao/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-informational)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-informational)
[![Licença](https://img.shields.io/badge/licen%C3%A7a-MIT-informational)](LICENSE)

## Sobre

Faz parte de um sistema em microsserviços:

- **[spring-jwt-auth](https://github.com/WallaksCardoso/spring-jwt-auth)** cadastra os usuários e emite o token JWT.
- **pelada-gestao** (este) valida esse token com a mesma chave e guarda sorteios e jogadores.
- **[email-service](https://github.com/WallaksCardoso/email-service)** recebe pelo Kafka cada jogador
  cadastrado e envia o e-mail de notificação.
- **[pelada-gestao-front](https://github.com/WallaksCardoso/pelada-gestao-front)** e
  **[pelada-gestao-front-web](https://github.com/WallaksCardoso/pelada-gestao-front-web)** são as interfaces.

## Funcionalidades

- Cadastro de sorteios com nome, data, jogadores por equipe e e-mail de notificação
- Cadastro de jogadores no sorteio, marcando quem é goleiro
- Sorteio dos times **Azul** e **Vermelho**:
  - a lista é embaralhada;
  - com dois goleiros ou mais, cada time recebe um; os demais goleiros vão para a reserva;
  - os jogadores de linha completam os times até o número por equipe, e o restante fica na reserva.
- Um sorteio só pode ser realizado uma vez; o resultado fica salvo e pode ser consultado depois
- Cada usuário só exclui os sorteios e jogadores que ele mesmo cadastrou
- Nomes repetidos são recusados (sorteio por usuário, jogador por sorteio)
- Publicação opcional de evento no Kafka a cada jogador cadastrado

## Stack

- Java 21 e Spring Boot 3.5 (Web, Security, Data JPA, Validation, Actuator)
- JJWT para validar o token
- PostgreSQL em produção; H2 em memória nos testes
- Spring for Apache Kafka
- springdoc-openapi (Swagger UI)
- Maven, Docker e GitHub Actions

## Endpoints

Todos exigem `Authorization: Bearer <token>`, exceto `/actuator/**`.

| Método | Rota | Descrição |
| --- | --- | --- |
| `POST` | `/api/sorteios` | Cria um sorteio |
| `GET` | `/api/sorteios` | Lista os sorteios |
| `GET` | `/api/sorteios/{id}` | Detalha um sorteio |
| `DELETE` | `/api/sorteios/{id}` | Exclui um sorteio do usuário |
| `GET` | `/api/sorteios/sortear/{id}` | Sorteia os times |
| `GET` | `/api/sorteios/resultado/{id}` | Mostra o resultado já sorteado |
| `POST` | `/api/jogadores` | Cadastra um jogador em um sorteio |
| `GET` | `/api/jogadores` | Lista os jogadores |
| `DELETE` | `/api/jogadores/{id}` | Exclui um jogador do usuário |

A especificação OpenAPI completa está em `src/main/resources/pelada-gestao.json`.

## Como rodar

Pré-requisitos: Java 21, um PostgreSQL acessível e a mesma chave JWT usada no
[spring-jwt-auth](https://github.com/WallaksCardoso/spring-jwt-auth).

| Variável | Descrição |
| --- | --- |
| `SPRING_DATASOURCE_URL` | URL JDBC do PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | Usuário do banco |
| `SPRING_DATASOURCE_PASSWORD` | Senha do banco |
| `JWT_SECRET` | Chave de validação do token (a mesma do serviço de autenticação) |
| `PELADA_KAFKA_ENABLED` | Opcional. `true` para publicar os eventos no Kafka (padrão: `false`) |
| `KAFKA_BOOTSTRAP_SERVERS` | Opcional. Endereço do Kafka (padrão: `localhost:9092`) |

```bash
./mvnw spring-boot:run
```

Para desenvolvimento, o perfil `local` (`application-local.properties`) cria as tabelas
automaticamente:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Kafka local para testar as notificações:

```bash
docker compose -f docker-compose.kafka.yml up -d
```

Sobe Kafka, Zookeeper e o painel Kafka UI em `http://localhost:9000`.

Com Docker:

```bash
docker build -t pelada-gestao .
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=... -e SPRING_DATASOURCE_USERNAME=... \
  -e SPRING_DATASOURCE_PASSWORD=... -e JWT_SECRET=... \
  pelada-gestao
```

## Testes

```bash
./mvnw verify
```

Os testes do sorteio rodam sem banco: conferem um goleiro por time, que nenhum jogador fica de fora,
que o sorteio fica marcado como realizado e que não pode ser repetido. Há também um teste que sobe a
aplicação inteira com H2 em memória. A CI roda os testes e monta a imagem Docker a cada push.

## Estrutura

```
src/main/java/com/pelada_gestao/
  config/        CORS
  controller/    rotas de sorteios e jogadores
  domain/        entidades, requisições, respostas e evento
  enuns/         times (azul, vermelho, reserva)
  exception/     erros de negócio e tratamento global
  kafka/         publicação dos eventos
  repository/    acesso ao banco
  scheduler/     consulta periódica que mantém o serviço ativo
  security/      validação do JWT
  service/       regras de sorteio e cadastro
```

## Licença

[MIT](LICENSE)
