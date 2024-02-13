package com.lucasalfare.rinhadebackend2024.database

import org.jetbrains.exposed.dao.id.IntIdTable

/**
 * Tabela que representa os clientes no banco de dados. Estende IntIdTable.
 */
object ClientsTable : IntIdTable("clients") {
  /** Limite de saldo do cliente. */
  var limit = integer("limit")

  /** Saldo atual do cliente. */
  val balance = integer("balance")
}

/**
 * Tabela que representa as transações no banco de dados. Estende IntIdTable.
 */
object TransactionsTable : IntIdTable("transactions") {
  /** O valor da transação. */
  var value = integer("value")

  /** O tipo da transação (por exemplo, "d" para débito, "c" para crédito). */
  var type = varchar(name = "type", length = 1)

  /** A descrição da transação. */
  var description = varchar(name = "description", length = 10)

  /** A data da transação em milissegundos desde a época UNIX. */
  var date = long("date")

  /** O ID do cliente associado a esta transação, referenciando a tabela ClientsTable. */
  var clientId = integer("client_id").references(ClientsTable.id)
}