#!/bin/bash
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}===============================================${NC}"
echo -e "${BLUE}🚀 Biashara360 AWS Deployment Helper${NC}"
echo -e "${BLUE}===============================================${NC}"

# Check for AWS CLI
if ! command -v aws &> /dev/null; then
    echo -e "${RED}❌ AWS CLI is not installed. Please install it first.${NC}"
    exit 1
fi

# Configuration prompts
read -p "Enter AWS Region (default: us-east-1): " AWS_REGION
AWS_REGION=${AWS_REGION:-us-east-1}

read -p "Enter AWS Account ID: " AWS_ACCOUNT_ID
if [ -z "$AWS_ACCOUNT_ID" ]; then
    echo -e "${RED}❌ AWS Account ID is required.${NC}"
    exit 1
fi

ECR_REPOSITORY="biashara360-api"
ECR_REGISTRY="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
IMAGE_URI="${ECR_REGISTRY}/${ECR_REPOSITORY}:latest"

echo -e "\n${BLUE}Settings Summary:${NC}"
echo "Region:     $AWS_REGION"
echo "Account ID: $AWS_ACCOUNT_ID"
echo "Registry:   $ECR_REGISTRY"
echo "Repository: $ECR_REPOSITORY"
echo "Target URI: $IMAGE_URI"

read -p "Confirm and proceed with deployment? (y/n): " CONFIRM
if [[ ! "$CONFIRM" =~ ^[Yy]$ ]]; then
    echo -e "${RED}Deployment cancelled.${NC}"
    exit 0
fi

# Step 1: ECR Authentication
echo -e "\n${BLUE}[1/3] Authenticating with Amazon ECR...${NC}"
aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$ECR_REGISTRY"
echo -e "${GREEN}✓ Authenticated successfully!${NC}"

# Step 2: Build Docker Image
echo -e "\n${BLUE}[2/3] Building optimized Docker image...${NC}"
docker build -t "$ECR_REPOSITORY:latest" .
docker tag "$ECR_REPOSITORY:latest" "$IMAGE_URI"
echo -e "${GREEN}✓ Image built and tagged successfully!${NC}"

# Step 3: Push to ECR
echo -e "\n${BLUE}[3/3] Pushing image to Amazon ECR...${NC}"
docker push "$IMAGE_URI"
echo -e "${GREEN}✓ Image pushed successfully to ECR!${NC}"

echo -e "\n${BLUE}===============================================${NC}"
echo -e "${GREEN}🎉 Deployment Preparation Complete!${NC}"
echo -e "${BLUE}===============================================${NC}"
echo -e "Next steps:"
echo -e "1. Go to AWS App Runner dashboard."
echo -e "2. Create or update your service to use: ${BLUE}${IMAGE_URI}${NC}"
echo -e "3. Make sure environment variables (DATABASE_URL, JWT_SECRET) are configured."
echo -e "===============================================${NC}"
