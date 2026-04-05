FROM gradle:8.8-jdk21 AS builder
WORKDIR /app
COPY backend/build.gradle.kts ./
COPY gradle.properties ./
COPY gradle ./gradle
COPY backend/src ./src
RUN gradle buildFatJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*-all.jar app.jar
COPY backend/entrypoint.sh /app/entrypoint.sh
COPY backend/src/main/resources/application-docker.conf /app/application.conf
RUN mkdir -p uploads config && chmod +x /app/entrypoint.sh

EXPOSE 8080

# Railway sets PORT environment variable
ENV PORT=8080

ENTRYPOINT ["/app/entrypoint.sh"]

