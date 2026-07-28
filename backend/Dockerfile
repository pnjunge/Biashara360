FROM gradle:8.7-jdk21 AS builder
WORKDIR /app
COPY . .
RUN ./gradlew installDist -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/backend/build/install/backend /app
EXPOSE 8080
ENTRYPOINT ["/app/bin/backend"]
