# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY batch-writer/pom.xml batch-writer/
COPY batch-writer/src batch-writer/src
RUN mvn clean package -pl batch-writer -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/batch-writer/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]