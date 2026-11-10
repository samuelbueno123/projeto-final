# Guia do Desenvolvedor — Âncora Gestão Condominial

> **Público-Alvo:** Desenvolvedores Backend (Spring Boot) e Frontend (Flutter)  
> **Objetivo:** Instruções práticas para setup, desenvolvimento, testes e contribuição no projeto.  

---

## 1. Pré-Requisitos do Ambiente

Antes de iniciar, certifique-se de que as seguintes ferramentas estão instaladas em sua máquina de desenvolvimento:

| Ferramenta | Versão Recomendada | Finalidade |
|:---|:---|:---|
| **Java JDK** | 17 LTS (OpenJDK ou Eclipse Temurin) | Compilação e execução da API Spring Boot |
| **Maven** | 3.9+ (ou utilizar o `./mvnw` incluso) | Gerenciador de dependências e build do backend |
| **Flutter SDK** | 3.10.x ou superior (canal stable) | Framework para construção do frontend Web e Mobile |
| **Dart SDK** | Incluso no Flutter (3.x) | Linguagem de desenvolvimento frontend |
| **PostgreSQL** | 15 ou 16 (ou via Docker) | Banco de dados relacional principal |
| **Docker & Docker Compose** | Docker 24+ / Compose v2 | Execução de containers e orquestração local |
| **Git** | 2.40+ | Controle de versionamento de código |

---

## 2. Estrutura dos Repositórios

O projeto está organizado em dois repositórios complementares:

```
├── samuelbueno123/projeto-final/          # Repositório Principal (Backend & Docs)
│   ├── backend/                           # Aplicação Spring Boot 3.3.6
│   │   ├── src/main/java/                 # Código-fonte Java por módulos
│   │   ├── src/main/resources/            # Configurações e Migrations Flyway
│   │   ├── src/test/java/                 # Suítes de testes unitários e de integração
│   │   ├── pom.xml                        # Descritor Maven
│   │   └── docker-compose.yml             # Orquestrador local PostgreSQL + pgAdmin
│   ├── docs/                              # Documentação Técnica Completa
│   │   ├── ARCHITECTURE.md                # Arquitetura e Decisões
│   │   ├── DATABASE.md                    # Dicionário de Dados e ERD
│   │   ├── API_REFERENCE.md               # Referência de Endpoints REST
│   │   ├── DEVELOPER_GUIDE.md             # Este guia de desenvolvimento
│   │   └── OPERATIONS_GUIDE.md            # Guia de Operações e DevOps
│   └── README.md                          # Visão geral do repositório
│
└── samuelbueno123/projeto-front/          # Repositório do Frontend (Flutter)
    ├── lib/                               # Código-fonte Dart
    │   ├── core/                          # Http client, config, design system, services
    │   └── features/                      # Módulos por funcionalidade (auth, condo, community)
    ├── test/                              # Testes automatizados de widgets e serviços
    ├── web/                               # Configuração para compilação Web
    ├── android/                           # Configuração para compilação Android
    ├── pubspec.yaml                       # Dependências Flutter
    └── README.md                          # Visão geral do frontend
```

---

## 3. Variáveis de Ambiente

As configurações do sistema são injetadas via variáveis de ambiente, com valores padrão pré-configurados para o perfil de desenvolvimento (`dev`):

| Variável | Descrição | Valor Padrão (`dev`) | Escopo |
|:---|:---|:---|:---|
| `APP_PROFILE` | Perfil ativo do Spring Boot (`dev`, `prod`, `test`) | `dev` | Backend |
| `DB_URL` | String de conexão JDBC com o PostgreSQL | `jdbc:postgresql://localhost:5433/projeto-final` | Backend |
| `DB_USERNAME` | Usuário do banco de dados | `postgres` | Backend |
| `DB_PASSWORD` | Senha do banco de dados | `postgres` | Backend |
| `CLIENT_ID` | Identificador do cliente OAuth2 local | `test-client` | Backend |
| `CLIENT_SECRET` | Segredo do cliente OAuth2 local | `test-secret` | Backend |
| `JWT_PRIVATE_KEY_PATH`| Caminho do arquivo PEM com a chave RSA privada | `./.secrets/jwt-private.pem` | Backend |
| `JWT_PUBLIC_KEY_PATH` | Caminho do arquivo PEM com a chave RSA pública | `./.secrets/jwt-public.pem` | Backend |
| `JWT_DURATION` | Validade do Access Token em segundos | `1800` (30 minutos) | Backend |
| `JWT_REFRESH_DURATION`| Validade do Refresh Token em segundos | `2592000` (30 dias) | Backend |
| `GOOGLE_CLIENT_ID` | Client ID público da aplicação no Google Cloud | ID corporativo padrão | Backend |
| `CORS_ORIGINS` | Origens autorizadas para requisições cross-origin | `http://localhost:*,http://127.0.0.1:*` | Backend |
| `ADMIN_EMAIL` | E-mail do administrador padrão no perfil `dev` | `admin@ancora.com` | Backend |
| `ADMIN_PASSWORD` | Senha do administrador padrão no perfil `dev` | `ancoraAdmin` | Backend |
| `API_BASE_URL` | URL base da API consumida pelo frontend Flutter | `http://localhost:8080` | Frontend |
| `GOOGLE_WEB_CLIENT_ID`| Client ID do Google Sign-In para Web | ID corporativo padrão | Frontend |

---

## 4. Configuração e Execução do Ambiente Local

