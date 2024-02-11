package com.lucasalfare.rinhadebackend2024

import io.ktor.http.*

/**
 * Objeto responsável por gerenciar o acesso aos dados do banco de dados simulado.
 */
object InMemoryDatabase {

  // Lista de clientes simulados com seus limites e saldos iniciais.
  private val clients = mutableListOf(
    Client(id = 1, limit = 100000, balance = 0),
    Client(id = 2, limit = 80000, balance = 0),
    Client(id = 3, limit = 1000000, balance = 0),
    Client(id = 4, limit = 10000000, balance = 0),
    Client(id = 5, limit = 500000, balance = 0)
  )

  /**
   * Cria uma nova transação para um cliente específico com base nos dados fornecidos.
   * @param id O ID do cliente.
   * @param transactionRequestDTO Os dados da transação a ser criada.
   * @return O resultado da operação, indicando sucesso ou falha.
   */
  fun createTransaction(
    id: Int,
    transactionRequestDTO: TransactionRequestDTO
  ): OperationResult {
    clients.find { it.id == id }?.let {
      // nós tentamos retirar o valor da transação do saldo atual
      val nextBalance = it.balance - transactionRequestDTO.value

      // nós verificamos se o saldo de teste é maior que 0,
      // ou seja, se o valor da transação não fez com que
      // o saldo atual fique negativo
      if (nextBalance >= 0) {
        // se o valor de teste estiver maior ou igual a 0
        // então nós atualizamos o saldo do cliente com
        // esse novo valor
        it.balance = nextBalance

        // adicionamos um novo resgistro de transação ao
        // cliente atual
        it.transactions += Transaction(
          transactionRequestDTO.value,
          transactionRequestDTO.type,
          transactionRequestDTO.description,
          AuxiliryDate.formatToString(System.currentTimeMillis())
        )

        // retornamos a operação indicando sucesso, juntamente
        // com o DTO no formato requerido pela especificação
        // da Rinha 2024
        return OperationResult(HttpStatusCode.OK, TransactionResponseDTO(it.limit, it.balance))
      } else {
        // quando não for possível retirar o valor da transação
        // do valor de saldo atual, retornamos 422, sem alterar
        // nada
        return OperationResult(HttpStatusCode.UnprocessableEntity)
      }
    }

    // caso não tenha sido possível localizar nenhum usuário com
    // ID igual ao solicitado, retornamos 404
    return OperationResult(HttpStatusCode.NotFound)
  }

  /**
   * Obtém o extrato bancário de um cliente específico.
   * @param id O ID do cliente.
   * @return O resultado da operação, contendo o extrato bancário ou indicando falha.
   */
  fun getBankStatement(id: Int): OperationResult {
    clients.find { it.id == id }?.let {
      val bankStatement = BankStatement(
        Balance(
          total = it.balance,
          bankStatementDate = AuxiliryDate.formatToString(System.currentTimeMillis()),
          limit = it.limit
        ),
        it.transactions.takeLast(10) as MutableList<Transaction>
      )

      return OperationResult(HttpStatusCode.OK, bankStatement)
    }

    return OperationResult(HttpStatusCode.NotFound)
  }
}