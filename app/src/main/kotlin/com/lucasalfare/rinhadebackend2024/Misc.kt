package com.lucasalfare.rinhadebackend2024

import java.text.SimpleDateFormat
import java.util.*

/**
 * Um objeto singleton para formatar datas e horas em uma representação de string específica.
 */
object Formatter {

  // O formato de data e hora desejado: "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
  // Criamos um objeto SimpleDateFormat para definir o formato exato de data e hora desejado.
  // "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" segue o padrão ISO 8601 comum em sistemas de comunicação entre sistemas e armazenamento de data e hora.
  // Locale.US é usado para interpretar o formato de acordo com as convenções de data e hora dos Estados Unidos.
  private val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

  /**
   * Formata um valor de data e hora em milissegundos para uma string no formato desejado.
   *
   * @param dateTime o valor de data e hora em milissegundos a ser formatado.
   * @return uma string formatada representando a data e hora no formato "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'".
   */
  fun format(dateTime: Long): String = sdf.format(dateTime)
}