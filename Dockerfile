# Build stage
FROM eclipse-temurin:25-jdk-jammy AS builder
WORKDIR /build
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:25-jre-jammy
WORKDIR /app

# Create app user for security
RUN useradd -m -u 1001 appuser

COPY --from=builder /build/target/patrimonial-api-*.jar app.jar
RUN chown -R appuser:appuser /app

USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0"]