### 4.1. Inicialização do Banco de Dados via Docker Compose
Na pasta `samuelbueno123/projeto-final/backend`:
```bash
docker compose up -d postgres
```
Isso iniciará o PostgreSQL 16 na porta `5433` com volume persistente `postgres_data`. O pgAdmin opcional estará disponível na porta `5050`.

### 4.2. Execução da API Backend
Na pasta `samuelbueno123/projeto-final/backend`:
```bash
./mvnw spring-boot:run
```
O Spring Boot iniciará na porta `8080`.
- As migrations do Flyway (V1 a V18) serão executadas automaticamente.
- A conta de administrador de desenvolvimento será provisionada (`admin@ancora.com` / `ancoraAdmin`).
- O Swagger UI estará acessível em: `http://localhost:8080/swagger-ui.html`.
- O health check da API pode ser verificado em: `http://localhost:8080/actuator/health`.

### 4.3. Execução do Frontend Flutter
Na pasta `samuelbueno123/projeto-front`:
```bash
flutter pub get
flutter run -d chrome --dart-define=API_BASE_URL=http://localhost:8080
```
Para executar no emulador Android (onde o localhost do host é mapeado em `10.0.2.2`):
```bash
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080
```

---

## 5. Execução dos Testes Automatizados

### 5.1. Testes do Backend (JUnit 5 + MockMvc)
Na pasta `samuelbueno123/projeto-final/backend`:
```bash
# Executa toda a suíte de testes (unitários, integração, persistência, RBAC, concorrência e observabilidade)
./mvnw test

# Executa apenas uma suíte específica
./mvnw test -Dtest=ProductionObservabilityIntegrationTests
./mvnw test -Dtest=DatabasePersistenceAuditTest
./mvnw test -Dtest=ConcurrencyAndEdgeCasesIntegrationTests
```

### 5.2. Testes do Frontend (Flutter Test)
Na pasta `samuelbueno123/projeto-front`:
```bash
# Executa todos os testes de widgets, integração HTTP e design system
flutter test

# Executa teste específico
flutter test test/http_client_wrapper_test.dart
flutter test test/saas_design_system_test.dart

# Análise estática de código
flutter analyze
```

---

## 6. Processo de Build e Empacotamento

### 6.1. Build do Backend (JAR Executável)
```bash
cd samuelbueno123/projeto-final/backend
./mvnw clean package -DskipTests=false
```
O artefato final será gerado em: `backend/target/projeto-final-0.0.1-SNAPSHOT.jar`.
Para rodar o jar compilado:
```bash
java -jar -Dspring.profiles.active=prod target/projeto-final-0.0.1-SNAPSHOT.jar
```

### 6.2. Build do Frontend
```bash
cd samuelbueno123/projeto-front

# Build para Web (distribuível estático para Nginx ou S3/CloudFront)
flutter build web --release --dart-define=API_BASE_URL=https://api.seudominio.com

# Build para Android (APK)
flutter build apk --release --dart-define=API_BASE_URL=https://api.seudominio.com
```

---

## 7. Convenções de Código e Boas Práticas

1. **DTOs Imutáveis:** No backend, utilize preferencialmente **Java Records** para DTOs de entrada e saída (ex.: `UserRegisterDTO`, `ReservationRequestDTO`).
2. **Camadas Isoladas:**
   - **Controllers:** Apenas roteamento, deserialização, validação de entrada (`@Valid`) e retorno de `ResponseEntity`. Não coloque lógica de negócio aqui.
   - **Services:** Regras de negócio, transações (`@Transactional`), travas pessimistas e orquestração de repositórios.
   - **Repositories:** Consultas Spring Data JPA, preferencialmente anotadas com `@EntityGraph` para evitar N+1 queries.
3. **Tratamento de Exceções:** Lance exceções de domínio especializadas (ex.: `ResourceNotFoundException`, `ReservationConflictException`). O [`ResourceExceptionHandler`](file:///home/antigravity/samuelbueno123/projeto-final/backend/src/main/java/com/cefet_rj/projeto_final/shared/exceptionhandler/ResourceExceptionHandler.java) converterá automaticamente no formato `StandardError` com `correlationId`.
4. **Log Seguro:** Nunca imprima variáveis de senhas ou tokens em concatenações manuais de strings de log. Utilize sempre o logger injetado via `@Slf4j` ou `LoggerFactory`.
5. **Componentes Reutilizáveis no Frontend:** Utilize o catálogo do SaaS Design System (`SaaSLoadingState`, `SaaSEmptyState`, `SaaSErrorState`, `SaaSDataCard`, `SaaSSearchBar`) para garantir consistência visual em todas as telas.

---

## 8. Orientações para Contribuição (GitFlow)

1. Crie uma branch de feature a partir da branch de desenvolvimento ativa:
   - Backend: `git checkout -b feature/sua-feature develop`
   - Frontend: `git checkout -b feature/sua-feature main`
2. Escreva testes automatizados para toda nova funcionalidade ou correção de bug.
3. Certifique-se de que a suíte completa passa (`./mvnw test` e `flutter test`).
4. Utilize commits semânticos no padrão Conventional Commits:
   - `feat(...)`: Nova funcionalidade.
   - `fix(...)`: Correção de bug.
   - `docs(...)`: Alteração em documentação.
   - `test(...)`: Adição ou refatoração de testes.
   - `refactor(...)`: Mudança no código que não altera o comportamento externo.
