# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:17-jre-alpine AS runtime

RUN addgroup -S aplicacao \
    && adduser -S -G aplicacao aplicacao

WORKDIR /app

COPY --from=build --chown=aplicacao:aplicacao /workspace/target/sistemabancario-*.jar /app/app.jar

USER aplicacao

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
