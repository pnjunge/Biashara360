#!/bin/bash

# Git Upload to GitHub - Interactive Script
# This script helps you upload your code to GitHub

set -e

echo "╔════════════════════════════════════════════════════════════╗"
echo "║   Biashara360 - Git Upload to GitHub                      ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Step 1: Check if git is initialized
echo "Step 1: Checking git repository..."
if [ ! -d ".git" ]; then
    echo "❌ Git not initialized in this directory"
    echo "Running: git init"
    git init
fi
echo "✅ Git repository found"
echo ""

# Step 2: Check git status
echo "Step 2: Checking git status..."
git status
echo ""

# Step 3: Get GitHub URL
echo "Step 3: GitHub Repository Setup"
echo ""
read -p "Enter your GitHub repository URL (e.g., https://github.com/username/b360-complete.git): " github_url

# Validate URL
if [[ ! $github_url =~ ^https://github.com/ ]]; then
    echo "❌ Invalid GitHub URL"
    exit 1
fi
echo "✅ URL received: $github_url"
echo ""

# Step 4: Add remote
echo "Step 4: Adding GitHub as remote..."
if git remote get-url origin &>/dev/null; then
    echo "Remote already exists. Removing..."
    git remote remove origin
fi
git remote add origin "$github_url"
echo "✅ Remote added successfully"
git remote -v
echo ""

# Step 5: Branch management
echo "Step 5: Preparing branch..."
current_branch=$(git rev-parse --abbrev-ref HEAD)
echo "Current branch: $current_branch"

if [ "$current_branch" != "main" ]; then
    read -p "Rename branch to 'main'? (y/n) " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        git branch -M main
        echo "✅ Branch renamed to main"
    fi
fi
echo ""

# Step 6: Push to GitHub
echo "Step 6: Pushing to GitHub..."
echo ""
echo "This will push your code to GitHub."
echo "You may be prompted for authentication."
echo ""
read -p "Ready to push? (y/n) " -n 1 -r
echo ""

if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Pushing code to GitHub..."
    git push -u origin main
    echo ""
    echo "✅ Push completed successfully!"
else
    echo "❌ Push cancelled"
    exit 1
fi

echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║   Upload Complete!                                         ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""
echo "Your code is now on GitHub!"
echo ""
echo "Next steps:"
echo "1. Visit: $github_url"
echo "2. Verify all files are there"
echo "3. (Optional) Connect to Railway for auto-deploy"
echo ""
echo "View your repository:"
echo "${github_url%.git}"

