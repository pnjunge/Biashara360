export interface ShareableReport {
  title: string
  period: string
  businessName: string
  summary: Array<[string, string]>
  columns: string[]
  rows: Array<Array<string | number | boolean | null | undefined>>
}

const escapeHtml = (value: unknown) => String(value ?? '')
  .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  .replace(/"/g, '&quot;').replace(/'/g, '&#039;')

const csvCell = (value: unknown) => {
  const raw = String(value ?? '')
  const safe = /^[=+\-@\t\r]/.test(raw) ? `'${raw}` : raw
  return `"${safe.replace(/"/g, '""')}"`
}
const fileName = (report: ShareableReport) => `${report.title}-${report.period}`.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '')

export function downloadReportCsv(report: ShareableReport) {
  const content = [report.columns, ...report.rows].map(row => row.map(csvCell).join(',')).join('\n')
  const link = document.createElement('a')
  link.href = URL.createObjectURL(new Blob([`\uFEFF${content}`], { type: 'text/csv;charset=utf-8' }))
  link.download = `${fileName(report)}.csv`
  link.click()
  URL.revokeObjectURL(link.href)
}

export function printReport(report: ShareableReport) {
  const target = window.open('', '_blank', 'width=1000,height=750')
  if (!target) throw new Error('Allow pop-ups to print reports')
  target.document.write(`<!doctype html><html><head><meta charset="utf-8"><title>${escapeHtml(report.title)}</title><style>
    @page{size:A4 landscape;margin:12mm}body{font:12px Arial,sans-serif;color:#17211b}h1{margin:0 0 4px}.meta{color:#52625a;margin-bottom:15px}.summary{display:flex;gap:10px;flex-wrap:wrap;margin:14px 0}.metric{border:1px solid #dce6df;border-radius:7px;padding:8px 12px}.metric b{display:block;margin-top:3px}table{width:100%;border-collapse:collapse;font-size:10px}th,td{padding:7px;border:1px solid #dce6df;text-align:left}th{background:#edf7f1}.no-print{margin:15px 0}@media print{.no-print{display:none}}
  </style></head><body><h1>${escapeHtml(report.businessName)}</h1><h2>${escapeHtml(report.title)}</h2><div class="meta">Period: ${escapeHtml(report.period)} · Generated ${escapeHtml(new Date().toLocaleString('en-KE'))}</div><div class="summary">${report.summary.map(([label,value])=>`<div class="metric">${escapeHtml(label)}<b>${escapeHtml(value)}</b></div>`).join('')}</div><table><thead><tr>${report.columns.map(column=>`<th>${escapeHtml(column)}</th>`).join('')}</tr></thead><tbody>${report.rows.map(row=>`<tr>${row.map(value=>`<td>${escapeHtml(value)}</td>`).join('')}</tr>`).join('')}</tbody></table><button class="no-print" onclick="window.print()">Print report</button><script>window.addEventListener('load',()=>setTimeout(()=>window.print(),150))</script></body></html>`)
  target.document.close()
}

export function reportSummaryText(report: ShareableReport) {
  return [`${report.businessName} — ${report.title}`, `Period: ${report.period}`, '', ...report.summary.map(([label,value])=>`${label}: ${value}`), '', `Detailed rows: ${report.rows.length}`, `Generated: ${new Date().toLocaleString('en-KE')}`].join('\n')
}

export function emailReport(report: ShareableReport) {
  downloadReportCsv(report)
  window.location.href = `mailto:?subject=${encodeURIComponent(`${report.businessName} — ${report.title}`)}&body=${encodeURIComponent(`${reportSummaryText(report)}\n\nThe detailed CSV has been downloaded and can be attached to this email.`)}`
}

export function whatsappReport(report: ShareableReport) {
  downloadReportCsv(report)
  window.open(`https://wa.me/?text=${encodeURIComponent(`${reportSummaryText(report)}\n\nThe detailed CSV has been downloaded and can be attached to this WhatsApp conversation.`)}`, '_blank', 'noopener,noreferrer')
}
