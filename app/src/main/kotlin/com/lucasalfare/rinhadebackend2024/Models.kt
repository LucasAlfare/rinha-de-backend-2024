package com.lucasalfare.rinhadebackend2024

import io.ktor.http.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Representa uma transação financeira.
 * @property value O valor da transação.
 * @property type O tipo da transação (seja "c" para crédito ou "d" para débito).
 * @property description Uma breve descrição da transação.
 * @property performedAtDate A data em que a transação foi realizada.
 */
@Serializable
data class Transaction(
  @SerialName("valor") val value: Int,
  @SerialName("tipo") val type: String,
  @SerialName("descricao") val description: String,
  // TODO: considerar usar, por padrão, diretamente a data em milissegundos
  // TODO: criar um tipo "Transaction" extra que contém esse campo em String
  @SerialName("realizada_em") val performedAtDate: String
)

/**
 * Representa um cliente no sistema bancário.
 * @property id O identificador único do cliente.
 * @property limit O limite de crédito para o cliente.
 * @property balance O saldo atual do cliente.
 */
@Serializable
data class Client(
  val id: Int,
  @SerialName("limite") val limit: Int,
  @SerialName("saldo") var balance: Int
) {
  val transactions = mutableListOf<Transaction>()
}

/**
 * Representa as informações de saldo.
 * @property total O saldo total.
 * @property bankStatementDate A data do extrato bancário.
 * @property limit O limite de crédito.
 */
@Serializable
data class Balance(
  val total: Int,
  @SerialName("data_extrato") val bankStatementDate: String,
  @SerialName("limite") val limit: Int
)

/**
 * Representa um extrato bancário, incluindo o saldo e as últimas transações.
 * @property balance As informações de saldo.
 * @property lastTransactions A lista das últimas transações.
 */
@Serializable
data class BankStatement(
  @SerialName("saldo") val balance: Balance,
  @SerialName("ultimas_transacoes") val lastTransactions: MutableList<Transaction>
) {

  init {
    // o excesso de conversão de String para Long é possivelmente um desperdício
    lastTransactions.sortBy { AuxiliryDate.formatToMilliseconds(it.performedAtDate) }
  }
}

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
 * Representa o resultado de uma operação.
 * @property code O código de status HTTP da operação.
 * @property data Os dados associados ao resultado da operação (o valor padrão é "Sem dados").
 */
@Serializable
data class OperationResult(
  @Contextual val code: HttpStatusCode,
  @Contextual val data: Any = "Sem dados"
)