FROM gradle:9.6.0-jdk25 AS builder
WORKDIR /app
COPY . .
RUN gradle :backend:installDist --no-daemon -x test

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /app/backend/build/install/backend /app
COPY backend/entrypoint.sh /app/entrypoint.sh
COPY backend/src/main/resources/application-docker.conf /app/application.conf
RUN mkdir -p uploads config && chmod +x /app/entrypoint.sh

EXPOSE 8080

# Railway sets PORT environment variable
ENV PORT=8080

ENTRYPOINT ["/app/entrypoint.sh"]

