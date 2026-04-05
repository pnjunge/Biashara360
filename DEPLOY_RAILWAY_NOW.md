# 🚀 DEPLOY TO RAILWAY - ACTION GUIDE

## STEP-BY-STEP INSTRUCTIONS

### ✅ Step 1: Create Railway Account (2 minutes)
```
1. Go to https://railway.app
2. Click "Start Building"
3. Sign up with:
   - GitHub account (recommended), OR
   - Email/Password
4. Verify email
```

### ✅ Step 2: Create New Project (1 minute)
```
1. Click "New Project" in Railway dashboard
2. Select "Deploy from GitHub" (if you have GitHub account)
   - OR select "Create a new service"
3. Name it: "biashara360-backend"
```

### ✅ Step 3: Generate JWT Secret (1 minute)
```bash
# Run this in your terminal
openssl rand -base64 32

# Copy the output - you'll need it in Step 5
```

### ✅ Step 4: Deploy Application (5 minutes)

**Option A: Using Deployment Script (Easiest)**
```bash
cd /home/sirpatrick/Downloads/biashara360-COMPLETE\(8\)/b360-complete
./deploy-railway.sh
# Follow interactive prompts
```

**Option B: Using Railway CLI**
```bash
# Install Railway CLI
npm install -g @railway/cli

# Login to Railway
railway login

# Initialize project
cd /home/sirpatrick/Downloads/biashara360-COMPLETE\(8\)/b360-complete
railway init

# Deploy
railway up
```

**Option C: GitHub Integration (Recommended for continuous deployment)**
```bash
# Push code to GitHub
git remote add origin <your-github-repo-url>
git push -u origin master

# Then in Railway dashboard:
# 1. Click "New Project"
# 2. Select "Deploy from GitHub"
# 3. Select this repository
# 4. Railway auto-deploys on every push
```

### ✅ Step 5: Configure Environment Variables (3 minutes)

In Railway Dashboard:
```
1. Go to your project
2. Click on the API service
3. Go to "Variables" tab
4. Add each variable and click "Add":

REQUIRED:
  JWT_SECRET = <paste from Step 3>
  DATABASE_URL = <from Railway PostgreSQL or your DB>
  DB_USER = biashara360
  DB_PASSWORD = <secure-password>

OPTIONAL:
  MPESA_CONSUMER_KEY = (your key)
  MPESA_CONSUMER_SECRET = (your secret)
  ... (other payment/service keys)
```

### ✅ Step 6: Add PostgreSQL (Optional - Railway provides)

```
In Railway Dashboard:
1. Click "Add service"
2. Select "PostgreSQL"
3. Railway auto-sets DATABASE_URL
4. Use generated credentials
```

### ✅ Step 7: Monitor Deployment

```bash
# View real-time logs
railway logs --tail

# Check deployment status
railway status

# Get public URL
railway open
```

### ✅ Step 8: Access Your API

```
Your API will be available at:
https://<railway-generated-domain>

Test it:
curl https://<your-url>/health

Or check in Railway dashboard:
Project → Service → Deployments → Public Domain
```

---

## 📋 CHECKLIST

- [ ] Created Railway account
- [ ] Created new project
- [ ] Generated JWT secret
- [ ] Deployed application (using one of the 3 options)
- [ ] Configured environment variables
- [ ] Added PostgreSQL (if using Railway DB)
- [ ] Verified deployment successful
- [ ] Tested API endpoints
- [ ] (Optional) Added custom domain
- [ ] (Optional) Configured monitoring/alerts

---

## 🔑 ENVIRONMENT VARIABLES TO SET

Copy-paste these into Railway Variables tab:

```
JWT_SECRET=<from Step 3 - YOUR_GENERATED_SECRET>
DATABASE_URL=<from Railway PostgreSQL or your DB>
DB_USER=biashara360
DB_PASSWORD=<your-secure-password>
MPESA_CONSUMER_KEY=<your-key>
MPESA_CONSUMER_SECRET=<your-secret>
MPESA_SHORT_CODE=174379
MPESA_PASS_KEY=<your-key>
MPESA_ENV=sandbox
CS_MERCHANT_ID=<your-id>
CS_MERCHANT_KEY_ID=<your-key>
CS_MERCHANT_SECRET_KEY=<your-secret>
CS_ENVIRONMENT=sandbox
KRA_ETIMS_ENV=sandbox
META_APP_ID=<your-id>
META_APP_SECRET=<your-secret>
TIKTOK_CLIENT_KEY=<your-key>
TIKTOK_CLIENT_SECRET=<your-secret>
ANTHROPIC_API_KEY=<your-key>
WEBHOOK_BASE_URL=https://your-domain.com/v1/social/webhook
```

---

## 🎯 IF SOMETHING GOES WRONG

### Check Logs
```bash
railway logs --tail
```

### Verify Variables
```bash
railway variables
```

### SSH into Container
```bash
railway shell
# Then check environment
env | grep DATABASE_URL
env | grep JWT_SECRET
```

### Restart Service
```bash
railway restart
```

### Reset Project
```bash
railway down
railway up
```

---

## ✨ AFTER DEPLOYMENT

### 1. Monitor Performance
- Check Railway dashboard regularly
- Enable alerts for errors
- Monitor resource usage

### 2. Set Up Monitoring
```
In Railway Dashboard:
1. Go to Monitoring tab
2. Enable email alerts
3. Set error thresholds
```

### 3. Configure Custom Domain (Optional)
```
In Railway Dashboard:
1. Project settings
2. Add custom domain
3. Update DNS records
4. SSL auto-configured
```

### 4. Enable CI/CD (If using GitHub)
```
If using GitHub integration:
- Every push to master auto-deploys
- Check deployment in Railway dashboard
- Rollback if needed
```

---

## 💡 PRO TIPS

1. **Use GitHub Integration**
   - Auto-deploy on push
   - Better for team workflows
   - Easy rollback

2. **Monitor Regularly**
   - Check logs daily initially
   - Set up alerts
   - Monitor performance

3. **Test Endpoints**
   ```bash
   curl https://<your-url>/health
   curl https://<your-url>/v1/auth/login
   curl https://<your-url>/v1/business
   ```

4. **Backup Database**
   - Use Railway's backup features
   - Or backup your PostgreSQL database
   - Test restore procedures

5. **Version Your Deployments**
   - Use git tags: `git tag v1.0.0`
   - Makes rollback easier
   - Track releases

---

## 🎓 LEARNING RESOURCES

- Railway Docs: https://docs.railway.app
- Docker Guide: https://docs.docker.com
- Ktor Documentation: https://ktor.io
- PostgreSQL Guide: https://www.postgresql.org/docs

---

## ⏱️ ESTIMATED TIME

- Account creation: 2 minutes
- Deployment script: 5 minutes
- Environment setup: 3 minutes
- Verification: 2 minutes
- **TOTAL: ~12 minutes**

---

## 🚀 YOU'RE READY!

Your Biashara360 backend is configured and ready to deploy.

### Next Action:
```bash
cd /home/sirpatrick/Downloads/biashara360-COMPLETE\(8\)/b360-complete
./deploy-railway.sh
```

Follow the interactive prompts and your backend will be live!

---

**Status: ✅ READY TO DEPLOY**  
**Configuration: ✅ COMPLETE**  
**Action: RUN ./deploy-railway.sh**

🎉 Good luck with your deployment!

