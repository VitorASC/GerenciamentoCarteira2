# GestãoAtiva — Sistema de Gestão de Carteiras de Investimento

Trabalho acadêmico desenvolvido para a disciplina de **Laboratório de Programação V**, no **6º semestre** do curso de **Sistemas de Informação**.

| Informação | Detalhe |
|---|---|
| Instituição | UNIFEF |
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

A aplicação utiliza **Spring Security + JWT**:
- login em `POST /auth/login`;
- endpoints protegidos exigem `Authorization: Bearer <token>`;
- senhas armazenadas com BCrypt;
- respostas 401/403 padronizadas;
- sessão expirada é tratada automaticamente no front-end.

```env
APP_SECURITY_JWT_SECRET=gere_uma_chave_segura
APP_SECURITY_JWT_EXPIRATION_MS=86400000
```

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
| POST | `/usuarios` | Cadastrar usuário |
| GET | `/usuarios/{id}` | Buscar usuário |
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
Previously run: 9
Database is up to date, no changesets to execute
```

## Como executar

### Docker Compose
```bash
cp .env.example .env
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
mvn test
```

Último resultado:
```text
Tests run: 48
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

Build completo:
```bash
mvn clean package
```

Também foram validados manualmente autenticação, sessão expirada, CNPJ, CEP, CVM, ações, histórico, compra sem saldo, preço médio, venda parcial, recompra, venda total, Dashboard, temas, persistência Docker/PostgreSQL e Liquibase após restart.

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
