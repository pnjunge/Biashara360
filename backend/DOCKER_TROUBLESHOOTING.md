# Docker Setup and Troubleshooting Guide

## Issues Fixed

### 1. ✅ Removed Obsolete Version Attribute
The `version: '3.8'` line has been removed from `docker-compose.yml` as it's deprecated and will be ignored by newer Docker Compose versions.

### 2. ✅ Fixed JWT Secret Configuration
- Updated `.env` file with a complete `JWT_SECRET` value
- Updated `docker-compose.yml` to properly load the `.env` file via `env_file` directive
- Added all missing environment variables for services

## Docker Permission Issue

If you encounter the error:
```
Error response from daemon: cannot stop container: ...: permission denied
```

This typically happens when:
1. The Docker daemon has stale permissions on containers
2. The Docker socket has ownership issues

### Solution: Clean Restart

Run these commands in sequence:

```bash
# 1. Stop all docker containers completely
docker-compose down --remove-orphans 2>&1 | cat || true

# 2. If stuck, wait and try force removal
sleep 5
docker container prune -f 2>&1 | cat || true

# 3. Restart Docker daemon
sudo systemctl restart docker
sleep 3

# 4. Verify containers are gone
docker ps -a

# 5. Rebuild from scratch
docker-compose up --build
```

### Alternative: Complete Docker Reset (Nuclear Option)

If the above doesn't work:

```bash
# WARNING: This will remove ALL containers and volumes!
sudo systemctl stop docker
sudo rm -rf /var/lib/docker/containers/*
sudo systemctl start docker
sleep 5

# Then start fresh
cd /home/sirpatrick/Downloads/biashara360-COMPLETE\(8\)/b360-complete/backend
docker-compose up --build
```

## Configuration Files Updated

### docker-compose.yml Changes:
- ✅ Removed obsolete `version: '3.8'` line
- ✅ Added `env_file: - .env` to load environment variables
- ✅ Updated `JWT_SECRET` to use `${JWT_SECRET:-default}`
- ✅ Added all required environment variables for:
  - Database (PostgreSQL)
  - JWT/Authentication
  - M-Pesa payments
  - CyberSource gateway
  - KRA eTIMS
  - Social commerce (Meta, TikTok, Claude)

### .env File Changes:
- ✅ Fixed truncated `JWT_SECRET` to include complete valid token

## Environment Variables Available

All these can be configured in the `.env` file:

| Variable | Purpose | Default |
|----------|---------|---------|
| JWT_SECRET | JWT signing key | `biashara360-super-secret-jwt-key...` |
| DATABASE_URL | PostgreSQL connection | `jdbc:postgresql://localhost:5432/biashara360` |
| DB_USER | Database user | `postgres` |
| DB_PASSWORD | Database password | `postgres` |
| MPESA_CONSUMER_KEY | M-Pesa API key | `your_consumer_key` |
| MPESA_CONSUMER_SECRET | M-Pesa API secret | `your_consumer_secret` |
| MPESA_SHORT_CODE | M-Pesa short code | `174379` |
| MPESA_PASS_KEY | M-Pesa pass key | `your_pass_key` |
| MPESA_ENV | M-Pesa environment | `sandbox` |
| CS_MERCHANT_ID | CyberSource merchant ID | `your_merchant_id` |
| CS_MERCHANT_KEY_ID | CyberSource key ID | `your_key_id` |
| CS_MERCHANT_SECRET_KEY | CyberSource secret | `your_shared_secret` |
| CS_ENVIRONMENT | CyberSource env | `sandbox` |
| KRA_ETIMS_ENV | KRA environment | `sandbox` |
| META_APP_ID | Meta app ID | `your_meta_app_id` |
| META_APP_SECRET | Meta app secret | `your_meta_app_secret` |
| TIKTOK_CLIENT_KEY | TikTok client key | `your_tiktok_client_key` |
| TIKTOK_CLIENT_SECRET | TikTok secret | `your_tiktok_client_secret` |
| ANTHROPIC_API_KEY | Claude AI API key | `your_anthropic_api_key` |
| WEBHOOK_BASE_URL | Social webhook URL | `https://your-domain.com/v1/social/webhook` |

## Testing the Build

Once containers are cleaned up, test with:

```bash
cd backend
docker-compose up --build

# In another terminal, test the API:
curl http://localhost:8080/health
```

You should see the application start without JWT secret errors.

