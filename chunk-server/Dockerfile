# Using root build context

# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy all pom files first to leverage Docker cache
COPY pom.xml .
COPY client/pom.xml client/
COPY master-server/pom.xml master-server/
COPY chunk-server/pom.xml chunk-server/

# Download dependencies
RUN mvn dependency:go-offline

# Copy all source code
COPY client/src client/src
COPY master-server/src master-server/src
COPY chunk-server/src chunk-server/src

# Build the chunk-server module
RUN mvn -pl chunk-server -am clean package

# Package stage
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY --from=build /app/chunk-server/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java","-jar","app.jar"]
