classDiagram

class Condominio {
  Long id
  String nome
  String cnpj
  LocalDateTime dataCadastro
}

class Setor {
  Long id
  String nome
}

class Unidade {
  Long id
  String identificador
  Boolean ativo
}

class Morador {
  Long id
  String nome
  String cpf
  String email
}

class Usuario {
  Long id
  String login
  String senhaHash
  PerfilUsuario perfil
  Boolean ativo
}

class Encomenda {
  Long id
  String descricao
  String destinatario
  StatusEncomenda status
  LocalDateTime dataRecebimento
  LocalDateTime dataRetirada
}

class StatusEncomenda {
  <<enumeration>>
  RECEBIDA
  RETIRADA
  CANCELADA
}

class PerfilUsuario {
  <<enumeration>>
  ADMINISTRADOR
  SINDICO
  PORTEIRO
  MORADOR
}

Condominio "1" --> "0..*" Setor
Condominio "1" --> "0..*" Unidade
Setor "0..1" --> "0..*" Unidade
Unidade "1" --> "0..*" Encomenda
Morador "0..*" --> "0..*" Unidade