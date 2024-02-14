package com.lucasalfare.rinhadebackend2024.database

import com.lucasalfare.rinhadebackend2024.*
import com.lucasalfare.rinhadebackend2024.Transaction
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.util.IsolationLevel
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Objeto singleton para interagir com o banco de dados PostgreSQL.
 */
object PostgresDatabase {

  // Fonte de dados Hikari que será inicializada posteriormente
  private lateinit var hikariDataSource: HikariDataSource

  /**
   * Inicializa a conexão com o banco de dados PostgreSQL.
   * @param address O endereço do banco de dados.
   * @param databaseName O nome do banco de dados.
   * @param username O nome de usuário para autenticação.
   * @param password A senha para autenticação.
   */
  suspend fun initialize(
    address: String,
    databaseName: String,
    username: String,
    password: String
  ) {
    // Cria a fonte de dados Hikari com as credenciais fornecidas
    hikariDataSource = createHikariDataSource(
      jdbcUrl = "jdbc:postgresql://$address/$databaseName",
      username = username,
      password = password
    )

    // Cria as tabelas necessárias se elas ainda não existirem
    transaction(Database.connect(hikariDataSource)) {
      SchemaUtils.createMissingTablesAndColumns(ClientsTable, TransactionsTable)
    }

    // Insere dados de clientes padrão para teste
    dbQuery { ClientsTable.insertIgnore { it[id] = 1; it[limit] = 100000; it[balance] = 0 } }
    dbQuery { ClientsTable.insertIgnore { it[id] = 2; it[limit] = 80000; it[balance] = 0 } }
    dbQuery { ClientsTable.insertIgnore { it[id] = 3; it[limit] = 1000000; it[balance] = 0 } }
    dbQuery { ClientsTable.insertIgnore { it[id] = 4; it[limit] = 10000000; it[balance] = 0 } }
    dbQuery { ClientsTable.insertIgnore { it[id] = 5; it[limit] = 500000; it[balance] = 0 } }
  }

  /**
   * Cria uma nova transação para um cliente específico.
   * @param id O ID do cliente.
   * @param transactionRequestDTO Os detalhes da transação a ser realizada.
   * @return Um objeto OperationResult representando o resultado da operação.
   */
  suspend fun createTransaction(
    id: Int,
    transactionRequestDTO: TransactionRequestDTO
  ): OperationResult {
    // Verifica se o cliente existe para executar as operações
    dbQuery { ClientsTable.select { ClientsTable.id eq id }.singleOrNull() }?.let { search ->
      when (transactionRequestDTO.type) {
        "d" -> {
          val nextBalance = search[ClientsTable.balance] - transactionRequestDTO.value
          // TODO: Verifica se a transação excede o limite de saldo disponível
          if (nextBalance < 0) {
            return OperationResult(code = HttpStatusCode.UnprocessableEntity)
          } else {
            // Atualiza o saldo do cliente e insere a transação
            dbQuery {
              ClientsTable.update({ ClientsTable.id eq search[ClientsTable.id] }) {
                it[balance] = nextBalance
              }
            }

            dbQuery {
              TransactionsTable.insert {
                it[value] = transactionRequestDTO.value
                it[type] = transactionRequestDTO.type
                it[description] = transactionRequestDTO.description
                it[date] = System.currentTimeMillis()
                it[clientId] = search[ClientsTable.id].value
              }
            }

            // Retorna o resultado da operação com os detalhes da transação
            return OperationResult(
              code = HttpStatusCode.OK,
              data = TransactionResponseDTO(search[ClientsTable.limit], nextBalance)
            )
          }
        }

        "c" -> {
          val nextBalance = search[ClientsTable.balance] + transactionRequestDTO.value
          // Atualiza o saldo do cliente e insere a transação
          dbQuery {
            ClientsTable.update({ ClientsTable.id eq search[ClientsTable.id] }) {
              it[balance] = nextBalance
            }
          }

          dbQuery {
            TransactionsTable.insert {
              it[value] = transactionRequestDTO.value
              it[type] = transactionRequestDTO.type
              it[description] = transactionRequestDTO.description
              it[date] = System.currentTimeMillis()
              it[clientId] = search[ClientsTable.id].value
            }
          }

          // Retorna o resultado da operação com os detalhes da transação
          return OperationResult(
            code = HttpStatusCode.OK,
            data = TransactionResponseDTO(search[ClientsTable.limit], nextBalance)
          )
        }

        else -> {}
      }
    }

    // Retorna um resultado de operação de não encontrado se o cliente não existe
    return OperationResult(code = HttpStatusCode.NotFound)
  }

  /**
   * Obtém o extrato bancário de um cliente específico.
   * @param id O ID do cliente.
   * @return Um objeto OperationResult representando o resultado da operação.
   */
  suspend fun getBankStatement(id: Int): OperationResult {
    // Verifica se o cliente existe
    dbQuery { ClientsTable.select { ClientsTable.id eq id }.singleOrNull() }?.let { search ->
      val result = BankStatementResponseDTO(
        balance = Balance(
          total = search[ClientsTable.balance],
          bankStatementDate = AuxiliryDate.formatToString(System.currentTimeMillis()),
          limit = search[ClientsTable.limit]
        ),
        lastTransactions = dbQuery {
          // Obtém as últimas transações do cliente
          TransactionsTable.select { TransactionsTable.clientId eq id }.orderBy(TransactionsTable.date).limit(10).map {
            Transaction(
              value = it[TransactionsTable.value],
              type = it[TransactionsTable.type],
              description = it[TransactionsTable.description],
              date = AuxiliryDate.formatToString(it[TransactionsTable.date])
            )
          }
        }
      )

      // Retorna o resultado da operação com o extrato bancário
      return OperationResult(code = HttpStatusCode.OK, data = result)
    }

    // Retorna um resultado de operação de não encontrado se o cliente não existe
    return OperationResult(code = HttpStatusCode.NotFound)
  }

  /**
   * Executa uma operação de banco de dados de forma assíncrona.
   * @param block O bloco de código a ser executado.
   * @return O resultado da operação.
   */
  private suspend fun <T> dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(
      context = Dispatchers.IO,
      db = Database.connect(hikariDataSource)
    ) {
      block()
    }

  /**
   * Cria e configura uma fonte de dados Hikari para o banco de dados PostgreSQL.
   * @param jdbcUrl A URL JDBC para a conexão com o banco de dados.
   * @param username O nome de usuário para autenticação.
   * @param password A senha para autenticação.
   * @return Uma instância de HikariDataSource configurada.
   */
  private fun createHikariDataSource(
    jdbcUrl: String,
    username: String,
    password: String
  ): HikariDataSource {
    val hikariConfig = HikariConfig().apply {
      this.jdbcUrl = jdbcUrl
      // Sempre usando PostgreSQL, então o driverClassName é fixo aqui
      this.driverClassName = "org.postgresql.Driver"
      this.username = username
      this.password = password
      this.maximumPoolSize = 20
      this.isAutoCommit = true
      this.transactionIsolation = IsolationLevel.TRANSACTION_READ_COMMITTED.name
      this.validate()
    }

    // Criando uma nova instância de HikariDataSource usando o HikariConfig configurado
    return HikariDataSource(hikariConfig)
  }
}