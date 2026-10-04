# Stage 1: Builder
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace

# Copy gradle wrapper and build files
COPY gradle/ gradle/
COPY gradlew .
COPY gradlew.bat .
COPY gradle.properties .
COPY settings.gradle.kts .
COPY build.gradle.kts .

# Copy source code
COPY synopsi-api/src synopsi-api/src
COPY synopsi-api/build.gradle.kts synopsi-api/

# Build the application
RUN ./gradlew bootJar -p synopsi-api --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

# Create non-root user for security
RUN addgroup -g 1000 synopsi && \
    adduser -D -u 1000 -G synopsi synopsi

WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder --chown=synopsi:synopsi /workspace/synopsi-api/build/libs/*.jar app.jar

# Switch to non-root user
USER synopsi

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
