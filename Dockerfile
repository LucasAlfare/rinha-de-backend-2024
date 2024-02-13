FROM openjdk:17
LABEL authors="Francisco Lucas"
RUN mkdir /app
COPY ./app/build/libs/app-all.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]