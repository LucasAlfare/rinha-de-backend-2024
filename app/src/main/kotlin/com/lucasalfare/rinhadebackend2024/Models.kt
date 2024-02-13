package com.lucasalfare.rinhadebackend2024

import io.ktor.http.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Representa um DTO (Data Transfer Object) de solicitação de transação.
 * @property value O valor da transação.
 * @property type O tipo da transação (seja "c" para crédito ou "d" para débito).
 * @property description Uma breve descrição da transação.
 */
@Serializable
data class TransactionRequestDTO(
  @SerialName("valor") val value: Int,
  @SerialName("tipo") val type: String,
  @SerialName("descricao") val description: String
) {

  /**
   * Nós validamos os dados dessa requisição no momento de
   * criação da mesma.
   */
  init {
    require(value >= 0) { "O valor da transação deve ser não negativo." }
    require(type == "c" || type == "d") { "O tipo de transação deve ser 'c' ou 'd'." }
    require(description.length in 1..10) { "O comprimento da descrição deve estar entre 1 e 10 caracteres." }
  }
}

/**
 * Representa um DTO (Data Transfer Object) de resposta de transação.
 * @property limit O limite de crédito.
 * @property balance O saldo atual.
 */
@Serializable
data class TransactionResponseDTO(
  @SerialName("limite") val limit: Int,
  @SerialName("saldo") val balance: Int
)

/**
 * Representa o saldo de uma conta bancária.
 * @property total O saldo total.
 * @property bankStatementDate A data do extrato bancário.
 * @property limit O limite de crédito associado à conta.
 */
@Serializable
data class Balance(
  @SerialName("total") val total: Int,
  @SerialName("data_extrato") val bankStatementDate: String,
  @SerialName("limite") val limit: Int
)

/**
 * Representa uma transação bancária.
 * @property value O valor da transação.
 * @property type O tipo da transação.
 * @property description A descrição da transação.
 * @property date A data em que a transação foi realizada.
 */
@Serializable
data class Transaction(
  @SerialName("valor") val value: Int,
  @SerialName("tipo") val type: String,
  @SerialName("descricao") val description: String,
  @SerialName("realizada_em") val date: String
)

/**
 * Representa o extrato bancário de um cliente.
 * @property balance O saldo da conta bancária.
 * @property lastTransactions As últimas transações realizadas.
 */
@Serializable
data class BankStatementResponseDTO(
  @SerialName("saldo") val balance: Balance,
  @SerialName("ultimas_transacoes") val lastTransactions: List<Transaction>
)

/**
 * Representa o resultado de uma operação.
 * @property code O código de status HTTP da operação.
 * @property data Os dados associados ao resultado da operação (o valor padrão é "Sem dados").
 */
@Serializable
data class OperationResult(
  @Contextual val code: HttpStatusCode,
  @Contextual val data: Any = "Sem dados"
)