# Stage 1: Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine
LABEL authors="Binoz"
LABEL description="Spring Boot Course Application"

WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

# Expose ports
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/api/actuator/health || exit 1

# Environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m"

ENTRYPOINT ["/app/docker-entrypoint.sh"]
