# Diagramas de Caso de Uso — Projeto Final

Este documento consolida o escopo funcional do projeto inteiro com base nos endpoints REST expostos no backend e no uso do frontend para consumo da API.

## 1) Diagrama de Caso de Uso Geral

> Fonte PlantUML: `docs/diagramas-caso-de-uso.puml`

```plantuml
@startuml
left to right direction
skinparam packageStyle rectangle

actor "Administrador" as Admin
actor "Síndico/Gestor" as Manager
actor "Porteiro" as Doorman
actor "Morador" as Resident
actor "Visitante (não autenticado)" as Guest

rectangle "Sistema de Gestão de Condomínio" {
  usecase "Autenticar\n(obter token OAuth2)" as UCAuth
  usecase "Cadastrar usuário" as UCRegister
  usecase "Consultar usuários" as UCUsersRead
  usecase "Atualizar usuário" as UCUsersUpdate
  usecase "Gerenciar condomínios\n(listar, detalhar, criar, atualizar)" as UCCondo
  usecase "Gerenciar setores\n(listar, detalhar, criar, atualizar)" as UCSector
  usecase "Gerenciar unidades\n(listar, detalhar, criar, atualizar)" as UCUnit
  usecase "Gerenciar vínculos\n(condomínio-usuário/role)" as UCMembership
  usecase "Gerenciar moradores\n(listar, detalhar, criar, atualizar)" as UCResident
  usecase "Gerenciar encomendas\n(listar, detalhar, registrar, atualizar status)" as UCPackage
  usecase "Calcular mensalidade\n(plano BASIC/PREMIUM)" as UCBilling
}

Guest --> UCAuth
Guest --> UCRegister

Admin --> UCAuth
Admin --> UCUsersRead
Admin --> UCUsersUpdate
Admin --> UCCondo
Admin --> UCSector
Admin --> UCUnit
Admin --> UCMembership
Admin --> UCResident
Admin --> UCPackage
Admin --> UCBilling

Manager --> UCAuth
Manager --> UCUsersRead
Manager --> UCCondo
Manager --> UCSector
Manager --> UCUnit
Manager --> UCMembership
Manager --> UCResident
Manager --> UCPackage
Manager --> UCBilling

Doorman --> UCAuth
Doorman --> UCUsersRead
Doorman --> UCPackage

Resident --> UCAuth
Resident --> UCUsersRead
Resident --> UCPackage

UCPackage ..> UCResident : <<include>>
UCResident ..> UCUnit : <<include>>
UCMembership ..> UCCondo : <<include>>
UCMembership ..> UCUsersRead : <<include>>
@enduml
```

## 2) Mapeamento de escopo considerado

- **Autenticação e autorização**: token OAuth2/JWT e proteção de rotas.
- **Gestão de usuários**: CRUD parcial via `GET/POST/PUT`.
- **Gestão condominial**: condomínios, setores, unidades, moradores e vínculos de papéis.
- **Gestão de encomendas**: cadastro, consulta e atualização de status.
- **Cálculo de cobrança**: cálculo com estratégia por plano (BASIC/PREMIUM).

## 3) Observações

- O frontend atual demonstra consumo de API (exemplo de leitura de usuários) e não limita o escopo funcional do domínio.
- O diagrama representa os **casos de uso do sistema como um todo** (principalmente o backend, que concentra as regras de negócio e endpoints).
