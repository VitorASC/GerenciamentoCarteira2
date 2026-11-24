# Aula 17 — Auditoria e proteção da autenticação

## Política de login

| Variável | Padrão | Justificativa no protótipo |
| --- | --- | --- |
| `AUTH_MAX_FAILED_ATTEMPTS` | `5` | Tolera erros ocasionais de digitação e permite demonstrar o bloqueio com poucas tentativas. |
| `AUTH_FAILURE_WINDOW` | `PT15M` | Agrupa uma sequência curta de falhas a partir da primeira. Erros isolados ao longo do dia deixam de se acumular. |
| `AUTH_LOCK_DURATION` | `PT15M` | Interrompe as tentativas por tempo observável e permite ao titular recuperar o acesso sem intervenção administrativa. |

A escolha é conservadora e didática. Uma implantação real exigiria ajustar os valores ao tráfego, ao risco e ao impacto de impedir o acesso de um titular. A aplicação valida a configuração ao iniciar. O máximo deve ser pelo menos um, e ambas as durações devem ser positivas.

O serviço aplica `trim`, minúsculas com `Locale.ROOT` e SHA-256 sobre os bytes UTF-8. Os 64 dígitos hexadecimais minúsculos formam a chave primária de `protecao_autenticacao`. A tabela contém apenas essa chave, o contador, o início da janela e o vencimento do bloqueio. E-mails previsíveis podem ser correlacionados por quem obtiver acesso ao banco. O hash é um pseudônimo.

A quinta falha dentro da janela inicia o bloqueio. Uma tentativa durante o bloqueio dispensa o BCrypt e preserva o vencimento. No instante exato do vencimento, a próxima tentativa é permitida. Uma nova falha depois do vencimento ou no limite da janela inicia outra contagem. Um login válido zera o contador e limpa os dois instantes. Senha incorreta, conta inexistente, conta inativa e bloqueio produzem o mesmo HTTP 401 com a mensagem `E-mail ou senha inválidos. Se houver muitas tentativas consecutivas, aguarde alguns minutos antes de tentar novamente.`.

## Concorrência e transações

O PostgreSQL executa `INSERT ... ON CONFLICT (chave_login) DO NOTHING` e em seguida adquire um lock pessimista de escrita na linha. A chave primária arbitra primeiras inserções concorrentes. Cada tentativa lê o relógio depois de adquirir o lock, mantido até a conclusão da transação. H2 e MySQL têm variantes de inserção que preservam os campos de uma linha existente.

O serviço retorna uma decisão ao controller. Uma recusa normal confirma contador e eventos e só então o controller produz o 401. No sucesso, sessão, limpeza e evento são confirmados juntos. O refresh também retorna a decisão depois de persistir rotação ou revogação e seu evento. A auditoria exige uma transação existente. Uma falha na gravação de um evento obrigatório desfaz a operação inteira.

## Eventos e consulta

| Operação | Registro em `eventos_seguranca` |
| --- | --- |
| Login | `LOGIN_SUCESSO` ou `LOGIN_FALHA`. A falha que atinge o limite registra também `BLOQUEIO_TEMPORARIO_ATIVADO`. Cada recusa posterior gera apenas `LOGIN_RECUSADO_BLOQUEIO`. |
| Rotação e recusa de refresh | `REFRESH_SUCESSO` ou `REFRESH_FALHA`. Um refresh conhecido e recusado registra o titular como alvo e o motivo da revogação, inclusive `REUTILIZACAO`. Um token desconhecido gera falha sem atribuição de identidade. |
| Encerramento de sessão | `LOGOUT` quando a família sofre revogação e `LOGOUT_GLOBAL` no pedido autenticado do titular. |
| Identidade | `PERFIL_ALTERADO`, `USUARIO_ATIVADO`, `USUARIO_INATIVADO` e `EMAIL_ALTERADO` quando o valor muda. O bootstrap registra `ADMIN_BOOTSTRAP_CRIADO` com alvo e sem ator humano. |

Cada linha guarda ID, tipo, instante UTC, IDs opcionais de ator e alvo, hash opcional do identificador de login, endereço de origem opcional e motivo de revogação enumerado. Os IDs preservam o histórico sem depender da existência posterior do cadastro. O endereço vem de `getRemoteAddr()`. Só caracteres de endereços IP e até 45 posições são aceitos. Valores com CR/LF são descartados. O serviço não interpreta `X-Forwarded-For`. O modelo não tem campo livre para corpos, cabeçalhos, e-mails, senhas ou material de tokens.

