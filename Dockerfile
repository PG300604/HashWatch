# Stage 1: Build JAR with Maven & Eclipse Temurin 21
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build application
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal Production JRE Runtime
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

# Create non-root system user for security compliance
RUN addgroup -S hashwatch && adduser -S hashwatch -G hashwatch

# Create directories for keys and watched files with proper ownership
RUN mkdir -p /app/keys /app/watched_files /app/logs && \
    chown -R hashwatch:hashwatch /app

COPY --from=builder --chown=hashwatch:hashwatch /build/target/hashwatch-*.jar app.jar

USER hashwatch

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/api/files || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
