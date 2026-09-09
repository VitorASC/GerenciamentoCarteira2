# FinFEF — Sistema Bancário de Gestão de Carteiras de Investimento

Trabalho acadêmico desenvolvido para a disciplina de **Laboratório de Programação V**, no 5º semestre do curso de **Sistemas de Informação**.

| | |
|---|---|
| **Instituição** | FEF |
| **Disciplina** | Laboratório de Programação V |
| **Professor** | Jefferson Antonio Ribeiro Passerini |
| **Semestre** | 5º semestre — Sistemas de Informação |

### Equipe

- Vitor Antonio Scandelai Cabrera

## 1. Visão geral do projeto

O **FinFEF** é uma aplicação web que simula um sistema bancário voltado para a **gestão de carteiras de investimento em ações**. O usuário pode se cadastrar, autenticar, gerenciar corretoras, cadastrar ações de mercados nacional (B3) e americano (NYSE/NASDAQ), montar carteiras, registrar operações de **compra e venda** e acompanhar **indicadores financeiros** (preço médio, valor de mercado, rentabilidade) com base em **cotações ao vivo** obtidas de APIs externas.

A aplicação possui:

- **Back-end REST** em **Java 17 + Spring Boot**, com autenticação JWT e camada de integração com serviços externos.
- **Front-end** servido pelo próprio Spring Boot via **Thymeleaf**, usando HTML/CSS/JavaScript puro e **Chart.js** para gráficos de histórico de cotações.
- **Banco de dados relacional** configurável (PostgreSQL, MySQL ou H2), com mapeamento via **JPA/Hibernate**.

---

## 2. Arquitetura

O projeto segue o padrão arquitetural em **camadas** clássico do ecossistema Spring, organizado por responsabilidade:

```
┌──────────────────────────────────────────────────────────────┐
│  Front-end (Thymeleaf + JS + Chart.js)  →  dashboard.html    │
└──────────────────────────────────────────────────────────────┘
                            │  HTTP/JSON  (Authorization: Bearer JWT)
                            ▼
┌──────────────────────────────────────────────────────────────┐
│  Controllers REST (Spring Web MVC)                           │
│  AuthController · UsuarioController · CorretoraController    │
│  AcaoController · CarteiraController · DashboardController   │
└──────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────────┐
│  Services (regras de negócio)                                │
│  UsuarioService · CorretoraService · AcaoService             │
│  CarteiraService · CarteiraTradingService                    │
│  CarteiraIndicadoresService                                  │
└──────────────────────────────────────────────────────────────┘
                            │
              ┌─────────────┴──────────────┐
              ▼                            ▼
┌─────────────────────────┐   ┌──────────────────────────────┐
│  Repositories (JPA)     │   │  Integração com APIs externas │
│  Spring Data JPA        │   │  Brasil API · ViaCEP · CVM    │
│                         │   │  BRAPI · Alpha Vantage        │
└─────────────────────────┘   └──────────────────────────────┘
              │
              ▼
┌──────────────────────────────────────────────────────────────┐
│  Banco de dados relacional (PostgreSQL / MySQL / H2)         │
└──────────────────────────────────────────────────────────────┘
```

### 2.1. Organização de pacotes

```
com.projeto.sistemabancario
├── controller/        → Controladores REST e da View (Thymeleaf)
├── service/           → Lógica de negócio e orquestração
├── repository/        → Interfaces Spring Data JPA
├── domains/
│   ├── entity/        → Entidades JPA (Usuario, Corretora, Acao, …)
│   ├── enums/         → Enumerações (Mercado, PerfilInvestidor, …)
│   └── usuario/state/ → Implementação do padrão State para Usuário
├── dto/
│   ├── request/       → Objetos de entrada (validação Bean Validation)
│   └── response/      → Objetos de saída para o cliente
├── exception/         → Exceções de domínio (RegraNegocio, NotFound, …)
├── integration/       → Clientes HTTP para APIs externas
│   ├── cep/           → ViaCEP
│   ├── cnpj/          → Brasil API
│   ├── cotacao/       → BRAPI + Alpha Vantage (roteamento por mercado)
│   ├── cvm/           → Validação de corretoras (CSV oficial CVM)
│   └── config/        → Properties e cache de integrações
├── config/
│   ├── security/      → Spring Security, JWT, filtros, handlers
│   ├── CryptoConfig.java
│   └── JacksonObjectMapperConfig.java
└── util/              → Utilitários (validação de documentos, etc.)
```

