#!/usr/bin/env bash

set -Eeuo pipefail

# =========================================================
# Output colors
# =========================================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# =========================================================
# Application configuration
# =========================================================

API_ECR_REPOSITORY="biashara360-api"
WEB_ECR_REPOSITORY="biashara360-web"

API_DOCKER_CONTEXT="."
API_DOCKERFILE="./Dockerfile"

WEB_DOCKER_CONTEXT="./webApp"
WEB_DOCKERFILE="./webApp/Dockerfile"

DEFAULT_AWS_REGION="us-east-1"

# =========================================================
# Error handling
# =========================================================

error_handler() {
    local exit_code=$?
    local line_number=$1

    echo
    echo -e "${RED}===============================================${NC}"
    echo -e "${RED}❌ Deployment failed at line ${line_number}.${NC}"
    echo -e "${RED}===============================================${NC}"

    exit "$exit_code"
}

trap 'error_handler $LINENO' ERR

# =========================================================
# Helper functions
# =========================================================

command_exists() {
    command -v "$1" >/dev/null 2>&1
}

create_ecr_repository() {
    local repository_name="$1"

    echo -e "${BLUE}Checking ECR repository: ${repository_name}${NC}"

    if aws ecr describe-repositories \
        --repository-names "$repository_name" \
        --region "$AWS_REGION" \
        >/dev/null 2>&1; then

        echo -e "${GREEN}✓ Repository already exists: ${repository_name}${NC}"
    else
        echo -e "${YELLOW}Repository not found. Creating: ${repository_name}${NC}"

        aws ecr create-repository \
            --repository-name "$repository_name" \
            --image-scanning-configuration scanOnPush=true \
            --image-tag-mutability MUTABLE \
            --region "$AWS_REGION" \
            >/dev/null

        echo -e "${GREEN}✓ Repository created: ${repository_name}${NC}"
    fi
}

build_and_push_image() {
    local application_name="$1"
    local repository_name="$2"
    local docker_context="$3"
    local dockerfile="$4"

    local local_image="${repository_name}:${IMAGE_TAG}"
    local versioned_image_uri="${ECR_REGISTRY}/${repository_name}:${IMAGE_TAG}"
    local latest_image_uri="${ECR_REGISTRY}/${repository_name}:latest"

    echo
    echo -e "${BLUE}===============================================${NC}"
    echo -e "${BLUE}Building ${application_name}${NC}"
    echo -e "${BLUE}===============================================${NC}"

    if [ ! -f "$dockerfile" ]; then
        echo -e "${RED}❌ Dockerfile not found: ${dockerfile}${NC}"
        exit 1
    fi

    if [ ! -d "$docker_context" ]; then
        echo -e "${RED}❌ Docker context directory not found: ${docker_context}${NC}"
        exit 1
    fi

    echo "Repository:     $repository_name"
    echo "Dockerfile:     $dockerfile"
    echo "Build context:  $docker_context"
    echo "Image tag:      $IMAGE_TAG"

    docker build \
        --pull \
        --file "$dockerfile" \
        --label "org.opencontainers.image.title=${application_name}" \
        --label "org.opencontainers.image.revision=${GIT_COMMIT}" \
        --label "org.opencontainers.image.created=${BUILD_DATE}" \
        --tag "$local_image" \
        "$docker_context"

    echo -e "${GREEN}✓ ${application_name} image built successfully.${NC}"

    docker tag "$local_image" "$versioned_image_uri"
    docker tag "$local_image" "$latest_image_uri"

    echo -e "${BLUE}Pushing versioned image...${NC}"
    docker push "$versioned_image_uri"

    echo -e "${BLUE}Pushing latest image...${NC}"
    docker push "$latest_image_uri"

    echo -e "${GREEN}✓ ${application_name} pushed successfully.${NC}"
    echo "Versioned image: $versioned_image_uri"
    echo "Latest image:    $latest_image_uri"
}

# =========================================================
# Header
# =========================================================

echo -e "${BLUE}===============================================${NC}"
echo -e "${BLUE}🚀 Biashara360 AWS Deployment Helper${NC}"
echo -e "${BLUE}===============================================${NC}"

# =========================================================
# Validate required tools
# =========================================================

echo
echo -e "${BLUE}Checking required tools...${NC}"

for required_command in git docker aws jq; do
    if ! command_exists "$required_command"; then
        echo -e "${RED}❌ Required command is not installed: ${required_command}${NC}"
        exit 1
    fi

    echo -e "${GREEN}✓ ${required_command} is installed.${NC}"
done

if ! docker info >/dev/null 2>&1; then
    echo -e "${RED}❌ Docker is installed but the Docker daemon is not running.${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Docker daemon is running.${NC}"

# =========================================================
# Validate Git repository
# =========================================================

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    echo -e "${RED}❌ Current directory is not a Git repository.${NC}"
    exit 1
fi

DEFAULT_GIT_BRANCH="$(git branch --show-current)"

