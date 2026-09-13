# Biashara360 on Oracle Cloud Always Free

This package targets one `VM.Standard.A1.Flex` ARM64 instance with **2 OCPUs,
6 GB RAM**, Ubuntu 24.04, and an 80 GB boot volume. It runs the React frontend,
Ktor API, PostgreSQL, and Caddy on the same VM. Only ports 80 and 443 are public;
PostgreSQL remains on an internal Docker network.

## 1. Create the Always Free instance

Choose the tenancy home region carefully because Always Free compute can only
be created there. Create an Ampere A1 flexible VM with:

- Shape: `VM.Standard.A1.Flex`
- OCPUs: `2`
- Memory: `6 GB`
- Image: Ubuntu 24.04 ARM64
- Boot volume: `80 GB`
- Public IPv4: enabled
- Cloud-init: contents of `cloud-init.yml` after replacing its SSH key

In the subnet security list or NSG, allow TCP 22 from the administrator's IP,
TCP 80 and 443 from anywhere, and UDP 443 from anywhere. Do not expose 5432 or
8080. The host UFW firewall applies the same restrictions.

## 2. Deploy

Point `biashara360.co.ke`, `www.biashara360.co.ke`, and `api.biashara360.co.ke` A records at the OCI
public IP. Clone the repository to `/opt/biashara360`, then:

```bash
cd /opt/biashara360/deploy/oracle
cp ../hetzner/.env.example .env
chmod 600 .env
editor .env
./deploy.sh
curl --fail https://api.biashara360.co.ke/v1/health
```

Preserve the existing AWS `JWT_SECRET` and `SOCIAL_TOKEN_ENCRYPTION_KEY`
exactly. Copy secrets without placing them in shell history or logs.

## 3. Migrate RDS

Rehearse first. During final cutover, stop writes to the AWS API, temporarily
allow the OCI public IP through the RDS security group, and run:

```bash
read -rsp 'Source database URL: ' SOURCE_DATABASE_URL
export SOURCE_DATABASE_URL
./import-rds.sh
unset SOURCE_DATABASE_URL
```

Remove the temporary RDS rule after import. Verify login, orders, payment
callbacks, Meta webhooks, uploads, and `/v1/health/detailed` before DNS cutover.

## 4. Monitor Oracle idle-reclamation risk

Oracle currently considers an A1 VM idle when CPU 95th percentile, network, and
memory utilization all remain below 20% during a seven-day period. The 6 GB VM,
768 MB initial JVM heap, and 512 MB PostgreSQL shared buffer are legitimate
application allocations intended to keep memory use representative and stable.
They are not a substitute for monitoring.

Oracle Cloud Agent should be running on the Oracle-provided Ubuntu image. In
the OCI console, confirm that Compute Instance Monitoring is enabled and create
alarms for:

- memory utilization below 25% for five days;
- CPU 95th percentile below 20% for five days;
- API health-check failure.

Schedule the local early-warning check:

```cron
13 */6 * * * /opt/biashara360/deploy/oracle/check-idle-risk.sh >>/opt/biashara360/logs/idle-risk.log 2>&1
```

Do not run artificial CPU load. If legitimate usage remains below the policy
threshold, reduce the VM memory allocation while retaining enough application
headroom, or move to paid compute.

## 5. Backups and recovery

Configure the restic variables in `.env` for storage outside this VM, initialize
the repository, and schedule:

```cron
17 2 * * * /opt/biashara360/deploy/oracle/backup.sh >>/opt/biashara360/logs/backup.log 2>&1
```

Test a restore before cutover. Keep AWS available as rollback for at least 48
hours and do not delete RDS until the final backup is independently verified.
