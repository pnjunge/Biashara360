# Biashara360 production infrastructure

This CDK stack migrates the API from App Runner to ECS Fargate while leaving the
existing App Runner service available as a rollback target during cutover.

It provisions:

- two Fargate tasks across public subnets with restricted inbound access;
- an HTTPS Application Load Balancer and DNS-validated ACM certificate;
- HTTP `/v1/health` checks, a 120-second JVM startup grace period, and rollback;
- CloudWatch logs, a dashboard, and API 5xx/payment/database alarms;
- ECS deployment-failure notifications through SNS;
- `api.biashara360.co.ke` as an ALB alias.

Before deployment, create the `biashara360/production/api` Secrets Manager
secret with the JSON keys referenced by the task definition and provide an alert
email:

```bash
npm ci
npx cdk bootstrap aws://403460914856/us-east-1
npx cdk synth --strict -c alertEmail=operations@example.com
npx cdk diff -c alertEmail=operations@example.com
npx cdk deploy --require-approval broadening -c alertEmail=operations@example.com
```

Do not delete the App Runner API until the Fargate endpoint has completed a
traffic soak and rollback has been tested.
