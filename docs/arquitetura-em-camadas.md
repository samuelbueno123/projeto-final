flowchart TB

subgraph Presentation Layer
    Controllers[REST Controllers]
    DTOs[Request/Response DTOs]
end

subgraph Application Layer
    Services[Application Services]
end

subgraph Domain Layer
    Entities[Entities]
    Enums[Enums]
    DomainRules[Business Rules]
end

subgraph Infrastructure Layer
    Repositories[Spring Data Repositories]
end

subgraph Database
    DB[(MySQL)]
end

Controllers --> Services
DTOs --> Controllers
Services --> Entities
Services --> Repositories
Repositories --> DB
Services --> DomainRules