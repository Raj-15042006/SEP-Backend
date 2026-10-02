# Multi-stage container build for Spring Boot backend (Optimized for Render)
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Install bash (required by gradlew script on Alpine)
RUN apk add --no-cache bash

# Copy Gradle wrapper and configuration files first for Docker layer caching
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Fix any Windows CRLF carriage returns and ensure execute permissions
RUN sed -i 's/\r$//' ./gradlew && chmod +x ./gradlew

# Copy source code and build runnable fat JAR (skip tests during container packaging)
COPY src src
RUN bash ./gradlew bootJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

# Render assigns a dynamic port via the PORT environment variable
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]
