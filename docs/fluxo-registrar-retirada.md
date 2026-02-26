sequenceDiagram

actor Porteiro
participant EncomendaController
participant EncomendaService
participant EncomendaRepository
participant DB

Porteiro->>EncomendaController: PATCH /encomendas/{id}/retirada

EncomendaController->>EncomendaService: registrarRetirada(id)

EncomendaService->>EncomendaRepository: findById(id)
EncomendaRepository->>DB: SELECT
DB-->>EncomendaRepository: encomenda

alt status != RECEBIDA
    EncomendaService-->>EncomendaController: RegraNegocioException
else status RECEBIDA
    EncomendaService->>EncomendaRepository: save(encomenda RETIRADA)
    EncomendaRepository->>DB: UPDATE
end

EncomendaController-->>Porteiro: 200 OK