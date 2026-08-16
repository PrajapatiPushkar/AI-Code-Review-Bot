# Multi-stage Dockerfile for AI Code Review Bot Backend (Spring Boot + Java 21)

# Stage 1: Build stage
FROM maven:3.9.8-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy pom.xml and pre-fetch dependencies for efficient Docker layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build executable JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Security: Create non-root system user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy built application JAR from builder stage
COPY --from=builder /app/target/ai-code-review-bot-0.0.1-SNAPSHOT.jar app.jar

# Set directory ownership to non-root user
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
