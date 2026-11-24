# GestãoAtiva — Sistema de Gestão de Carteiras de Investimento

Trabalho acadêmico desenvolvido para a disciplina de **Laboratório de Programação V**, no **6º semestre** do curso de **Sistemas de Informação**.

| Informação | Detalhe |
|---|---|
| Instituição | FEF |
| Disciplina | Laboratório de Programação V |
| Professor | Jefferson Antonio Ribeiro Passerini |
| Semestre | 6º semestre — Sistemas de Informação |
| Aluno | Vitor Antonio Scandelai Cabrera |

## Visão geral

O **GestãoAtiva** é uma aplicação web para gerenciamento de carteiras de investimento em ações. Permite cadastrar usuários e corretoras, acompanhar ações brasileiras e americanas, registrar compras e vendas e visualizar indicadores financeiros calculados a partir das operações e das cotações obtidas por APIs externas.

**Principais recursos:** autenticação JWT, cadastro de corretoras com CNPJ/CEP/CVM, ações BR/EUA, BRAPI e Alpha Vantage, histórico de cotações, carteiras, preço médio ponderado, lucro/prejuízo realizado e não realizado, dashboard, temas claro/escuro, PostgreSQL, Liquibase, Docker Compose e testes automatizados.

## Arquitetura

```text
React 18 + Vite + Chart.js
        ↓ HTTP/JSON + JWT
Controllers REST
        ↓
Services / Regras de negócio
        ↓
Repositories JPA
        ↓
PostgreSQL / H2 / MySQL

Integrações: Brasil API · ViaCEP · CVM · BRAPI · Alpha Vantage
```

### Organização principal

```text
com.projeto.sistemabancario
├── controller/   → Endpoints REST
├── service/      → Regras de negócio
├── repository/   → Spring Data JPA
├── domains/      → Entidades e enums
├── dto/          → Requests e responses
├── exception/    → Tratamento de erros
├── integration/  → APIs externas
├── config/       → Segurança e configurações
└── util/         → Validações e utilitários
```

## Tecnologias

**Back-end:** Java 17, Spring Boot 4.0.6, Spring Web MVC, Spring Data JPA, Hibernate, Spring Security, JJWT 0.12.6, Bean Validation, Caffeine, Actuator, Liquibase e Maven.

**Front-end:** React 18, Vite 5, JavaScript/JSX, CSS3, Chart.js, react-chartjs-2, Fetch API, Context API, `sessionStorage` e `localStorage`.

**Banco/infraestrutura:** PostgreSQL 17, H2, MySQL, Docker e Docker Compose.

O build do React é gerado em `src/main/resources/static` e servido pelo Spring Boot.

## Integrações externas

| Serviço | Uso |
|---|---|
| Brasil API | Consulta cadastral de CNPJ |
| ViaCEP | Consulta de endereço por CEP |
| CVM | Validação de corretoras |
| BRAPI | Cotações de ações brasileiras |
| Alpha Vantage | Cotações de ações americanas |

Roteamento automático:
```text
Brasil → BRAPI
Estados Unidos → Alpha Vantage
```

As integrações utilizam **Caffeine Cache** para reduzir chamadas repetidas.

Variáveis opcionais:
```env
BRAPI_TOKEN=seu_token
ALPHAVANTAGE_API_KEY=sua_chave
```

> Nunca versione `.env`, chaves de API ou segredos JWT.

## Segurança

O GestãoAtiva autentica por `POST /auth/login` e protege as rotas da API com Spring Security, BCrypt e um filtro Bearer próprio. O JJWT assina o token com HS256 e valida o emissor configurado. O token contém `iss`, `sub` com o ID da conta, `iat`, `exp`, `jti` único por emissão e `perfilAcesso`.

