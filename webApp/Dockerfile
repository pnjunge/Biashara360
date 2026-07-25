FROM node:20-alpine AS builder
WORKDIR /app

ARG VITE_API_BASE_URL=https://sddgmezqj2.us-east-1.awsapprunner.com/v1
ARG VITE_META_APP_ID
ARG VITE_META_EMBEDDED_SIGNUP_CONFIG_ID
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_META_APP_ID=$VITE_META_APP_ID
ENV VITE_META_EMBEDDED_SIGNUP_CONFIG_ID=$VITE_META_EMBEDDED_SIGNUP_CONFIG_ID

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]
