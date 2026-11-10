# Referência Completa da API REST — Âncora Gestão Condominial

> **Versão da API:** 2.0 (OpenAPI 3.0 / Swagger UI)  
> **Base URL Local:** `http://localhost:8080`  
> **Swagger UI:** `http://localhost:8080/swagger-ui.html`  
> **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`  

---

## 1. Padrões Globais da API

### 1.1. Cabeçalhos HTTP Padronizados

| Cabeçalho | Tipo | Obrigatório | Descrição |
|:---|:---|:---|:---|
| `Authorization` | String | Sim (endpoints autenticados) | Token JWT emitido pelo sistema: `Bearer <token>` ou `Basic <base64>` para `/oauth2/token`. |
| `X-Correlation-ID` | String | Opcional (Recomendado) | Código de rastreamento da requisição (ex.: `req-19315b-a4e`). Se omitido, o backend gera um UUID e o retorna no response header. |
| `Content-Type` | String | Sim (requisições com corpo) | Sempre `application/json; charset=UTF-8` (exceto `/oauth2/token`, que usa `application/x-www-form-urlencoded`). |
| `Accept` | String | Sim | `application/json` ou `text/csv` / `application/pdf` para relatórios exportados. |

### 1.2. Estrutura Padrão de Erros (`StandardError`)

Todas as respostas de erro da API (códigos 4xx e 5xx) seguem o contrato estrito abaixo, contendo o `correlationId` para auditoria e suporte:

```json
{
  "timestamp": "2026-11-10T14:30:00.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Horário indisponível para esta área comum",
  "path": "/common-areas/1/reservations",
  "correlationId": "req-19315b-a4e"
}
```

Para erros de validação de formulários (`400 Bad Request` em `@Valid`), o payload estende para `ValidationError`:

```json
{
  "timestamp": "2026-11-10T14:30:00.123Z",
  "status": 400,
  "error": "Validation error",
  "message": "Um ou mais campos contêm valores inválidos",
  "path": "/users",
  "correlationId": "req-19315b-a4e",
  "errors": [
    {
      "fieldName": "email",
      "message": "O email informado é inválido ou já está em uso"
    },
    {
      "fieldName": "password",
      "message": "A senha deve ter no mínimo 8 caracteres"
    }
  ]
}
```

---

## 2. Catálogo de Endpoints por Módulo

### 2.1. Autenticação e Sessão

#### `POST /oauth2/token`
Emissão de token de acesso JWT via OAuth2 Password Grant.
- **Autenticação:** HTTP Basic Auth com as credenciais do cliente OAuth2 (`test-client:test-secret` ou `CLIENT_ID:CLIENT_SECRET`).
- **Content-Type:** `application/x-www-form-urlencoded`
- **Parâmetros do Formulário:**
  - `grant_type`: `"password"`
  - `username`: Email do usuário (ex.: `morador@condominio.com`)
  - `password`: Senha do usuário
  - `scope`: `"read write"`
- **Respostas:**
  - `200 OK`: `{ "access_token": "eyJhbGciOi...", "token_type": "Bearer", "expires_in": 1800, "scope": "read write" }`
  - `400 Bad Request`: `{ "error": "invalid_grant", "error_description": "Invalid credentials" }`

#### `POST /auth/google`
Troca de Google ID Token por sessão JWT da plataforma.
- **Acesso:** Público (sem necessidade de token prévio).
- **Body:** `{ "idToken": "eyJhbGciOiJSUzI1NiIs..." }`
- **Respostas:**
  - `200 OK`: `{ "accessToken": "eyJhbGci...", "tokenType": "Bearer", "expiresIn": 1800, "email": "usuario@gmail.com", "name": "Nome Completo" }`
  - `401 Unauthorized`: Token Google inválido, expirado ou emitido por cliente não autorizado.

---

### 2.2. Usuários e Perfil

#### `POST /users`
Cadastro de nova conta de usuário.
- **Acesso:** Público.
- **Body:**
  ```json
  {
    "firstName": "Carlos",
    "lastName": "Silva",
    "email": "carlos.silva@email.com",
    "password": "SenhaSegura123!"
  }
  ```
- **Respostas:** `201 Created` (retorna dados do usuário sem senha), `400 Bad Request` (validação ou e-mail duplicado).

#### `GET /users/me`
Consulta os dados e permissões do usuário atualmente autenticado.
- **Acesso:** Qualquer usuário autenticado (`Bearer <token>`).
- **Respostas:** `200 OK` contendo ID, nome, e-mail, roles ativas e foto de perfil.

#### `PUT /users/me/profile`
Atualização cadastral do perfil e avatar.
- **Acesso:** Autenticado.
- **Body:**
  ```json
  {
    "firstName": "Carlos",
    "lastName": "Silva",
    "profileImage": "data:image/png;base64,iVBORw0KGgoAAA...",
    "currentPassword": "SenhaSegura123!"
  }
  ```
- **Respostas:** `200 OK`, `400 Bad Request`, `403 Forbidden` (senha incorreta para alteração sensível).

---

### 2.3. Condomínios e Estrutura Física

#### `GET /condominiums`
Lista os condomínios cadastrados (paginado).
- **Acesso:** `ROLE_ADMIN` ou membros autorizados.
- **Query Params:** `page` (default 0), `size` (default 20), `sort` (ex.: `name,asc`).
- **Respostas:** `200 OK` (Spring Page com `content`, `totalElements`, `totalPages`).

#### `GET /condominiums/{id}`
Detalhes de um condomínio específico por ID.
- **Acesso:** Autenticado com vínculo ao condomínio ou `ROLE_ADMIN`.
- **Respostas:** `200 OK`, `404 Not Found`.

#### `POST /condominiums`
Criação de um novo condomínio na plataforma.
- **Acesso:** `ROLE_ADMIN`.
- **Body:** `{ "name": "Condomínio Solar das Flores", "address": "Av. Brasil, 1500", "billingPlanId": 2 }`
- **Respostas:** `201 Created`.

#### `GET /sectors?condominiumId={id}` e `POST /sectors`
Listagem e cadastro de blocos/setores pertencentes ao condomínio.
- **Permissões:** `ROLE_MANAGER` ou `ROLE_ADMIN` para criação.
- **Body (POST):** `{ "name": "Bloco B", "structuralType": "BLOCK", "condominiumId": 1 }`

#### `GET /units?sectorId={id}` e `POST /units`
Listagem e cadastro de apartamentos/unidades de um setor.
- **Body (POST):** `{ "identifier": "402", "sectorId": 3 }`

---

### 2.4. Gestão de Membros e Convites

#### `GET /memberships/my`
Consulta todos os condomínios aos quais o usuário logado está vinculado e os papéis atribuídos.
- **Respostas:** `200 OK` (lista de condomínios com ID, nome, papel local e status do vínculo).

#### `POST /memberships/request`
Solicitação de entrada do morador em um condomínio.
- **Body:** `{ "condominiumId": 1, "unitId": 10, "isTenant": false }`
- **Respostas:** `201 Created` (status: `REQUESTED`).

#### `POST /manager/condominiums/{condoId}/membership-requests/{id}/approve`
Aprovação da solicitação de entrada pelo síndico.
- **Acesso:** `ROLE_MANAGER` ou `ROLE_ADMIN`.
- **Respostas:** `200 OK` (status transiciona para `ACCEPTED` e vincula o morador à unidade).

---

### 2.5. Áreas Comuns e Reservas Concorrentes

#### `GET /common-areas?condominiumId={id}`
Lista as áreas comuns disponíveis (churrasqueiras, salões de festas, piscinas).
- **Acesso:** Membros do condomínio.

#### `POST /common-areas/{id}/reservations`
Solicitação de agendamento de uma área comum.
- **Acesso:** `ROLE_RESIDENT`, `ROLE_MANAGER`.
- **Body:**
  ```json
  {
    "startAt": "2026-12-15T10:00:00Z",
    "endAt": "2026-12-15T15:00:00Z",
    "contactPhone": "(21) 98765-4321",
    "notes": "Aniversário de família",
    "joinWaitlist": true
  }
  ```
- **Respostas:**
  - `201 Created`: Reserva confirmada ou enviada para aprovação do síndico.
  - `200 OK`: Entrou automaticamente na fila de espera na posição indicada (`joinWaitlist: true`).
  - `409 Conflict`: Horário ocupado e o usuário não optou por lista de espera.

#### `PATCH /reservations/{id}/approve` e `PATCH /reservations/{id}/cancel`
Aprovação manual pelo síndico ou cancelamento pelo solicitante.
- Sob cancelamento, a fila de espera é processada automaticamente promovendo o próximo morador da fila.

---

### 2.6. Encomendas e Armários Inteligentes (Smart Lockers)

#### `GET /packages?unitId={id}`
Lista encomendas recebidas para a unidade do morador.
- **Status possíveis:** `RECEIVED`, `READY_FOR_PICKUP`, `DELIVERED`.

#### `POST /packages`
Registro de recebimento de encomenda pela portaria.
- **Acesso:** `ROLE_CONCIERGE`, `ROLE_MANAGER`.
- **Body:** `{ "description": "Caixa Média Amazon", "trackingCode": "BR123456789X", "carrier": "Loggi", "unitId": 5 }`
- **Respostas:** `201 Created` (notifica o morador via SSE e registra histórico).

#### `POST /condominiums/{id}/smart-lockers/deposit`
Alocação de encomenda em compartimento automático de Smart Locker.
- **Acesso:** Portaria / Entregador autorizado.
- **Body:** `{ "packageId": 12, "compartmentNumber": "04", "accessPin": "8492" }`
- **Respostas:** `200 OK` (compartimento trancado, morador recebe o PIN seguro para retirada).

---

### 2.7. Financeiro, Boletos e Prestação de Contas

#### `GET /condominiums/{id}/financial/expenses`
Consulta despesas do condomínio para prestação de contas transparente.
- **Acesso:** Todos os condôminos do condomínio.

#### `POST /condominiums/{id}/financial/invoices/generate-boleto`
Gera cobrança condominial com código de barras, linha digitável e QR Code Pix.
- **Acesso:** `ROLE_MANAGER`, `ROLE_ADMIN`.
- **Body:** `{ "unitId": 14, "amount": 450.00, "dueDate": "2026-12-10", "description": "Cota Condominial Dezembro/2026" }`
- **Respostas:** `201 Created` com código de barras formatado e linha digitável.

---

### 2.8. Assembleias e Votação Virtual

#### `GET /condominiums/{id}/assemblies` e `POST /condominiums/{id}/assemblies`
Gestão de assembleias virtuais ordinárias e extraordinárias.

#### `POST /assemblies/{id}/vote`
Votação formal em pauta de assembleia com quórum calculado por fração ideal.
- **Body:** `{ "agendaItemId": 3, "voteType": "APPROVE" }`
- **Respostas:** `200 OK`, `409 Conflict` (morador já votou nesta pauta).

---

### 2.9. Comunidade, Portaria e Segurança

#### `POST /condominiums/{id}/emergency/panic`
Disparo prioritário do botão de pânico / SOS.
- **Acesso:** Qualquer morador autenticado.
- **Body:** `{ "alertType": "FIRE_ALARM", "description": "Fumaça no corredor do 4º andar", "unitIdentifier": "402" }`
- **Respostas:** `201 Created` (disparo instantâneo de SSE para portaria e síndicos com registro em `AUDIT_EMERGENCY`).

#### `POST /condominiums/{id}/visitors`
Emissão de passe digital de visitante com QR Code e link WhatsApp.
- **Body:** `{ "visitorName": "Marcos Silva", "visitorDocument": "123.456.789-00", "startDate": "2026-11-15T08:00:00Z", "endDate": "2026-11-15T22:00:00Z" }`
- **Respostas:** `201 Created` com `accessCode` alfanumérico e QR Code base64.

#### `POST /condominiums/{id}/renovations`
Abertura de pedido de reforma sob a norma ABNT NBR 16.280.
- **Body:** `{ "unitId": 5, "description": "Troca de piso e demolição de parede interna", "artNumber": "ART-2026-9988", "hasStructuralImpact": false }`

---

### 2.10. Tempo Real (SSE) e Relatórios

#### `GET /realtime/stream?condominiumId={id}`
Abre fluxo contínuo Server-Sent Events (SSE) para entrega de notificações instantâneas.
- **Headers:** `Accept: text/event-stream`
- **Eventos emitidos:**
  - `PACKAGE_RECEIVED`: Nova encomenda disponível.
  - `EMERGENCY_ALERT`: Alerta de pânico disparado.
  - `DOORMAN_CHAT`: Nova mensagem da portaria.
  - `RESERVATION_STATUS`: Atualização em reserva ou fila de espera.

#### `GET /condominiums/{id}/reports/export/csv` e `/pdf`
Exportação consolidada de indicadores financeiros e operacionais em arquivo para download.

---

### 2.11. Observabilidade e Saúde do Sistema (Actuator)

| Método e Rota | Acesso | Descrição da Operação | Resposta Sucesso |
|:---|:---|:---|:---|
| `GET /actuator` | Público | Descoberta HATEOAS dos links de observabilidade | `200 OK` |
| `GET /actuator/health` | Público | Status do banco de dados, disco, memória JVM e ping | `200 OK` (`UP`) ou `503` (`DOWN`) |
| `GET /actuator/health/liveness` | Público | Probe de liveness para Kubernetes / Docker | `200 OK` (`UP`) |
| `GET /actuator/health/readiness` | Público | Probe de readiness verificando conexão ao banco | `200 OK` (`UP`) ou `503` (`OUT_OF_SERVICE`) |
| `GET /actuator/info` | Público | Metadados do sistema (versão 1.0.0, Java runtime) | `200 OK` |
| `GET /actuator/metrics` | `ROLE_ADMIN` | Lista todas as métricas operacionais disponíveis | `200 OK` |
| `GET /actuator/metrics/{name}` | `ROLE_ADMIN` | Valor pontual da métrica operacional solicitada | `200 OK` |
