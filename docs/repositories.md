classDiagram

class CondominioRepository {
  +save()
  +findById()
}

class UnidadeRepository {
  +save()
  +findById()
  +findByCondominioId()
}

class MoradorRepository {
  +save()
  +findById()
}

class EncomendaRepository {
  +save()
  +findById()
  +findByUnidadeId()
}