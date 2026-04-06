# ── Build stage ──────────────────────────────────────────────────────────────
FROM node:20-alpine AS builder
WORKDIR /app

# npm ci installs ALL deps (no --production flag, no deprecation warning)
COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM node:20-alpine
WORKDIR /app

# Copy built output + node_modules (vite binary needed for preview)
COPY --from=builder /app/package*.json ./
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/dist ./dist

# Railway injects PORT at runtime; vite.config.ts already reads process.env.PORT
ENV PORT=4173
EXPOSE 4173

CMD ["npm", "run", "preview"]
