classDiagram

class EncomendaController {
  +registrarEncomenda(dto)
  +registrarRetirada(id)
  +listarPorUnidade(unidadeId)
}

class UnidadeController {
  +listarUnidades()
}

class MoradorController {
  +listarMoradoresPorUnidade(id)
}