package com.lucasalfare.rinhadebackend2024

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

fun main() {
  embeddedServer(Netty, 9999) {
    install(ContentNegotiation) {
      json(Json {
        prettyPrint = true
        isLenient = false
      })
    }

    routing {
      /*
      curl -v -d '{"valor": 1000, "tipo" : "d", "descricao" : "pgto?"}' -H 'Content-Type: application/json' http://localhost:9999/clientes/1/transacoes
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
        // deserrialização.
        runCatching {
          val dto = call.receive<TransactionRequestDTO>()
          val myResponse = Database.createTransaction(id, dto)
          call.respond(myResponse.code, myResponse.data)
        }.onFailure {
          call.respond(HttpStatusCode.BadRequest, it.cause?.message ?: "")
        }
      }

      /*
      curl http://localhost:9999/clientes/1/extrato
       */
      get("/clientes/{id}/extrato") {
        val id = call.parameters["id"]!!.toInt()
        val myResponse = Database.getBankStatement(id)

        call.respond(myResponse.code, myResponse.data)
      }
    }
  }.start(true)
}