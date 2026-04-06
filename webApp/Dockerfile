# ── Build stage ──────────────────────────────────────────────────────────────
FROM node:20-alpine AS builder
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM node:20-alpine
WORKDIR /app

# serve automatically reads Railway's injected PORT env var
RUN npm install -g serve

COPY --from=builder /app/dist ./dist

CMD ["serve", "-s", "dist"]