### 2.2. Padrões de projeto aplicados

- **MVC** (Model-View-Controller) — separação clara entre apresentação, controle e modelo.
- **DTO** (Data Transfer Object) — `request/` e `response/` isolam o modelo de domínio da API pública.
- **Repository** — abstração de acesso a dados via Spring Data JPA.
- **Service Layer** — concentra as regras de negócio em serviços coesos.
- **State** — `EstadoUsuario`, `EstadoUsuarioAtivo` e `EstadoUsuarioInativo` controlam o ciclo de vida da conta do usuário.
- **Strategy / Adapter (Ports & Adapters)** — interfaces `CotacaoConsultationPort`, `CnpjConsultationPort`, `CepConsultationPort`, `CvmCorretoraValidationPort` permitem trocar provedores externos sem afetar o restante do sistema.
- **Roteamento por contexto** — `RoutingCotacaoClient` escolhe entre BRAPI (BR) e Alpha Vantage (EUA) com base no mercado da ação.

---

## 3. Tecnologias utilizadas

### 3.1. Back-end

| Tecnologia | Versão | Finalidade |
|---|---|---|
| **Java** | 17 | Linguagem principal |
| **Spring Boot** | 4.0.6 | Framework base (auto-configuração, embedded Tomcat) |
| **Spring Web MVC** | — | Endpoints REST e roteamento HTTP |
| **Spring Data JPA** | — | Persistência declarativa com repositórios |
| **Hibernate** | — | Implementação JPA (ORM) |
| **Spring Security** | — | Autenticação, autorização e proteção de endpoints |
| **JJWT (jsonwebtoken)** | 0.12.6 | Geração e validação de tokens JWT (HS256) |
| **Spring Validation** | — | Bean Validation (Jakarta) nas DTOs de entrada |
| **Jackson** | — | Serialização/desserialização JSON (com `jsr310` para datas Java 8+) |
| **Spring Cache + Caffeine** | — | Cache em memória para CNPJ, CEP e cotações |
| **Spring Boot Actuator** | — | Endpoints de saúde e métricas |
| **Spring Boot DevTools** | — | Hot reload em desenvolvimento |
| **Maven Wrapper** | — | Build reprodutível sem instalar Maven globalmente |

### 3.2. Bancos de dados suportados

| SGBD | Driver | Perfil Spring |
|---|---|---|
| **PostgreSQL** | `org.postgresql` | `postgresql` (padrão) |
| **MySQL** | `com.mysql:mysql-connector-j` | `mysql` |
| **H2** (em arquivo) | `com.h2database:h2` | `dev` |

### 3.3. Front-end

| Tecnologia | Finalidade |
|---|---|
| **Thymeleaf** | Engine de template no servidor (`dashboard.html`) |
| **HTML5 / CSS3** | Marcação semântica e estilização responsiva (com tema claro/escuro) |
| **JavaScript puro (ES6+)** | Lógica do cliente, consumo da API via `fetch`, manipulação de DOM |
| **Chart.js 4.4.1** | Gráficos de linha do histórico de cotações |

### 3.4. Testes

| Biblioteca | Finalidade |
|---|---|
| **Spring Boot Starter Test** | JUnit 5 + AssertJ + Mockito |
| **Spring Security Test** | Testes de endpoints autenticados |
| **MockWebServer (OkHttp)** | Simulação de respostas das APIs externas em testes de integração |

---

## 4. Integrações com serviços externos

Uma das principais características do projeto é a **integração com APIs públicas reais** para enriquecimento dos dados:

| API | Uso no sistema | Endpoint base |
|---|---|---|
| **Brasil API** | Consulta de CNPJ ao cadastrar uma corretora (razão social, endereço, etc.) | `https://brasilapi.com.br/api` |
| **ViaCEP** | Complementação automática do endereço a partir do CEP | `https://viacep.com.br` |
| **CVM** (Comissão de Valores Mobiliários) | Validação se a corretora informada é regulamentada (parsing do CSV oficial) | `https://dados.cvm.gov.br/dados/INTERMED/CAD/DADOS/cad_intermed.zip` |
| **BRAPI** | Cotações de ações brasileiras (B3) | `https://brapi.dev/api` |
| **Alpha Vantage** | Cotações de ações americanas (NYSE / NASDAQ) | `https://www.alphavantage.co` |