`GET /auditoria-seguranca` exige Bearer de ADMIN. A política HTTP e o método de consulta exigem esse papel. Sem autenticação a resposta é 401, e PADRAO recebe 403. A página inicial é zero e o tamanho padrão é 20. A ordenação é decrescente por instante e ID. Os parâmetros opcionais são `tipo`, `inicio` (inclusivo), `fim` (exclusivo), `ator` e `alvo`. O DTO omite o hash de login. Exemplo de consulta:

```http
GET /auditoria-seguranca?tipo=LOGIN_FALHA&page=0&size=10
```

O changeset `005-01-auditoria-autenticacao` cria as duas tabelas e os índices de consulta por instante e por tipo/instante, com ID como desempate. A suíte executa o changelog em H2 e em PostgreSQL 17 via Testcontainers e verifica a segunda execução.

## Matriz de ameaças

| Ameaça | Controle | Evidência de teste | Risco residual |
| --- | --- | --- | --- |
| Brute force contra uma conta | Contagem por identificador e bloqueio compartilhado no banco | `ProtecaoAutenticacaoTest` verifica o limite. `SessaoRefreshPostgreSqlIT` verifica 3 → 5 com duas transações aguardando o lock. | Tentativas espaçadas entre janelas continuam possíveis. |
| Credential stuffing | Contagem e eventos de falha por e-mail | `AuditoriaSegurancaHttpTest` verifica falhas persistidas e consulta por tipo. | Uma credencial válida pode funcionar na primeira tentativa. |
| Password spraying | Identificadores normalizados e eventos consultáveis por período | `AuditoriaSegurancaHttpTest` exercita variantes de e-mail, paginação e filtros. | Poucas tentativas por conta podem ficar abaixo do limite em muitas contas. |
| Enumeração | Resposta genérica para credenciais recusadas | `AccessControlIntegrationTest` compara desconhecido, inativo e senha incorreta. `AuditoriaSegurancaHttpTest` verifica a mesma mensagem durante o bloqueio. | Tempos de resposta e outros fluxos, como cadastro, podem diferir. |
| Injeção de linhas de log | Campos estruturados, hash hexadecimal, origem restrita e ausência de texto livre | `ProtecaoAutenticacaoTest` verifica CR/LF e hash inválido. | Um endereço válido representa a conexão observada e pode pertencer a um proxy. |
| Bloqueio intencional de terceiro | Prazo finito, tentativas durante o bloqueio preservam o prazo | Testes de domínio verificam o último instante bloqueado, o vencimento e a nova contagem. | Quem conhece o e-mail pode provocar novos bloqueios após o vencimento. |
| Esgotamento de armazenamento | Campos limitados, uma linha de proteção por hash e consulta paginada | Teste PostgreSQL exercita a primeira criação concorrente. O teste HTTP verifica tamanho e campos da página. | Muitos identificadores distintos e eventos repetidos podem consumir disco. Esta etapa não implementa expurgo ou cotas. |

[Contrato OpenAPI](seguranca.openapi.yaml) · [Coleção Postman](seguranca.postman_collection.json)

## Arquivos da entrega

Os caminhos abaixo são relativos ao módulo `sistemabancario`. As classes Java estão no pacote `com.projeto.sistemabancario`.

| Local | Arquivos criados ou alterados |
| --- | --- |
| Raiz do módulo | `.env.example`, `compose.yaml`, `README.md` |
| `docs` | `aula-17-auditoria.md`, `seguranca.openapi.yaml`, `seguranca.postman_collection.json` |
| Aplicação e `config/security` | `SistemabancarioApplication.java`, `AdminBootstrap.java`, `ProtecaoLoginProperties.java`, `SecurityConfig.java`, `UsuarioUserDetailsService.java` |
| `controller` e `dto` | `AuthController.java`, `AuditoriaSegurancaController.java`, `request/LoginRequest.java`, `response/EventoSegurancaResponse.java` |
| `domains` | `entity/ProtecaoAutenticacao.java`, `entity/EventoSeguranca.java`, `enums/TipoEventoSeguranca.java` |
| `repository` | `ProtecaoAutenticacaoRepository.java`, `EventoSegurancaRepository.java`, `SessaoRefreshTokenRepository.java`, `UsuarioRepository.java` |
| `service` e `util` | `LoginService.java`, `AuditoriaSegurancaService.java`, `SessaoService.java`, `UsuarioService.java`, `HashSha256.java` |
| `src/main/resources` | `application.properties`, `db/changelog/db.changelog-master.yaml`, `db/changelog/changes/005-auditoria-autenticacao.yaml` |
| `src/test/java` | `domains/entity/ProtecaoAutenticacaoTest.java`, `migration/LiquibaseMigrationIntegrationTest.java`, `repository/SessaoRefreshPostgreSqlIT.java`, `web/AuditoriaSegurancaHttpTest.java`, `web/CenarioSessaoHttp.java`, `web/SessaoHttpIntegrationTest.java` |
