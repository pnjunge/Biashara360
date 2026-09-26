# Biashara360 Web App

React 18 + TypeScript + Vite + Tailwind CSS

## Quick Start
```bash
cd webApp/
npm install
npm run dev
```
Open http://localhost:3000

## Production Build
```bash
npm run build   # outputs to dist/
```

## Environment
Edit `.env` to point at your backend:
```
VITE_API_BASE_URL=http://localhost:8080/v1
```

## Excel product and stock imports

Open **Inventory → Import Excel** and choose:

- **New products with opening stock**: creates products using SKU, Name, Buying Price,
  Selling Price and Current Stock. Optional columns are Category, Low Stock Threshold,
  Description, Barcode and Image URL. Existing SKUs are rejected.
- **Add stock to existing products**: matches SKU and adds Quantity to current stock.
  Note is optional. This never replaces the stock count or changes prices.

Download the template for the selected mode, fill it in, and upload an `.xlsx` workbook
(up to 5 MB and 1,000 rows per worksheet). Format SKU and Barcode as Text to preserve
leading zeros. Formula cells are rejected; paste their values before uploading.
For multiple worksheets, select the sheet to import. Review the preview and correct
all row errors before importing. All writes use the existing authenticated product APIs.

Rows save individually. The results identify imported, failed, unconfirmed and untouched
rows. Do not upload already imported stock rows again: doing so adds their quantity again.
If a request cannot be confirmed, the import stops; check inventory before retrying it.

## Revenue trend

The dashboard uses the report's daily revenue breakdown. For API versions that omit it,
it reads every page of paid orders and groups revenue by order date in Nairobi time.
Missing days are zero-filled; failed or incomplete requests show an error instead of
partial totals. Reporting API permissions remain unchanged.

## Tests

```bash
npm test
npm run build
```
