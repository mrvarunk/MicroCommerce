# Multi-stage build for Spring Boot Java 17 application
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy only pom.xml first for better Docker layer caching
COPY pom.xml .
RUN mvn dependency:resolve

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Final stage: lightweight runtime image
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the JAR from builder stage
COPY --from=builder /app/target/stock-service-*.jar app.jar

# Create non-root user for security
RUN addgroup -g 1000 spring && adduser -D -u 1000 -G spring spring
USER spring

# Health check endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
