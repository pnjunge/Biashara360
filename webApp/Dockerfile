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

COPY --from=builder /app/package*.json ./
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/dist ./dist

# Pass host and port directly via CLI — no vite.config.ts needed at runtime
# PORT is injected by Railway; default 4173 for local preview
EXPOSE 4173
CMD sh -c "node node_modules/.bin/vite preview --host 0.0.0.0 --port ${PORT:-4173}"
