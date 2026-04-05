# Biashara360 (B360) — Complete Multi-Platform Project

[![Deploy on Railway](https://railway.app/button.svg)](https://railway.app/new)

All-in-one SaaS platform for Kenyan traders. Inventory, sales, CRM, expenses, Mpesa + CyberSource payments.

## Quick Start

### Backend
```bash
cd backend && ./gradlew run   # http://localhost:8080
```

### Deploy Backend to Railway
```bash
cd b360-complete
./deploy-railway.sh
# OR
npm install -g @railway/cli
railway init
railway up
```

See [RAILWAY_DEPLOYMENT.md](./RAILWAY_DEPLOYMENT.md) for detailed instructions.

### Web
```bash
cd webApp && npm install && npm run dev   # http://localhost:3000
```

### Android
Open androidApp/ in Android Studio → Run.

### Desktop
```bash
./gradlew :desktopApp:run
```

### iOS
Open iosApp/iosApp.xcodeproj in Xcode → Run on simulator.

## Environment Variables (backend)
```
DATABASE_URL, DB_USER, DB_PASSWORD
JWT_SECRET
MPESA_CONSUMER_KEY, MPESA_CONSUMER_SECRET, MPESA_SHORT_CODE, MPESA_PASS_KEY, MPESA_ENV
CS_MERCHANT_ID, CS_MERCHANT_KEY_ID, CS_MERCHANT_SECRET_KEY, CS_ENVIRONMENT
```

## API: 33 endpoints total
Auth (4) · Products (6) · Orders (4) · Customers (4) · Expenses (3) · Payments/Mpesa (4) · CyberSource Card (8) · Reports (1)

## CyberSource Sandbox Test Cards
- Approve: 4242 4242 4242 4242 (Visa), 5555 5555 5555 4444 (MC)
- Decline: 4111 1111 1111 1111
- Expiry: 12/2031, CVV: 123
