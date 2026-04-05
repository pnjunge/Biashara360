# Docker Configuration Fixes

## Problem
The application was failing with:
```
io.ktor.server.config.ApplicationConfigurationException: Property jwt.secret not found.
```

## Root Cause
1. The `.env` file had a truncated `JWT_SECRET` value
2. The `docker-compose.yml` was not using the `.env` file via `env_file` directive
3. Missing environment variable mappings for CyberSource, KRA, Social, and other services

## Solutions Applied

### 1. Updated `.env` file
- Fixed the truncated `JWT_SECRET` to include a complete long string suitable for development/testing
- Already includes all required environment variables for the backend services

### 2. Updated `docker-compose.yml`
Added the following improvements:
- Added `env_file: - .env` to load environment variables from the `.env` file
- Updated `JWT_SECRET` to use `${JWT_SECRET:-default_value}` pattern to read from `.env` first
- Updated `DB_USER` and `DB_PASSWORD` to match database config (postgres/postgres)
- Added fallback environment variables for all services:
  - MPESA (M-Pesa payment integration)
  - CyberSource (payment gateway)
  - KRA eTIMS (tax system)
  - Social Commerce (Meta, TikTok, Claude)

## How It Works
1. Docker Compose reads the `.env` file before starting containers
2. Environment variables from `.env` are passed to the application container
3. The application (via `entrypoint.sh`) exports these variables
4. Ktor's config system (`application-docker.conf`) reads these variables using the `${?VAR_NAME}` syntax
5. If a variable is not set, the default value in `docker-compose.yml` is used

## Testing the Fix

To rebuild and restart with the fixed configuration:

```bash
cd backend
docker-compose down
docker-compose up --build
```

The application should now start without the JWT secret error.

## Production Deployment
For production, make sure to:
1. Set strong, unique values for all secret environment variables
2. Use a secure `.env.production` file or environment management service
3. Never commit production `.env` files to version control
4. Update the `JWT_SECRET` to a cryptographically secure random string (at least 32 characters)

