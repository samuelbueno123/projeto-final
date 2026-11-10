# Guia de Operações, Deploy e DevOps — Âncora Gestão Condominial

> **Público-Alvo:** Engenheiros de Operações, DevOps, SREs e Administradores de Infraestrutura  
> **Objetivo:** Procedimentos de deploy, configuração de produção, backup, monitoramento e recuperação de falhas.  

---

## 1. Topologia de Implantação em Produção

```mermaid
flowchart TB
    Internet((Internet / Clientes)) --> Firewall[WAF / Firewall de Aplicação]
    Firewall --> NginxProxy[Nginx Reverse Proxy / Load Balancer\n(Terminação SSL/TLS + SSE Buffering Off)]
    
    subgraph AppCluster["Cluster da Aplicação (Docker / K8s)"]
        direction LR
        App1["Backend Instance 1\n(Spring Boot Port 8080)"]
        App2["Backend Instance 2\n(Spring Boot Port 8080)"]
    end

    subgraph DataCluster["Camada de Dados & Persistência"]
        PGPrimary[("PostgreSQL 16 Primary\n(Read / Write)")]
        PGReplica[("PostgreSQL 16 Replica\n(Hot Standby)")]
        PGBackup[("S3 / Backup Storage\n(Backups Criptografados)")]
    end

    subgraph Monitoring["Observabilidade & Alertas"]
        Prometheus["Prometheus / Datadog\n(Coleta /actuator/metrics)"]
        Grafana["Grafana Dashboards"]
        AlertManager["Alertmanager (Slack / PagerDuty)"]
    end

    NginxProxy --> App1
    NginxProxy --> App2
    App1 --> PGPrimary
    App2 --> PGPrimary
    PGPrimary -. Streaming WAL .-> PGReplica
    PGPrimary -. Backup Diário (pg_dump) .-> PGBackup

    Prometheus --> App1
    Prometheus --> App2
    Prometheus --> Grafana
    Prometheus --> AlertManager
```

---

## 2. Configurações para Ambiente de Produção

### 2.1. Variáveis Essenciais no Perfil `prod`
O arquivo `application-prod.properties` é ativado definindo `APP_PROFILE=prod` ou `-Dspring.profiles.active=prod`.

```properties
# Banco de Dados
spring.datasource.url=jdbc:postgresql://db-host:5432/ancora_prod
spring.datasource.username=ancora_app
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

# Validação estrita de esquema (sem alterações automáticas no banco)
spring.jpa.hibernate.ddl-auto=validate
spring.sql.init.mode=never
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

# Pool de Conexões HikariCP Resiliente
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=1200000
spring.datasource.hikari.leak-detection-threshold=30000

# Chaves Criptográficas Persistentes
security.jwt.private-key-path=/etc/secrets/jwt-private.pem
security.jwt.public-key-path=/etc/secrets/jwt-public.pem

# Sanitização de Respostas de Erro
server.error.include-stacktrace=never
server.error.include-message=always
logging.level.com.cefet_rj.projeto_final=INFO
logging.level.org.springframework.web=WARN
```

---

## 3. Configuração do Proxy Reverso Nginx

Para servir o frontend Flutter Web e rotear as chamadas da API REST com suporte a Server-Sent Events (SSE) sem interrupção de conexões, utilize a configuração do Nginx abaixo:

```nginx
server {
    listen 80;
    server_name app.condominio.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name app.condominio.com;

    ssl_certificate /etc/letsencrypt/live/app.condominio.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/app.condominio.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # Headers de Segurança
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    # Frontend Flutter Web
    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # API REST
    location /api/ {
        proxy_pass http://backend:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Correlation-ID $http_x_correlation_id;
        proxy_connect_timeout 10s;
        proxy_read_timeout 60s;
    }

    # Streaming em Tempo Real (Server-Sent Events)
    location /api/realtime/stream {
        proxy_pass http://backend:8080/realtime/stream;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Obrigatório para SSE: desativa buffer e timeout prematuro
        proxy_buffering off;
        proxy_cache off;
        proxy_set_header Connection '';
        proxy_http_version 1.1;
        chunked_transfer_encoding off;
        proxy_read_timeout 86400s; # Conexão persistente de 24h
    }
}
```

---

## 4. Estratégia de Backup, Restauração e Scripts Operacionais

O repositório inclui utilitários de linha de comando no diretório `scripts/` prontos para automação em produção:

### 4.1. Backup Automatizado (`scripts/backup-db.sh`)
Executa dump comprimido com carimbo de data/hora, gera checksum SHA256 e descarta backups com mais de 14 dias:
```bash
./scripts/backup-db.sh
```
Pode ser agendado via cron (ex: diariamente às 02:00):
```cron
0 2 * * * cd /opt/projeto-final && ./scripts/backup-db.sh >> /var/log/backup-ancora.log 2>&1
```

### 4.2. Restauração Validada (`scripts/restore-db.sh`)
Verifica a integridade criptográfica SHA256 antes de restaurar o banco de dados:
```bash
./scripts/restore-db.sh backups/db_backup_20261110_060700.sql.gz
```