- `401 Unauthorized` indica token ausente, inválido ou expirado em rota protegida. O login também retorna 401 com a mesma mensagem para senha incorreta, e-mail desconhecido ou conta inativa.
- `403 Forbidden` indica que a conta autenticada não tem permissão para a operação ou para o recurso.
- O cadastro público cria contas `PADRAO`. A cadeia HTTP e os serviços restringem a `ADMIN` as operações administrativas de ações, corretoras e usuários.
- Um `ADMIN` pode usar `PUT /usuarios/{id}/perfil-acesso` com `{"perfilAcesso":"ADMIN"}` ou `{"perfilAcesso":"PADRAO"}` para alterar outra conta. A aplicação preserva pelo menos um administrador ativo.
- Um usuário promovido deve fazer login novamente para receber um token com `ADMIN`. O rebaixamento retira os privilégios na próxima requisição à API.

Preencha `APP_SECURITY_JWT_SECRET` com pelo menos 32 bytes aleatórios. Use `APP_SECURITY_JWT_ISSUER` para configurar o emissor, cujo padrão é `gestaoativa`. `APP_SECURITY_JWT_EXPIRATION_MS` mantém a validade de 24 horas por padrão. A redução dessa duração fica para a Aula 16, junto com o refresh token. Tokens emitidos antes da mudança de emissor exigem novo login.

### Primeiro administrador

Quando houver um administrador ativo, use a conta existente. Para criar a primeira conta administrativa, preencha as variáveis abaixo em `.env` com Docker Compose ou exporte-as no ambiente do processo Spring ao executar via Maven.

```env
APP_ADMIN_BOOTSTRAP_ENABLED=true
APP_ADMIN_BOOTSTRAP_EMAIL=
APP_ADMIN_BOOTSTRAP_CPF=
APP_ADMIN_BOOTSTRAP_PASSWORD=
```

Use um e-mail e um CPF de 11 dígitos ainda livres. A senha precisa ter pelo menos 12 caracteres e até 72 bytes em UTF-8. Após confirmar o login, volte a flag para `false` e remova as credenciais do ambiente. Uma nova inicialização preserva o administrador existente enquanto houver um `ADMIN` ativo.

### OpenAPI e Postman

Importe o contrato de segurança no Swagger Editor ou em outra ferramenta OpenAPI local. Para executar as chamadas no Postman, importe a coleção e configure `email_padrao`, `senha_padrao`, `email_admin` e `senha_admin` no ambiente local. Use contas de teste. O cadastro de exemplo usa `cpf_padrao`. Os exemplos de promoção e rebaixamento usam `usuario_alvo_id` e devem ser executados individualmente em um banco de teste.

[Contrato OpenAPI](docs/seguranca.openapi.yaml) · [Coleção Postman](docs/seguranca.postman_collection.json)

## Funcionalidades

### Corretoras
- cadastro por CNPJ;
- consulta de dados empresariais;
- consulta de CEP;
- validação CVM;
- busca por ID/CNPJ;
- prevenção de duplicidade.

### Ações
- ações brasileiras e americanas;
- roteamento BRAPI/Alpha Vantage;
- busca por ID/ticker;
- atualização manual de cotação;
- histórico persistido;
- gráfico de cotação;
- prevenção de ticker duplicado.

### Carteiras e operações
- criação e edição de carteira;
- saldo inicial editável antes da primeira operação;
- associação com corretora;
- compra e venda;
- cotação atual separada do preço efetivo da operação;
- bloqueio de compra sem saldo;
- bloqueio de venda acima da posição;
- preço médio ponderado;
- venda parcial mantém o preço médio;
- recompra recalcula o preço médio;
- lucro/prejuízo realizado e não realizado;
- posição removida quando a quantidade chega a zero.

### Dashboard
Exibe saldo, total investido, valor de mercado, rentabilidade não realizada, lucro/prejuízo realizado, distribuição por ativo, posições e indicadores complementares. A distribuição é calculada pelo **valor de mercado das posições**.

## Endpoints principais

| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/auth/login` | Login |
| POST | `/usuarios` | Cadastrar usuário PADRAO |
| GET | `/usuarios` | Listar usuários como ADMIN |
| GET | `/usuarios/{id}` | Buscar usuário |
| PUT | `/usuarios/{id}/perfil-acesso` | Alterar o perfil de outra conta como ADMIN |
| POST | `/corretoras` | Cadastrar corretora |
| GET | `/corretoras` | Listar corretoras |
| GET | `/corretoras/cnpj/{cnpj}` | Buscar por CNPJ |
| POST | `/acoes` | Cadastrar ação |
| GET | `/acoes` | Listar ações |
| GET | `/acoes/ticker/{ticker}` | Buscar por ticker |
| PUT | `/acoes/{id}/atualizar-cotacao` | Atualizar cotação |
| GET | `/acoes/{id}/historico-cotacoes` | Histórico |
| POST | `/carteiras` | Criar carteira |
| GET | `/carteiras/usuario/{usuarioId}` | Listar carteiras |
| POST | `/carteiras/{id}/compras` | Registrar compra |
| POST | `/carteiras/{id}/vendas` | Registrar venda |
| GET | `/carteiras/{id}/indicadores/media-carteira` | Indicadores |

## Banco e Liquibase

O schema é versionado pelo **Liquibase**. O changelog inicial possui 9 changesets: usuários, corretoras, ações, carteiras, posições, transações, histórico de cotações, constraints únicas e chaves estrangeiras.

Após a primeira execução:
```text
Run: 0
Previously run: 11
Database is up to date, no changesets to execute
```

## Como executar

### Docker Compose
Copie `.env.example` para `.env` e preencha as variáveis obrigatórias antes de iniciar os serviços.

```bash
docker compose up -d --build
docker compose ps
```

Acesse: `http://localhost:8080`

Encerrar sem apagar dados:
```bash
docker compose down
```

Apagar também o volume:
```bash
docker compose down -v
```

### Front-end
```bash
cd frontend
npm ci
npm run build
```

### Maven
Windows:
```powershell
.\mvnw.cmd spring-boot:run
```

Linux/macOS:
```bash
./mvnw spring-boot:run
```

## Testes

```bash
mvn test           # Suíte sem Docker
mvn clean verify   # Suíte completa, relatório e verificação de cobertura
```

A suíte cobre domínio e serviços com JUnit e Mockito, aplicação, persistência e
migrations com H2 em memória, HTTP e autorização com MockMvc e integrações
externas com MockWebServer e dublês de teste.

Para `verify`, use Java 17 e mantenha o Docker disponível. O Maven Failsafe
executa `PosicaoCarteiraPostgreSqlIT` em um PostgreSQL 17 temporário com
Testcontainers. O Liquibase aplica o changelog, o Hibernate valida o schema e
o teste verifica a precisão e a unicidade da posição por carteira e ação.
O container é gerenciado pela suíte, com uma senha efêmera gerada para o teste.

O relatório HTML é gerado em
[`target/site/jacoco/index.html`](target/site/jacoco/index.html)
a partir deste módulo. O JaCoCo verifica o módulo em `verify` e falha
se a cobertura ficar abaixo de **70% de linhas** ou **40% de branches**.
A suíte original atingiu **71,93% de linhas** e **52,48% de branches**,
o que sustenta os limites da Aula 15.

## Estrutura do repositório

```text
sistemabancario/
├── frontend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   │       ├── db/changelog/
│   │       └── static/
│   └── test/
├── Dockerfile
├── compose.yaml
├── .env.example
├── pom.xml
└── README.md
```

## Referências

[Spring Boot](https://spring.io/projects/spring-boot) · [Spring Security](https://spring.io/projects/spring-security) · [Spring Data JPA](https://spring.io/projects/spring-data-jpa) · [Hibernate](https://hibernate.org/orm/) · [Liquibase](https://www.liquibase.com/) · [Docker](https://www.docker.com/) · [React](https://react.dev/) · [Vite](https://vite.dev/) · [Chart.js](https://www.chartjs.org/) · [Brasil API](https://brasilapi.com.br/) · [ViaCEP](https://viacep.com.br/) · [BRAPI](https://brapi.dev/) · [Alpha Vantage](https://www.alphavantage.co/) · [CVM](https://dados.cvm.gov.br/)

> Projeto desenvolvido com fins exclusivamente **acadêmicos** para a disciplina de **Laboratório de Programação V**, ministrada pelo Prof. **Jefferson Antonio Ribeiro Passerini**.
