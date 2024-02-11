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
      when (transactionRequestDTO.type) {
        "d" -> {
          val nextBalance = it.balance - transactionRequestDTO.value
          if (nextBalance < 0) {
            return OperationResult(HttpStatusCode.UnprocessableEntity) //422
          } else {
            it.balance = nextBalance
            it.transactions += Transaction(
              transactionRequestDTO.value,
              transactionRequestDTO.type,
              transactionRequestDTO.description,
              AuxiliryDate.formatToString(System.currentTimeMillis())
            )
            return OperationResult(HttpStatusCode.OK, TransactionResponseDTO(it.limit, it.balance)) //200
          }
        }

        "c" -> {
          it.balance += transactionRequestDTO.value
          it.transactions += Transaction(
            transactionRequestDTO.value,
            transactionRequestDTO.type,
            transactionRequestDTO.description,
            AuxiliryDate.formatToString(System.currentTimeMillis())
          )
          return OperationResult(HttpStatusCode.OK, TransactionResponseDTO(it.limit, it.balance)) // 200
        }

        else -> {}
      }
    }

    return OperationResult(HttpStatusCode.NotFound) // 404
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