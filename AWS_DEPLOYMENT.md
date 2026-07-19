# 🚀 Deploying Biashara360 Backend to AWS

This guide describes how to deploy the Ktor backend application to **AWS App Runner** connected to an **Amazon RDS (PostgreSQL)** database.

---

## 🏗️ Target Architecture

```
[ GitHub Repo ] ──(GitHub Actions CI/CD)──> [ Amazon ECR ]
                                                   │
                                                   ▼
[ Client Apps ] ───────────────> [ AWS App Runner Service ]
                                       │ (VPC Connector)
                                       ▼
                              [ Amazon RDS (Postgres) ]
```

---

## 📋 Prerequisites

1. An active AWS Account.
2. The AWS CLI installed and configured locally (`aws configure`).
3. Docker installed locally (if pushing images manually).

---

## 🛠️ Step 1: Create Amazon RDS PostgreSQL Database

1. Open the **Amazon RDS console** and click **Create database**.
2. **Database creation method**: Standard create.
3. **Engine options**: PostgreSQL.
4. **Templates**: Free Tier (for testing) or Production.
5. **Settings**:
   - **DB instance identifier**: `biashara360-db`
   - **Master username**: `postgres` (or your chosen username)
   - **Master password**: *Choose a secure password* (save this for `.env`)
6. **Connectivity**:
   - **Virtual private cloud (VPC)**: Choose your default or target VPC.
   - **Public access**: Select **Yes** (if you want to access it from home/local development) or **No** (if you want database traffic to remain strictly private inside the VPC).
   - **VPC security group**: Create a new security group (e.g., `biashara360-rds-sg`).
7. **Database port**: `5432`
8. **Additional configuration**:
   - **Initial database name**: `biashara360`
9. Click **Create database**. Once created, copy the **Endpoint** URL from the Connectivity & security tab.

---

## 🛠️ Step 2: Create Amazon ECR Private Repository

1. Open the **Amazon ECR console** and click **Create repository**.
2. **Visibility settings**: Private.
3. **Repository name**: `biashara360-api`
4. Click **Create repository**.
5. Copy the URI of the created repository (e.g., `123456789012.dkr.ecr.us-east-1.amazonaws.com/biashara360-api`).

---

## 🛠️ Step 3: Configure Security Groups (Allowing Connection)

If your RDS database has **Public access = No**, you must allow App Runner to connect to RDS:
1. Open the **EC2 Security Groups console**.
2. Select the RDS database security group (`biashara360-rds-sg`).
3. Add an **Inbound Rule**:
   - **Type**: PostgreSQL (port 5432).
   - **Source**: Custom (enter the ID of the security group that your App Runner VPC connector will use, or temporarily allow your VPC CIDR block, e.g., `172.31.0.0/16`).

---

## 🚀 Step 4: Create AWS App Runner Service

1. Open the **AWS App Runner console** and click **Create service**.
2. **Source**:
   - **Repository type**: Container registry.
   - **Provider**: Amazon ECR.
   - **Container image URI**: Browse or paste your ECR image URI (e.g., `.../biashara360-api:latest`).
   - **Deployment settings**: Choose **Automatic** (auto-deploys on image push) or **Manual**.
   - **App Runner service role**: Allow AWS to create or choose the default service role.
3. Click **Next**.
4. **Configure service**:
   - **Service name**: `biashara360-api-service`
   - **Virtual CPU & Memory**: 1 vCPU, 2 GB Memory (adjust based on load).
   - **Environment variables**: Add the following:
     ```env
     DATABASE_URL = jdbc:postgresql://<your-rds-endpoint>:5432/biashara360
     DB_USER      = <your-db-master-username>
     DB_PASSWORD  = <your-db-master-password>
     JWT_SECRET   = <a-secure-random-32-character-string>
     API_PORT     = 8080
     ```
5. **Configure networking**:
   - If RDS is private: Under **Networking**, choose **Custom VPC** and click **Add new VPC Connector**. Select the subnets and security group associated with your database's VPC to route App Runner egress through it.
6. **Health check**:
   - **Protocol**: TCP or HTTP.
   - If HTTP: **Path** = `/health` (Ktor exposes health status at `/health` or `/v1/health`).
7. Click **Next**, review the configuration, and click **Create & deploy**.

---

## 🔄 Step 5: Continuous Deployment with GitHub Actions

To enable automatic builds and deployments on code changes:
1. Add the following repository secrets to your GitHub repository (**Settings -> Secrets and variables -> Actions**):
   - `AWS_ACCESS_KEY_ID`: Your AWS IAM User access key.
   - `AWS_SECRET_ACCESS_KEY`: Your AWS IAM User secret key.
   - `AWS_REGION`: Your AWS region (e.g., `us-east-1`).
   - `ECR_REPOSITORY`: `biashara360-api`
   - `APP_RUNNER_SERVICE_ARN`: The ARN of your App Runner service (found in the service description).
2. The GitHub Actions workflow defined in `.github/workflows/deploy-aws.yml` will automatically build the fat jar, push it to ECR, and deploy it to App Runner on every push to the `main` or `master` branch.
