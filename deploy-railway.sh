#!/bin/bash

# Biashara360 Railway Deployment Script
# This script guides you through deploying to Railway

set -e

echo "╔════════════════════════════════════════════════════════════╗"
echo "║   Biashara360 Backend - Railway Deployment Script          ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Check if Railway CLI is installed
if ! command -v railway &> /dev/null; then
    echo "❌ Railway CLI not found. Installing..."
    npm install -g @railway/cli
fi

echo "✅ Railway CLI is installed"
echo ""

# Step 1: Initialize Railway
echo "Step 1: Initializing Railway project..."
echo "You will be asked to:"
echo "  - Log in with your Railway account"
echo "  - Create a new project or select existing one"
echo ""
read -p "Press Enter to continue..." -r

railway init

echo ""
echo "✅ Railway project initialized"
echo ""

# Step 2: Add PostgreSQL plugin (optional)
echo "Step 2: Add PostgreSQL Database"
echo ""
read -p "Would you like Railway to provision PostgreSQL? (y/n) " -n 1 -r
echo ""
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Please add PostgreSQL plugin through Railway dashboard:"
    echo "1. Go to your Railway project dashboard"
    echo "2. Click 'Add' or the '+' button"
    echo "3. Select 'PostgreSQL'"
    echo "4. Railway will auto-configure DATABASE_URL"
    echo ""
    read -p "Press Enter after adding PostgreSQL..." -r
fi

echo ""
echo "Step 3: Setting up environment variables"
echo ""
echo "You need to set the following variables in Railway dashboard:"
echo ""
echo "Required:"
echo "  - JWT_SECRET: Use 'openssl rand -base64 32' to generate"
echo "  - DB_USER: (usually auto-set if using Railway PostgreSQL)"
echo "  - DB_PASSWORD: (usually auto-set if using Railway PostgreSQL)"
echo ""
echo "Optional:"
echo "  - MPESA_CONSUMER_KEY"
echo "  - MPESA_CONSUMER_SECRET"
echo "  - MPESA_SHORT_CODE"
echo "  - MPESA_PASS_KEY"
echo "  - CS_MERCHANT_ID"
echo "  - CS_MERCHANT_KEY_ID"
echo "  - CS_MERCHANT_SECRET_KEY"
echo "  - KRA_ETIMS_ENV"
echo "  - META_APP_ID"
echo "  - META_APP_SECRET"
echo "  - TIKTOK_CLIENT_KEY"
echo "  - TIKTOK_CLIENT_SECRET"
echo "  - ANTHROPIC_API_KEY"
echo "  - WEBHOOK_BASE_URL"
echo ""
read -p "Press Enter after setting environment variables..." -r

echo ""
echo "Step 4: Deploying to Railway"
echo ""
echo "Adding files to git..."
git add .
git commit -m "Prepare for Railway deployment" || true

echo ""
echo "Pushing to Railway..."
railway up

echo ""
echo "✅ Deployment initiated!"
echo ""
echo "Step 5: Monitoring deployment"
echo ""
echo "View logs:"
echo "  railway logs --tail"
echo ""
echo "Get your URL:"
echo "  railway env"
echo ""
echo "Open dashboard:"
echo "  railway open"
echo ""
echo "📋 Next steps:"
echo "1. Monitor logs for any errors: railway logs --tail"
echo "2. Test your API: curl <your-railway-url>"
echo "3. Configure custom domain in Railway dashboard (if needed)"
echo "4. Set up monitoring and alerts in Railway dashboard"
echo ""
echo "🎉 Deployment complete!"

