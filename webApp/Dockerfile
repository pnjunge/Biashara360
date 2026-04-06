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

# Install serve globally
RUN npm install -g serve

# Copy package.json so Railway's npm start override also works
COPY --from=builder /app/package*.json ./
COPY --from=builder /app/dist ./dist

# Railway injects PORT; serve reads it automatically
CMD ["serve", "-s", "dist"]
