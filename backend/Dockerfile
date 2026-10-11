FROM gradle:9.6.0-jdk25 AS builder
WORKDIR /app
COPY . .
RUN gradle installDist -x test

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/install/backend /app
EXPOSE 8080
ENTRYPOINT ["/app/bin/backend"]
