import React, { useEffect, useState } from 'react'
import QRCode from 'qrcode'

export type OrderingTable = { id: string; name: string; area: string }
export const orderingUrl = (slug: string, tableId?: string) =>
  `${window.location.origin}/shop/${encodeURIComponent(slug)}${tableId ? `?table=${encodeURIComponent(tableId)}` : ''}`

export default function OrderingQr({ slug, businessName, tables = [] }: {
  slug: string; businessName: string; tables?: OrderingTable[]
}) {
  const [tableId, setTableId] = useState('')
  const [png, setPng] = useState('')
  const [error, setError] = useState('')
  const [copied, setCopied] = useState(false)
  const table = tables.find(item => item.id === tableId)
  const url = orderingUrl(slug, table?.id)
  useEffect(() => {
    let active = true
    setPng(''); setError(''); setCopied(false)
    QRCode.toDataURL(url, { width: 512, margin: 4, errorCorrectionLevel: 'M' })
      .then(value => { if (active) setPng(value) })
      .catch(() => { if (active) setError('Could not generate the QR code. Please reload.') })
    return () => { active = false }
  }, [url])

  const print = () => {
    const popup = window.open('', '_blank', 'width=700,height=800')
    if (!popup) { setError('Allow popups to print the QR code.'); return }
    popup.opener = null
    popup.document.title = `${businessName} — Scan to order`
    const root = popup.document.body
    Object.assign(root.style, { textAlign: 'center', fontFamily: 'sans-serif', padding: '32px' })
    for (const text of [businessName, table ? `Order for ${table.name}` : 'Order online']) {
      const heading = popup.document.createElement('h1'); heading.textContent = text; root.appendChild(heading)
    }
    const img = popup.document.createElement('img')
    img.width = 350; img.height = 350; img.alt = 'Scan to order'; img.src = png
    img.onload = () => { popup.focus(); popup.print() }
    root.appendChild(img)
    for (const text of ['Scan with your phone camera. No account needed.', url]) {
      const paragraph = popup.document.createElement('p'); paragraph.textContent = text; root.appendChild(paragraph)
    }
  }

  return <section style={{ background: 'white', padding: 24, borderRadius: 16, border: '1px solid #dce4e2', display: 'grid', gap: 16, maxWidth: 640, margin: 'auto' }}>
    <h2 style={{ margin: 0 }}>Customer ordering QR</h2>
    <p style={{ margin: 0 }}>Customers can scan, browse products and place an order without signing in.</p>
    <label>QR destination<select value={tableId} onChange={event => setTableId(event.target.value)} style={{ display: 'block', width: '100%', padding: 12, marginTop: 6 }}>
      <option value="">General online shop</option>
      {tables.map(item => <option key={item.id} value={item.id}>{item.name} · {item.area}</option>)}
    </select></label>
    {table && <p style={{ margin: 0 }}>Orders from this code are assigned to <strong>{table.name}</strong>. Only food appears on the kitchen screen.</p>}
    {png && <img src={png} alt={`QR code for ${table?.name || businessName}`} width={256} height={256} style={{ maxWidth: '100%', height: 'auto', justifySelf: 'center' }} />}
    <a href={url} target="_blank" rel="noreferrer" style={{ overflowWrap: 'anywhere' }}>{url}</a>
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12 }}>
      <button onClick={async () => { try { await navigator.clipboard.writeText(url); setCopied(true) } catch { setError('Copy the link shown above.') } }}>{copied ? 'Copied' : 'Copy link'}</button>
      {png && <a href={png} download={`${slug}-${table?.name || 'shop'}-qr.png`}>Download QR (PNG)</a>}
      <button disabled={!png} onClick={print}>Print QR sign</button>
    </div>
    {error && <p role="alert" style={{ color: '#b91c1c' }}>{error}</p>}
  </section>
}
