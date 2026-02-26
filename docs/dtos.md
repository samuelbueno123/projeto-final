classDiagram

class RegistrarEncomendaRequestDTO {
  Long unidadeId
  String descricao
  String destinatario
}

class EncomendaResponseDTO {
  Long id
  String descricao
  String status
  LocalDateTime dataRecebimento
}

class RegistrarRetiradaRequestDTO {
  Long encomendaId
}