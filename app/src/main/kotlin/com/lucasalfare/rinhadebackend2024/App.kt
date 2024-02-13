package com.lucasalfare.rinhadebackend2024

import com.lucasalfare.rinhadebackend2024.database.PostgresDatabase
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

/**
 * Função principal responsável por iniciar o servidor embutido utilizando Netty.
 * Configura o Content Negotiation para fornecer respostas em formato JSON.
 * Define rotas para manipulação de transações e obtenção de extrato bancário.
 */
suspend fun main() {
  PostgresDatabase.initialize(
    address = System.getenv("PG_ADDRESS"),
    databaseName = System.getenv("PG_DATABASE"),
    username = System.getenv("PG_USERNAME"),
    password = System.getenv("PG_PASSWORD")
  )

  embeddedServer(
    factory = Netty,
    port = 9999
  ) {
    install(ContentNegotiation) {
      json(Json {
        prettyPrint = true
        isLenient = false
      })
    }

    routing {
      /**
       * Rota para criação de uma nova transação para um cliente específico.
       * Utiliza o método HTTP POST para enviar os dados da transação no corpo da requisição.
       * Exemplo de uso:
       * ```
       * curl -v -d '{"valor": 1000, "tipo" : "d", "descricao" : "pgto?"}' -H 'Content-Type: application/json' http://localhost:9999/clientes/1/transacoes
       * ```
       */
      post("/clientes/{id}/transacoes") {
        val id = call.parameters["id"]!!.toInt()

        // Neste ponto, é possível que a deserialização automática não
        // funcione caso algum dos parâmetros seja considerado
        // inválido pelo bloco "init" de [TransactionRequestDTO].
        // Por conta disso, nós usamos a função [runCatching] para
        // detectar quando isso eventualmente ocorrer e, assim,
        // retornar o erro correspondente (que será proveniente
        // do bloco "init" de [TransactionRequestDTO].
        // Além disso, podemos obter erros relacionados à má
        // deserialização.
        runCatching {
          val dto = call.receive<TransactionRequestDTO>()
          val myResponse = PostgresDatabase.createTransaction(id, dto)
          call.respond(myResponse.code, myResponse.data)
        }.onFailure {
          buildFailureOutputResponse(it) { code, message -> call.respond(code, message) }
        }
      }

      /**
       * Rota para obtenção do extrato bancário de um cliente específico.
       * Utiliza o método HTTP GET para realizar a requisição.
       * Exemplo de uso:
       * ```
       * curl -v http://localhost:9999/clientes/1/extrato
       * ```
       */
      get("/clientes/{id}/extrato") {
        val id = call.parameters["id"]!!.toInt()
        runCatching {
          val myResponse = PostgresDatabase.getBankStatement(id)
          call.respond(myResponse.code, myResponse.data)
        }.onFailure {
          buildFailureOutputResponse(it) { code, message -> call.respond(code, message) }
        }
      }
    }
  }.start(true)
}

/**
 * Constrói e envia uma resposta de falha para uma solicitação, fornecendo detalhes sobre o erro ocorrido.
 * @param it O Throwable que representa a exceção lançada.
 * @param callback O callback que será invocado para enviar a resposta de falha, contendo o status HTTP e a mensagem.
 */
private suspend fun buildFailureOutputResponse(it: Throwable, callback: suspend (HttpStatusCode, String) -> Unit) {
  // Constrói a mensagem detalhada de erro a ser enviada na resposta.
  val messages = buildString {
    append("\n")
    append("Mensagem de erro: [${it.message}]")
    append("\n")
    append("Mensagem da causa de erro: [${it.cause?.message}]")
    append("\n")
    append("StackTrace:")
    append("\n")
    // Adiciona cada linha do stack trace como uma linha na mensagem.
    it.stackTrace.forEach { se ->
      append("\t")
      append(se)
      append("\n")
    }
    append("\n")
  }

  // Chama o callback fornecido para enviar a resposta de erro com os detalhes apropriados.
  callback(HttpStatusCode.InternalServerError, messages)
}