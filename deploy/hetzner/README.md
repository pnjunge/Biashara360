# Biashara360 on a Hetzner CX23

This deployment runs Caddy, the static React frontend, the Ktor API, and
PostgreSQL on one CX23. PostgreSQL is never exposed to the public network.
Caddy obtains and renews TLS certificates automatically.

## 1. Create the server

Create a CX23 in Germany or Finland with Ubuntu 24.04. Replace the placeholder
SSH key in `cloud-init.yml` first, then supply that file as cloud-init user data.
Enable Hetzner backups. Do not add ports 5432 or 8080 to the Hetzner firewall.

If using the `hcloud` CLI after authenticating it:

```bash
hcloud server create \
  --name biashara360-prod \
  --type cx23 \
  --image ubuntu-24.04 \
  --location nbg1 \
  --user-data-from-file deploy/hetzner/cloud-init.yml
```

Also create a Hetzner Cloud Firewall allowing TCP 22, 80 and 443 and UDP 443.
Restrict SSH/22 to the administrator's IP whenever possible.

## 2. Install the application

Point both DNS A records at the new server before starting Caddy:

- `app.biashara360.co.ke`
- `api.biashara360.co.ke`

Use a low DNS TTL such as 300 seconds during migration. Then connect as
`deploy`, clone the repository into `/opt/biashara360`, and configure secrets:

```bash
cd /opt/biashara360/deploy/hetzner
cp .env.example .env
chmod 600 .env
editor .env
./deploy.sh
```

Copy the existing `JWT_SECRET` and `SOCIAL_TOKEN_ENCRYPTION_KEY` exactly. A new
JWT secret logs everyone out; losing the social encryption key can make stored
merchant tokens unreadable. Copy all provider credentials from AWS Secrets
Manager without printing them into shell history or logs.

## 3. Migrate RDS with a short maintenance window

First run a rehearsal import and test login, checkout, M-Pesa callbacks, Meta
webhooks, uploads, and the detailed health endpoint. For final cutover, stop
writes to the AWS API, run one final import, and then change DNS.

`SOURCE_DATABASE_URL` must be a PostgreSQL/libpq URL, not a JDBC URL:

```bash
cd /opt/biashara360/deploy/hetzner
read -rsp 'Source database URL: ' SOURCE_DATABASE_URL
export SOURCE_DATABASE_URL
./import-rds.sh
unset SOURCE_DATABASE_URL
curl --fail https://api.biashara360.co.ke/v1/health
```

The RDS security group must temporarily allow port 5432 from the new server IP.
Remove that rule immediately after the final import.

## 4. Backups and operations

Initialize the optional off-site restic repository once, then schedule the
backup script. Local dumps alone do not protect against server loss.

```bash
docker run --rm --env-file .env restic/restic:0.18.0 init
sudo crontab -e
```

Cron entry:

```cron
17 2 * * * /opt/biashara360/deploy/hetzner/backup.sh >>/var/log/biashara360-backup.log 2>&1
```

Test a restore before cutover. Monitor disk space, container health, certificate
renewal, and backup completion. Keep AWS available as rollback for at least 48
hours after DNS cutover.

## 5. Retire AWS only after verification

After the rollback window, delete the two App Runner services and RDS instance
only after taking a final snapshot/export. Release the unattached Lightsail
static IP immediately; it is unrelated to the migration and incurs a charge.
