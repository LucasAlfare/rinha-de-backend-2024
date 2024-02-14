#multistage build:
#1) Constroi projeto usando gradle;
#-- move código local para a pasta /tmp do container;
#-- "change directory" (cd) para a pasta acima;
#-- build
#2) Usa o projeto construido para ser devidamente executado

FROM gradle:8.5 AS build
COPY . /tmp
RUN cd /tmp; gradle clean; gradle build; gradle jar --no-daemon

FROM openjdk:17
LABEL authors="Francisco Lucas"
RUN mkdir /app
COPY --from=build ./tmp/app/build/libs/app-all.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]