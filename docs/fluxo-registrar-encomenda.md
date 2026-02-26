sequenceDiagram

actor Porteiro
participant EncomendaController
participant EncomendaService
participant UnidadeRepository
participant EncomendaRepository
participant DB

Porteiro->>EncomendaController: POST /encomendas (DTO)

EncomendaController->>EncomendaService: registrarEncomenda(dto)

EncomendaService->>UnidadeRepository: findById(unidadeId)
UnidadeRepository->>DB: SELECT
DB-->>UnidadeRepository: unidade

alt unidade inexistente
    EncomendaService-->>EncomendaController: RegraNegocioException
else unidade válida
    EncomendaService->>EncomendaRepository: save(encomenda)
    EncomendaRepository->>DB: INSERT
    DB-->>EncomendaRepository: OK
end

EncomendaService-->>EncomendaController: EncomendaResponseDTO
EncomendaController-->>Porteiro: 201 Created