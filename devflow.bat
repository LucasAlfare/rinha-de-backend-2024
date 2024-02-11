@echo off
:: Este script automatiza o fluxo de desenvolvimento do projeto Kotlin.
:: Realiza as seguintes etapas: limpeza, compilação, construção do JAR,
:: e gerenciamento de containers Docker para garantir consistência durante o desenvolvimento.

:: Limpar todos os builds existentes
call gradlew clean

:: Compilar o projeto
call gradlew build

:: Construir o JAR do projeto
call gradlew jar

:: Remover todos os recursos relacionados ao Docker Compose, incluindo volumes
call docker-compose down --volumes --rmi local

:: Remover a imagem Docker existente do projeto
:: call docker rmi rinha-de-backend-2024-image

:: Iniciar containers com Docker Compose para reiniciar o ambiente
call docker-compose up --build