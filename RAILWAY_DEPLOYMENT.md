# Railway Deployment Guide for Biashara360 Backend

## Prerequisites

1. Railway account (https://railway.app)
2. Railway CLI installed
3. Git initialized and committed code

## Setup Steps

### 1. Install Railway CLI

```bash
npm install -g @railway/cli
# OR
curl -fsSL https://railway.app/install.sh | bash
```

### 2. Initialize Railway Project

```bash
cd /path/to/b360-complete
railway init
# Select "Create a new project"
# Give it a name like "biashara360-backend"
```

### 3. Configure Environment Variables

Set the required environment variables in Railway dashboard:

```bash
railway variables add DATABASE_URL postgresql://user:password@host:port/biashara360
railway variables add DB_USER biashara360
railway variables add DB_PASSWORD <your-secure-password>
railway variables add JWT_SECRET <your-jwt-secret-key>
railway variables add MPESA_CONSUMER_KEY <key>
railway variables add MPESA_CONSUMER_SECRET <secret>
railway variables add MPESA_SHORT_CODE <code>
railway variables add MPESA_PASS_KEY <key>
railway variables add CS_MERCHANT_ID <id>
railway variables add CS_MERCHANT_KEY_ID <key-id>
railway variables add CS_MERCHANT_SECRET_KEY <secret>
railway variables add KRA_ETIMS_ENV sandbox
```

### 4. Add PostgreSQL Plugin (Optional in Railway)

Railway can automatically provision PostgreSQL. Use Railway dashboard:

1. Go to project settings
2. Click "Add Plugin"
3. Select PostgreSQL
4. Railway will auto-configure DATABASE_URL

### 5. Deploy

```bash
# Add all files to git
git add .
git commit -m "Initial commit for Railway deployment"

# Deploy to Railway
railway up

# View logs
railway logs

# Open dashboard
railway open
```

### 6. Get Your URL

```bash
railway env
# Copy the RAILWAY_PUBLIC_DOMAIN or custom domain
```

## Environment Variables for Railway

### Required

```
DATABASE_URL=postgresql://user:password@db-host:5432/biashara360
DB_USER=biashara360
DB_PASSWORD=<secure-password>
JWT_SECRET=<64+ character random string>
PORT=8080 (Railway sets this automatically)
```

### Optional (with defaults)

```
MPESA_CONSUMER_KEY=your_key
MPESA_CONSUMER_SECRET=your_secret
MPESA_SHORT_CODE=174379
MPESA_PASS_KEY=your_pass_key
MPESA_ENV=sandbox
CS_MERCHANT_ID=your_id
CS_MERCHANT_KEY_ID=your_key_id
CS_MERCHANT_SECRET_KEY=your_secret
CS_ENVIRONMENT=sandbox
KRA_ETIMS_ENV=sandbox
```

## Production Configuration

For production deployment:

1. **Update JWT_SECRET**: Use a strong, randomly generated 64+ character string
2. **Use production databases**: Configure real PostgreSQL (Railway provides managed DB)
3. **SSL/HTTPS**: Railway automatically provides SSL
4. **Custom Domain**: Add your domain in Railway dashboard
5. **Monitoring**: Enable Railway's monitoring and alerts

## Build Configuration

The deployment uses:
- **Dockerfile**: `backend/Dockerfile` (multi-stage build)
- **Build Tool**: Gradle (via gradle wrapper)
- **Java Version**: Eclipse Temurin 21 JRE Alpine
- **Port**: 8080 (exposed by API)

## Deployment Process

1. Railway detects the Dockerfile in `backend/` directory
2. Builds the Docker image:
   - Stage 1: Compiles code with Gradle
   - Stage 2: Runs compiled JAR
3. Deploys container to Railway infrastructure
4. Routes traffic to port 8080

## Monitoring & Debugging

```bash
# View real-time logs
railway logs --tail

# SSH into running container
railway shell

# View deployment status
railway status

# View environment variables
railway variables

# Restart service
railway restart
```

## Common Issues & Solutions

### Database Connection Issues

```bash
# Verify DATABASE_URL format
railway variables

# Check database logs
railway logs

# Connect to PostgreSQL directly
railway shell
psql $DATABASE_URL
```

### Port Issues

Railway automatically sets the `PORT` environment variable. The application listens on the port specified in the environment or defaults to 8080.

### Build Failures

Check build logs:
```bash
railway logs --build
```

### Memory Issues

Railway allocates resources based on your plan. If out of memory, upgrade your plan or optimize the application.

## Scaling

To scale the application:

1. Go to Railway dashboard
2. Select your service
3. Adjust replica count and resources
4. Railway will handle load balancing

## Removing the Application

```bash
railway remove
# Confirms and removes all resources
```

## Additional Resources

- Railway Docs: https://docs.railway.app
- Railway CLI: https://docs.railway.app/cli/commands
- Docker best practices: https://docs.docker.com

## Support

For Railway-specific issues: https://railway.app/support
For application issues: Check logs with `railway logs`

