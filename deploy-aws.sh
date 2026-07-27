#!/usr/bin/env bash

set -Eeuo pipefail

# Output colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

ECR_REPOSITORY="biashara360-api"
DEFAULT_AWS_REGION="us-east-1"
DEFAULT_GIT_BRANCH="$(git branch --show-current 2>/dev/null || echo main)"

error_handler() {
    local exit_code=$?
    local line_number=$1

    echo -e "\n${RED}❌ Deployment failed at line ${line_number}.${NC}"
    exit "$exit_code"
}

trap 'error_handler $LINENO' ERR

echo -e "${BLUE}===============================================${NC}"
echo -e "${BLUE}🚀 Biashara360 AWS Deployment Helper${NC}"
echo -e "${BLUE}===============================================${NC}"

# ---------------------------------------------------------
# Prerequisite checks
# ---------------------------------------------------------

for command_name in git docker aws; do
    if ! command -v "$command_name" >/dev/null 2>&1; then
        echo -e "${RED}❌ ${command_name} is not installed.${NC}"
        exit 1
    fi
done

if ! docker info >/dev/null 2>&1; then
    echo -e "${RED}❌ Docker is installed but is not running.${NC}"
    exit 1
fi

if [ ! -f "Dockerfile" ]; then
    echo -e "${RED}❌ No Dockerfile found in the current directory.${NC}"
    exit 1
fi

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    echo -e "${RED}❌ Current directory is not a Git repository.${NC}"
    exit 1
fi

# ---------------------------------------------------------
# Configuration
# ---------------------------------------------------------

read -rp "Enter AWS Region [${DEFAULT_AWS_REGION}]: " AWS_REGION
AWS_REGION="${AWS_REGION:-$DEFAULT_AWS_REGION}"

read -rp "Enter Git branch [${DEFAULT_GIT_BRANCH}]: " GIT_BRANCH
GIT_BRANCH="${GIT_BRANCH:-$DEFAULT_GIT_BRANCH}"

# Obtain account ID from the authenticated AWS identity
AWS_ACCOUNT_ID="$(
    aws sts get-caller-identity \
        --query Account \
        --output text
)"

if [ -z "$AWS_ACCOUNT_ID" ] || [ "$AWS_ACCOUNT_ID" = "None" ]; then
    echo -e "${RED}❌ Could not determine AWS Account ID.${NC}"
    echo "Run: aws configure"
    exit 1
fi

GIT_COMMIT="$(git rev-parse --short HEAD)"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
IMAGE_TAG="${GIT_COMMIT}-${TIMESTAMP}"

ECR_REGISTRY="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
VERSIONED_IMAGE_URI="${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}"
LATEST_IMAGE_URI="${ECR_REGISTRY}/${ECR_REPOSITORY}:latest"

echo -e "\n${BLUE}Settings Summary${NC}"
echo "AWS Region:       $AWS_REGION"
echo "AWS Account ID:   $AWS_ACCOUNT_ID"
echo "Git Branch:       $GIT_BRANCH"
echo "Git Commit:       $GIT_COMMIT"
echo "ECR Repository:   $ECR_REPOSITORY"
echo "Versioned Image:  $VERSIONED_IMAGE_URI"
echo "Latest Image:     $LATEST_IMAGE_URI"

read -rp "Commit, push, build, and publish these changes? (y/n): " CONFIRM

if [[ ! "$CONFIRM" =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}Deployment cancelled.${NC}"
    exit 0
fi

# ---------------------------------------------------------
# Step 1: Commit and push Git changes
# ---------------------------------------------------------

echo -e "\n${BLUE}[1/6] Checking Git changes...${NC}"

git checkout "$GIT_BRANCH"

if [ -n "$(git status --porcelain)" ]; then
    read -rp "Enter Git commit message: " COMMIT_MESSAGE

    if [ -z "$COMMIT_MESSAGE" ]; then
        COMMIT_MESSAGE="Deploy Biashara360 API"
    fi

    git add .
    git commit -m "$COMMIT_MESSAGE"
else
    echo -e "${YELLOW}No uncommitted Git changes found.${NC}"
fi

echo -e "${BLUE}Pushing branch to Git remote...${NC}"
git push origin "$GIT_BRANCH"

# Recalculate commit tag after committing
GIT_COMMIT="$(git rev-parse --short HEAD)"
IMAGE_TAG="${GIT_COMMIT}-${TIMESTAMP}"
VERSIONED_IMAGE_URI="${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}"

echo -e "${GREEN}✓ Git changes pushed successfully.${NC}"

# ---------------------------------------------------------
# Step 2: Create or verify ECR repository
# ---------------------------------------------------------

echo -e "\n${BLUE}[2/6] Verifying ECR repository...${NC}"

if ! aws ecr describe-repositories \
    --repository-names "$ECR_REPOSITORY" \
    --region "$AWS_REGION" \
    >/dev/null 2>&1; then

    echo "Repository does not exist. Creating it..."

    aws ecr create-repository \
        --repository-name "$ECR_REPOSITORY" \
        --image-scanning-configuration scanOnPush=true \
        --image-tag-mutability MUTABLE \
        --region "$AWS_REGION" \
        >/dev/null
fi

echo -e "${GREEN}✓ ECR repository is ready.${NC}"

# ---------------------------------------------------------
# Step 3: Authenticate with ECR
# ---------------------------------------------------------

echo -e "\n${BLUE}[3/6] Authenticating with Amazon ECR...${NC}"

aws ecr get-login-password \
    --region "$AWS_REGION" |
docker login \
    --username AWS \
    --password-stdin "$ECR_REGISTRY"

echo -e "${GREEN}✓ ECR authentication successful.${NC}"

# ---------------------------------------------------------
# Step 4: Build image
# ---------------------------------------------------------

echo -e "\n${BLUE}[4/6] Building Docker image...${NC}"

docker build \
    --pull \
    --label "org.opencontainers.image.revision=${GIT_COMMIT}" \
    --label "org.opencontainers.image.created=$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
    -t "$ECR_REPOSITORY:$IMAGE_TAG" \
    .

docker tag \
    "$ECR_REPOSITORY:$IMAGE_TAG" \
    "$VERSIONED_IMAGE_URI"

docker tag \
    "$ECR_REPOSITORY:$IMAGE_TAG" \
    "$LATEST_IMAGE_URI"

echo -e "${GREEN}✓ Docker image built successfully.${NC}"

# ---------------------------------------------------------
# Step 5: Push image
# ---------------------------------------------------------

echo -e "\n${BLUE}[5/6] Pushing versioned image...${NC}"
docker push "$VERSIONED_IMAGE_URI"

echo -e "\n${BLUE}[6/6] Pushing latest image...${NC}"
docker push "$LATEST_IMAGE_URI"

echo -e "${GREEN}✓ Images pushed successfully.${NC}"

# ---------------------------------------------------------
# Completion
# ---------------------------------------------------------

echo -e "\n${BLUE}===============================================${NC}"
echo -e "${GREEN}🎉 Deployment image published successfully${NC}"
echo -e "${BLUE}===============================================${NC}"
echo "Git commit:     $GIT_COMMIT"
echo "Versioned URI:  $VERSIONED_IMAGE_URI"
echo "Latest URI:     $LATEST_IMAGE_URI"
echo
echo "App Runner configuration:"
echo "  Image repository: $ECR_REPOSITORY"
echo "  Image tag:        latest"
echo
echo "Required environment variables:"
echo "  DATABASE_URL"
echo "  JWT_SECRET"
echo -e "${BLUE}===============================================${NC}"