> **Observação:** as integrações usam **cache Caffeine** (`maximumSize=5000, expireAfterWrite=15m`) para reduzir chamadas repetidas e respeitar os limites gratuitos das APIs.

### 4.1. Chaves de API necessárias

- **BRAPI**: sem token, apenas `PETR4`, `VALE3`, `MGLU3` e `ITUB4` são consultáveis. Para liberar todos os tickers, defina a variável de ambiente `BRAPI_TOKEN` (cadastro gratuito em <https://brapi.dev/dashboard>).
- **Alpha Vantage**: obrigatório para cotar ações americanas. Defina `ALPHAVANTAGE_API_KEY` (cadastro gratuito em <https://www.alphavantage.co/support/#api-key>).

---

## 5. Segurança

A camada de segurança foi implementada com **Spring Security** e tokens **JWT (JSON Web Token)** assinados em **HS256**.

- **Login (público):** `POST /auth/login` recebe `{ email, senha }`, valida via `AuthenticationManager` e retorna um **access token** (`Bearer`).
- **Demais endpoints:** exigem o cabeçalho `Authorization: Bearer <token>`, validado pelo filtro `JwtAuthenticationFilter`.
- **Tratamento de erros:** `JwtAuthenticationEntryPoint` (401) e `JwtAccessDeniedHandler` (403) padronizam respostas JSON.
- **Senhas:** armazenadas com hash (BCrypt) — nunca em texto puro.
- **Expiração:** definida via `app.security.jwt.expiration-ms` (padrão 24h).

> Em produção, configure obrigatoriamente `APP_SECURITY_JWT_SECRET` com pelo menos 32 bytes aleatórios.

---

## 6. Funcionalidades do sistema

| Módulo | Funcionalidades |
|---|---|
| **Autenticação** | Cadastro de usuário, login com JWT, atualização de conta, ativação/desativação |
| **Corretoras** | Cadastro com validação CVM + enriquecimento via Brasil API e ViaCEP, listagem, busca por ID e CNPJ |
| **Ações** | Cadastro de ações (BR/EUA), atualização de cotação sob demanda, listagem, histórico de cotações |
| **Carteiras** | Criação, atualização, listagem por usuário, saldo inicial |
| **Operações** | Compra e venda com **cotação ao vivo** no momento da operação |
| **Indicadores** | Preço médio por ticker, valor de custo, valor de mercado, rentabilidade acumulada, média da carteira inteira |
| **Gráficos** | Histórico de cotações de cada ação em gráfico de linha (Chart.js) |
| **Tema** | Alternância entre tema claro e escuro com persistência em `localStorage` |

---

## 7. Endpoints REST (resumo)

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/auth/login` | Autenticar e obter JWT |
| `POST` | `/usuarios` | Cadastro público de usuário |
| `GET`  | `/usuarios` | Listar usuários (autenticado) |
| `GET`  | `/usuarios/{id}` | Buscar usuário por ID |
| `PUT`  | `/usuarios/{id}` | Atualizar dados do usuário |
| `POST` | `/corretoras` | Cadastrar corretora (Brasil API + ViaCEP + CVM) |
| `GET`  | `/corretoras` | Listar corretoras |
| `GET`  | `/corretoras/{id}` | Buscar por ID |
| `GET`  | `/corretoras/cnpj/{cnpj}` | Buscar por CNPJ |
| `POST` | `/acoes` | Cadastrar ação |
| `GET`  | `/acoes` | Listar ações |
| `GET`  | `/acoes/{id}` | Buscar por ID |
| `GET`  | `/acoes/ticker/{ticker}` | Buscar por ticker |
| `PUT`  | `/acoes/{id}/atualizar-cotacao` | Atualizar cotação via API externa |
| `GET`  | `/acoes/{id}/historico-cotacoes` | Histórico paginado para o gráfico |
| `POST` | `/carteiras` | Criar nova carteira |
| `GET`  | `/carteiras` | Listar carteiras do usuário logado |
| `POST` | `/carteiras/{id}/compras` | Registrar compra (cotação ao vivo) |
| `POST` | `/carteiras/{id}/vendas` | Registrar venda (cotação ao vivo) |
| `GET`  | `/carteiras/{id}/indicadores/ticker/{ticker}` | Indicador de um ticker da carteira |
| `GET`  | `/carteiras/{id}/indicadores/media-carteira` | Médias e totais da carteira |

---

## 8. Como executar

### 8.1. Pré-requisitos

- **Java 17+** instalado (`java -version`)
- **Maven Wrapper** (já incluso — não precisa instalar Maven)
- Um banco de dados:
  - **PostgreSQL** rodando localmente (recomendado), **ou**
  - Use o perfil `dev` para rodar com **H2** em arquivo (sem instalar nada)

### 8.2. Configuração do banco (PostgreSQL — perfil padrão)

```sql
CREATE DATABASE sistemabancario;
```

Variáveis de ambiente opcionais (com defaults para desenvolvimento):

```bash
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DATABASE=sistemabancario
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postdba
```

### 8.3. Variáveis de ambiente recomendadas

```bash
# Integrações
BRAPI_TOKEN=seu_token_brapi
ALPHAVANTAGE_API_KEY=sua_chave_alphavantage

# Segurança (obrigatório em produção, opcional em dev)
APP_SECURITY_JWT_SECRET=uma-chave-aleatoria-com-pelo-menos-32-bytes
```

### 8.4. Executando

**Windows (PowerShell):**

```powershell
.\mvnw.cmd spring-boot:run
```

**Linux / macOS:**

```bash
./mvnw spring-boot:run
```

**Executando com perfil de desenvolvimento (H2):**

```bash
./mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"
```

A aplicação inicia em <http://localhost:8080>.

### 8.5. Acessando

- **Dashboard:** <http://localhost:8080/>
- **Console H2** (apenas perfil `dev`): <http://localhost:8080/h2-console>
- **Actuator:** <http://localhost:8080/actuator/health>

---

## 9. Estrutura de pastas do repositório

```
sistemabancario/
├── mvnw / mvnw.cmd            → Wrapper do Maven
├── pom.xml                    → Dependências e build
├── src/
│   ├── main/
│   │   ├── java/com/projeto/sistemabancario/   → Código-fonte
│   │   └── resources/
│   │       ├── application.properties          → Config padrão
│   │       ├── application-postgresql.properties
│   │       ├── application-mysql.properties
│   │       ├── application-dev.properties      → Perfil H2
│   │       ├── static/                         → CSS e JS
│   │       │   ├── css/dashboard.css
│   │       │   └── js/dashboard.js
│   │       └── templates/
│   │           └── dashboard.html              → View principal
│   └── test/                                   → Testes unitários e de integração
└── README.md
```

---

## 10. Conceitos da disciplina aplicados

O projeto foi desenhado para exercitar, de forma prática, os principais tópicos abordados em **Laboratório de Programação V**:

- **Programação orientada a objetos avançada** (herança, polimorfismo, interfaces, enums).
- **Padrões de projeto** (State, Strategy/Ports & Adapters, DTO, Repository, MVC).
- **Arquitetura em camadas** e separação de responsabilidades.
- **Persistência com JPA/Hibernate**, mapeamento de relacionamentos (1:N, N:1), enums, datas e auditoria.
- **Consumo de APIs REST externas** com tratamento de falhas e cache.
- **Construção de APIs REST** com validação de entrada, paginação e respostas padronizadas.
- **Segurança da aplicação** com autenticação stateless via JWT e hashing de credenciais.
- **Front-end consumindo back-end** via `fetch` com tratamento de erros e estados de UI.
- **Testes automatizados** (unitários e de integração) com mocks de serviços externos.

---

## 11. Créditos e referências

Documentações oficiais consultadas:

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring Security](https://spring.io/projects/spring-security)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [Hibernate ORM](https://hibernate.org/orm/)
- [JJWT](https://github.com/jwtk/jjwt)
- [Caffeine Cache](https://github.com/ben-manes/caffeine)
- [Thymeleaf](https://www.thymeleaf.org/)
- [Chart.js](https://www.chartjs.org/)
- [Brasil API](https://brasilapi.com.br/)
- [ViaCEP](https://viacep.com.br/)
- [BRAPI](https://brapi.dev/)
- [Alpha Vantage](https://www.alphavantage.co/)
- [Portal de Dados Abertos da CVM](https://dados.cvm.gov.br/)

---

> Projeto desenvolvido com fins exclusivamente **acadêmicos** para a disciplina de Laboratório de Programação V, ministrada pelo Prof. **Jefferson Antonio Ribeiro Passerini**.
