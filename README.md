# projeto-final

## Banco local

O `backend/docker-compose.yml` declara PostgreSQL 16 e pgAdmin. Os dados do
PostgreSQL ficam no volume Docker `postgres_data`; a API conecta pela porta `5433`
do host e o pgAdmin fica na porta `5050`.

Na pasta `backend`, suba os serviços com `docker compose up -d --build`. Depois
inicie a API com `./mvnw spring-boot:run` (PowerShell: `./mvnw.cmd spring-boot:run`).
O Compose constrói o Flutter Web e o serve em `http://localhost:8081`. Por padrão,
o frontend chama a API do host em `http://localhost:8080`; defina `API_BASE_URL`
antes de executar o Compose para usar outro endereço e reconstrua com `--build`.
O Swagger UI da API fica em `http://localhost:8080/swagger-ui.html` e o documento
OpenAPI em `http://localhost:8080/v3/api-docs`.

## Conta administrativa local

No perfil `dev`, a API cria ou atualiza a conta administrativa durante a inicialização:

- Email: `admin@ancora.com`
- Senha: `ancoraAdmin`

Use-a no aplicativo para abrir o painel administrativo global. Os valores podem ser
sobrescritos pelas variáveis `ADMIN_EMAIL` e `ADMIN_PASSWORD`. A conta padrão só é
criada no perfil de desenvolvimento.
