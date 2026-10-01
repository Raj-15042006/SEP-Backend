# Multi-stage container build for Spring Boot backend (Optimized for Render)
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Copy Gradle wrapper and configuration files first for Docker layer caching
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x ./gradlew

# Copy source code and build runnable fat JAR (skip tests during container packaging)
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

# Render assigns a dynamic port via the PORT environment variable
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]
