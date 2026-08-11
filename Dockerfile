# ── Stage 1: Build ────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies first (cached unless pom.xml changes)
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -DskipTests -q

# ── Stage 2: Run ──────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create directory for H2 persistent data
RUN mkdir -p /app/data

# Copy the built JAR
COPY --from=build /app/target/carwash-1.0.0.jar app.jar

# Non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
RUN chown -R spring:spring /app
USER spring

# Expose port (Vercel / Railway / Render read $PORT)
EXPOSE 8080

# Start with env-variable port support
ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dserver.port=${PORT:-8080}", \
  "-jar", "app.jar"]