### 4.3. Pipeline de Implantação e Rollback
- **Deploy com Zero Downtime (`scripts/deploy.sh`):**
  ```bash
  # Validação pré-voo em modo dry-run
  ./scripts/deploy.sh --dry-run

  # Deploy em produção com monitoramento do Actuator Healthcheck
  ./scripts/deploy.sh --env=prod --timeout=60
  ```
- **Rollback Imediato (`scripts/rollback.sh`):**
  ```bash
  ./scripts/rollback.sh
  ```

### 4.4. Pipelines de Integração e Entrega Contínua (CI/CD)
Os dois repositórios possuem automação configurada no GitHub Actions:
- **Backend CI (`.github/workflows/backend-ci.yml`):**
  1. *Hygiene & Script Lint:* Validação de scripts shell (`bash -n`) e ausência de segredos commitados.
  2. *Build & Test:* Execução de todos os 122 testes automatizados em JDK 17 Temurin com cache Maven.
  3. *Security Audit:* Análise de árvore de dependências e vulnerabilidades.
  4. *Docker Build:* Compilação de container não-root e validação de inicialização.
- **Backend Release (`.github/workflows/deploy-production.yml`):**
  Gera imagem Docker com tags semânticas e publica no GitHub Container Registry (ghcr.io).
- **Frontend CI (`.github/workflows/frontend-ci.yml`):**
  Análise estática Dart/Flutter, 38 testes de widgets e serviços, compilação Web release e imagem Nginx unprivileged.


---

## 5. Monitoramento e Alertas

### 5.1. Probes de Vida e Prontidão do Container (Kubernetes)
Configure os probes nos manifestos de deployment:
```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  timeoutSeconds: 3
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 15
  periodSeconds: 5
  timeoutSeconds: 3
  failureThreshold: 2
```

### 5.2. Métricas Críticas para Alerta no Prometheus / Grafana

| Métrica | Regra de Alerta | Ação Operacional |
|:---|:---|:---|
| `jvm.memory.used / jvm.memory.max` | Consumo de Heap > 90% por 5 min | Verificar memory leak ou escalar réplicas verticalmente. |
| `hikaricp.connections.active / max` | Conexões ativas > 80% do pool | Investigar queries lentas ou aumentar `maximum-pool-size`. |
| `condo.auth.login.failure` | > 50 falhas em 1 min para um IP | Possível ataque de força bruta; aplicar bloqueio no firewall/WAF. |
| `condo.emergency.triggered` | > 0 alertas novos | Notificar imediatamente a guarita e a central de monitoramento. |
| `http.server.requests` (status 5xx) | Taxa de erro 5xx > 1% das requisições | Consultar logs com o `traceId` correspondente para isolar o erro. |

---

## 6. Diagnóstico de Problemas Comuns (Troubleshooting Playbook)

### Problema 1: Erro de Checksum no Flyway (`FlywayValidateException`)
- **Sintoma:** Aplicação não inicia com a mensagem: `Migration checksum mismatch for migration version X`.
- **Causa:** Um arquivo de migração já executado em produção foi modificado localmente após o deploy.
- **Resolução:**
  1. Nunca altere migrations passadas. Reverta o arquivo para seu conteúdo original.
  2. Crie uma nova migration com versão subsequente (ex.: `V19__correcao_campo.sql`).
  3. Se for ambiente de desenvolvimento, utilize `./mvnw flyway:repair` para atualizar os hashes calculados.

### Problema 2: Esgotamento do Pool HikariCP (`ConnectionTimeoutException`)
- **Sintoma:** Requisições demoram exatamente 20 segundos e falham com `Connection is not available, request timed out after 20000ms`.
- **Causa:** Conexão mantida aberta por tempo excessivo devido a consulta pesada ou transação sem `@Transactional(readOnly = true)`.
- **Resolução:**
  1. Verifique nos logs a linha emitida por `leak-detection-threshold=30000` apontando a classe que reteve a conexão.
  2. Otimize a consulta utilizando `@EntityGraph` para evitar N+1 queries.
  3. Aumente temporariamente o parâmetro `spring.datasource.hikari.maximum-pool-size=30`.

### Problema 3: Bloqueio de Requisição por CORS no Frontend
- **Sintoma:** Navegador exibe erro `Cross-Origin Request Blocked: The Same Origin Policy disallows reading the remote resource`.
- **Causa:** O domínio do frontend não está cadastrado na variável `CORS_ORIGINS`.
- **Resolução:**
  1. Configure `CORS_ORIGINS=https://app.condominio.com,https://admin.condominio.com` no ambiente do backend.
  2. Reinicie a aplicação backend. O cabeçalho `Access-Control-Allow-Origin` passará a incluir o domínio solicitado.

### Problema 4: Queda das Conexões SSE (Eventos não chegam em tempo real)
- **Sintoma:** O frontend se conecta a `/realtime/stream`, mas cai a cada 60 segundos ou não recebe atualizações instantâneas.
- **Causa:** O proxy reverso (Nginx, Cloudflare ou AWS ALB) está aplicando buffer ou encerrando conexões por inatividade.
- **Resolução:**
  1. Garanta `proxy_buffering off;` no Nginx.
  2. Configure `proxy_read_timeout 86400s;`.
  3. O backend envia eventos de ping periódico para manter o canal TCP ativo.
