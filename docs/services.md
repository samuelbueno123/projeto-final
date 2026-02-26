classDiagram

class EncomendaService {
  +registrarEncomenda(dto)
  +registrarRetirada(id)
  +listarPorUnidade(unidadeId)
}

class UnidadeService {
  +buscarPorId(id)
  +listarPorCondominio(id)
}

class MoradorService {
  +listarPorUnidade(id)
}