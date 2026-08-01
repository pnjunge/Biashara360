import { BusinessProfileResponse, OrderResponse } from '../services/api'

const escapeHtml = (value: unknown) => String(value ?? '')
  .replace(/&/g, '&amp;')
  .replace(/</g, '&lt;')
  .replace(/>/g, '&gt;')
  .replace(/"/g, '&quot;')
  .replace(/'/g, '&#039;')

const money = (value: number) => `KES ${value.toLocaleString('en-KE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`

export function printOrderReceipt(order: OrderResponse, profile: BusinessProfileResponse | null) {
  const printWindow = window.open('', '_blank', 'width=420,height=720')
  if (!printWindow) throw new Error('Allow pop-ups to print receipts')

  const baseAmount = order.baseAmount ?? order.items.reduce((sum, item) => sum + item.lineTotal, 0)
  const taxAmount = order.taxAmount ?? Math.max(0, order.subtotal - baseAmount)
  const logo = profile?.receiptLogo && (
    profile.receiptLogo.startsWith('data:image/png;base64,') ||
    profile.receiptLogo.startsWith('data:image/jpeg;base64,') ||
    profile.receiptLogo.startsWith('data:image/webp;base64,') ||
    profile.receiptLogo.startsWith('https://')
  ) ? `<img class="logo" src="${escapeHtml(profile.receiptLogo)}" alt="Business logo">` : ''
  const customer = profile?.receiptShowCustomer === false ? '' : `
    <div class="rule"></div>
    <div>CUSTOMER: ${escapeHtml(order.customerName || 'Walk-in Customer')}</div>
    ${order.customerPhone ? `<div>PHONE: ${escapeHtml(order.customerPhone)}</div>` : ''}`
  const tax = profile?.receiptShowTax === false ? '' : `
    <div class="line"><span>VAT (${Math.round((order.taxRate ?? 0) * 100)}%):</span><span>${money(taxAmount)}</span></div>`

  printWindow.document.write(`<!doctype html><html><head><meta charset="utf-8"><title>Receipt ${escapeHtml(order.orderNumber)}</title>
    <style>
      @page { size: 80mm auto; margin: 4mm; }
      * { box-sizing: border-box; } body { width: 72mm; margin: 0 auto; color: #111; font: 11px/1.35 ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; }
      .center { text-align: center; } .logo { display: block; max-width: 42mm; max-height: 20mm; object-fit: contain; margin: 0 auto 5px; }
      h1 { font-size: 16px; margin: 3px 0; } .muted { color: #444; font-size: 10px; } .rule { border-top: 1px dashed #111; margin: 7px 0; }
      .line { display: flex; justify-content: space-between; gap: 8px; } .item { margin: 5px 0; } .item-name { font-weight: 700; }
      .total { font-size: 14px; font-weight: 800; margin-top: 4px; } .message { margin-top: 8px; white-space: pre-wrap; }
      @media print { .no-print { display: none; } body { width: auto; } }
    </style></head><body>
      <div class="center">${logo}<h1>${escapeHtml(profile?.name || 'Receipt')}</h1><div>${escapeHtml(profile?.address)}</div><div>${escapeHtml(profile?.county)}${profile?.county ? ', Kenya' : ''}</div><div>${escapeHtml(profile?.phone)}</div>${profile?.kraPin ? `<div>PIN: ${escapeHtml(profile.kraPin)}</div>` : ''}</div>
      <div class="rule"></div><div class="line"><span>RECEIPT</span><strong>${escapeHtml(order.orderNumber)}</strong></div><div class="line"><span>DATE</span><span>${escapeHtml(new Date(order.createdAt).toLocaleString('en-KE'))}</span></div><div class="line"><span>PAYMENT</span><span>${escapeHtml(order.paymentMethod)} / ${escapeHtml(order.paymentStatus)}</span></div>
      ${customer}<div class="rule"></div>
      ${order.items.map(item => `<div class="item"><div class="item-name">${escapeHtml(item.productName)}</div><div class="line"><span>${item.quantity} × ${money(item.unitPrice)}</span><span>${money(item.lineTotal)}</span></div></div>`).join('')}
      <div class="rule"></div><div class="line"><span>SUBTOTAL:</span><span>${money(baseAmount)}</span></div>${tax}<div class="line total"><span>TOTAL:</span><span>${money(order.subtotal)}</span></div>
      <div class="rule"></div><div class="center message"><strong>${escapeHtml(profile?.receiptHeader)}</strong><br>${escapeHtml(profile?.receiptFooter)}</div><div class="center muted" style="margin-top:9px">Powered by Biashara360</div>
      <div class="center no-print" style="margin-top:16px"><button onclick="window.print()">Print receipt</button></div>
      <script>window.addEventListener('load', () => setTimeout(() => window.print(), 150))</script>
    </body></html>`)
  printWindow.document.close()
}