if [ -z "$DEFAULT_GIT_BRANCH" ]; then
    DEFAULT_GIT_BRANCH="main"
fi

# =========================================================
# Read deployment configuration
# =========================================================

echo

read -rp "Enter AWS Region [${DEFAULT_AWS_REGION}]: " AWS_REGION
AWS_REGION="${AWS_REGION:-$DEFAULT_AWS_REGION}"

read -rp "Enter Git branch [${DEFAULT_GIT_BRANCH}]: " GIT_BRANCH
GIT_BRANCH="${GIT_BRANCH:-$DEFAULT_GIT_BRANCH}"

echo
echo -e "${BLUE}Validating AWS authentication...${NC}"

AWS_ACCOUNT_ID="$(
    aws sts get-caller-identity \
        --query Account \
        --output text
)"

AWS_IDENTITY_ARN="$(
    aws sts get-caller-identity \
        --query Arn \
        --output text
)"

if [ -z "$AWS_ACCOUNT_ID" ] || [ "$AWS_ACCOUNT_ID" = "None" ]; then
    echo -e "${RED}❌ Unable to determine the AWS Account ID.${NC}"
    echo "Configure AWS credentials using:"
    echo
    echo "  aws configure"
    exit 1
fi

ECR_REGISTRY="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

echo -e "${GREEN}✓ AWS authentication successful.${NC}"

# =========================================================
# Settings summary
# =========================================================

echo
echo -e "${BLUE}Deployment settings${NC}"
echo "-----------------------------------------------"
echo "AWS region:       $AWS_REGION"
echo "AWS account:      $AWS_ACCOUNT_ID"
echo "AWS identity:     $AWS_IDENTITY_ARN"
echo "Git branch:       $GIT_BRANCH"
echo "ECR registry:     $ECR_REGISTRY"
echo "API repository:   $API_ECR_REPOSITORY"
echo "Web repository:   $WEB_ECR_REPOSITORY"
echo "-----------------------------------------------"

echo
read -rp "Commit, push, build and publish both applications? (y/n): " CONFIRM

