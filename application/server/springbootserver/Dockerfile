# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /workspace

COPY pom.xml ./
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=builder /workspace/target/*.jar /app/app.jar
COPY config/application.yaml /app/config/application.yml

EXPOSE 8888
ENTRYPOINT ["java","-jar","/app/app.jar"]