if [[ ! "$CONFIRM" =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}Deployment cancelled.${NC}"
    exit 0
fi

# =========================================================
# Step 1: Checkout the selected Git branch
# =========================================================

echo
echo -e "${BLUE}[1/7] Checking Git branch...${NC}"

CURRENT_BRANCH="$(git branch --show-current)"

if [ "$CURRENT_BRANCH" != "$GIT_BRANCH" ]; then
    if git show-ref --verify --quiet "refs/heads/${GIT_BRANCH}"; then
        git checkout "$GIT_BRANCH"
    else
        echo -e "${RED}❌ Local branch does not exist: ${GIT_BRANCH}${NC}"
        exit 1
    fi
fi

echo -e "${GREEN}✓ Current branch: ${GIT_BRANCH}${NC}"

# =========================================================
# Step 2: Commit local changes
# =========================================================

echo
echo -e "${BLUE}[2/7] Checking local Git changes...${NC}"

if [ -n "$(git status --porcelain)" ]; then
    echo
    git status --short
    echo

    read -rp "Enter Git commit message [Deploy Biashara360 changes]: " COMMIT_MESSAGE
    COMMIT_MESSAGE="${COMMIT_MESSAGE:-Deploy Biashara360 changes}"

    git add --all
    git commit -m "$COMMIT_MESSAGE"

    echo -e "${GREEN}✓ Local changes committed.${NC}"
else
    echo -e "${YELLOW}No uncommitted changes found.${NC}"
fi

# =========================================================
# Step 3: Push Git branch
# =========================================================

echo
echo -e "${BLUE}[3/7] Pushing Git branch...${NC}"

git push origin "$GIT_BRANCH"

echo -e "${GREEN}✓ Git branch pushed successfully.${NC}"

# =========================================================
# Generate image metadata after Git commit
# =========================================================

GIT_COMMIT="$(git rev-parse --short HEAD)"
TIMESTAMP="$(date -u +%Y%m%d-%H%M%S)"
BUILD_DATE="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
IMAGE_TAG="${GIT_COMMIT}-${TIMESTAMP}"

API_VERSIONED_IMAGE_URI="${ECR_REGISTRY}/${API_ECR_REPOSITORY}:${IMAGE_TAG}"
API_LATEST_IMAGE_URI="${ECR_REGISTRY}/${API_ECR_REPOSITORY}:latest"

WEB_VERSIONED_IMAGE_URI="${ECR_REGISTRY}/${WEB_ECR_REPOSITORY}:${IMAGE_TAG}"
WEB_LATEST_IMAGE_URI="${ECR_REGISTRY}/${WEB_ECR_REPOSITORY}:latest"

echo
echo -e "${BLUE}Build information${NC}"
echo "-----------------------------------------------"
echo "Git commit:       $GIT_COMMIT"
echo "Build date:       $BUILD_DATE"
echo "Image tag:        $IMAGE_TAG"
echo "-----------------------------------------------"

# =========================================================
# Step 4: Verify or create ECR repositories
# =========================================================

echo
echo -e "${BLUE}[4/7] Preparing Amazon ECR repositories...${NC}"

create_ecr_repository "$API_ECR_REPOSITORY"
create_ecr_repository "$WEB_ECR_REPOSITORY"

echo -e "${GREEN}✓ ECR repositories are ready.${NC}"

# =========================================================
# Step 5: Authenticate Docker with ECR
# =========================================================

echo
echo -e "${BLUE}[5/7] Authenticating Docker with Amazon ECR...${NC}"

aws ecr get-login-password \
    --region "$AWS_REGION" |
docker login \
    --username AWS \
    --password-stdin "$ECR_REGISTRY"

echo -e "${GREEN}✓ Docker authenticated with Amazon ECR.${NC}"

# =========================================================
# Step 6: Build and push API
# =========================================================

echo
echo -e "${BLUE}[6/7] Building and pushing Biashara360 API...${NC}"

build_and_push_image \
    "Biashara360 API" \
    "$API_ECR_REPOSITORY" \
    "$API_DOCKER_CONTEXT" \
    "$API_DOCKERFILE"

# =========================================================
# Step 7: Build and push Web
# =========================================================

echo
echo -e "${BLUE}[7/7] Building and pushing Biashara360 Web...${NC}"

build_and_push_image \
    "Biashara360 Web" \
    "$WEB_ECR_REPOSITORY" \
    "$WEB_DOCKER_CONTEXT" \
    "$WEB_DOCKERFILE"

# =========================================================
# Completion summary
# =========================================================

echo
echo -e "${BLUE}===============================================${NC}"
echo -e "${GREEN}🎉 Biashara360 deployment images published${NC}"
echo -e "${BLUE}===============================================${NC}"
echo
echo "Git branch: $GIT_BRANCH"
echo "Git commit: $GIT_COMMIT"
echo "Image tag:  $IMAGE_TAG"
echo
echo "API images:"
echo "  Versioned: $API_VERSIONED_IMAGE_URI"
echo "  Latest:    $API_LATEST_IMAGE_URI"
echo
echo "Web images:"
echo "  Versioned: $WEB_VERSIONED_IMAGE_URI"
echo "  Latest:    $WEB_LATEST_IMAGE_URI"
echo
echo "App Runner configuration:"
echo
echo "  API service image:"
echo "    $API_LATEST_IMAGE_URI"
echo
echo "  Web service image:"
echo "    $WEB_LATEST_IMAGE_URI"
echo
echo -e "${BLUE}Updating AWS App Runner image revisions...${NC}"
API_SERVICE_ARN=$(aws apprunner list-services --query "ServiceSummaryList[?ServiceName=='biashara360-api-service'].ServiceArn" --output text --region "$AWS_REGION" || true)
WEB_SERVICE_ARN=$(aws apprunner list-services --query "ServiceSummaryList[?ServiceName=='biashara360-web-service'].ServiceArn" --output text --region "$AWS_REGION" || true)

if [ -n "$API_SERVICE_ARN" ] && [ "$API_SERVICE_ARN" != "None" ]; then
    echo "Updating API service ($API_SERVICE_ARN)..."
    API_SOURCE_CONFIGURATION=$(
        aws apprunner describe-service \
            --service-arn "$API_SERVICE_ARN" \
            --region "$AWS_REGION" \
            --query 'Service.SourceConfiguration' \
            --output json |
        jq -c --arg image "$API_VERSIONED_IMAGE_URI" '.ImageRepository.ImageIdentifier = $image'
    )
    aws apprunner update-service \
        --service-arn "$API_SERVICE_ARN" \
        --source-configuration "$API_SOURCE_CONFIGURATION" \
        --region "$AWS_REGION" \
        >/dev/null
    echo -e "${GREEN}✓ API service deployment initiated.${NC}"
fi

if [ -n "$WEB_SERVICE_ARN" ] && [ "$WEB_SERVICE_ARN" != "None" ]; then
    echo "Updating Web service ($WEB_SERVICE_ARN)..."
    WEB_SOURCE_CONFIGURATION=$(
        aws apprunner describe-service \
            --service-arn "$WEB_SERVICE_ARN" \
            --region "$AWS_REGION" \
            --query 'Service.SourceConfiguration' \
            --output json |
        jq -c --arg image "$WEB_VERSIONED_IMAGE_URI" '.ImageRepository.ImageIdentifier = $image'
    )
    aws apprunner update-service \
        --service-arn "$WEB_SERVICE_ARN" \
        --source-configuration "$WEB_SOURCE_CONFIGURATION" \
        --region "$AWS_REGION" \
        >/dev/null
    echo -e "${GREEN}✓ Web service deployment initiated.${NC}"
fi

echo -e "${BLUE}===============================================${NC}"
