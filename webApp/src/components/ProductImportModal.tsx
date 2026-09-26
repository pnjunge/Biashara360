import React, { useRef, useState } from 'react'
import type { Workbook } from 'exceljs'
import { Btn, Modal } from './ui'
import { productApi, ProductResponse } from '../services/api'
import { downloadTemplate, ImportMode, ImportRow, readWorkbook, validateWorkbook } from '../utils/productImport'

export default function ProductImportModal({ onClose, onImported }: { onClose: () => void; onImported: () => void }) {
  const [mode, setMode] = useState<ImportMode>('products')
  const [rows, setRows] = useState<ImportRow[]>([])
  const [workbook, setWorkbook] = useState<Workbook | null>(null)
  const [sheet, setSheet] = useState('')
  const [products, setProducts] = useState<ProductResponse[]>([])
  const [busy, setBusy] = useState(false)
  const [started, setStarted] = useState(false)
  const [error, setError] = useState('')
  const [results, setResults] = useState<Record<number, string>>({})
  const lock = useRef(false)
  const invalid = rows.filter(row => row.errors.length).length
  const close = () => { if (!lock.current) onClose() }
  const preview = (book: Workbook, selected: string, catalog: ProductResponse[]) => {
    setRows([]); setError('')
    try { setRows(validateWorkbook(book, mode, catalog, selected)) } catch (e) { setError((e as Error).message) }
  }
  const upload = async (file?: File) => {
    if (!file || lock.current) return
    lock.current = true; setBusy(true); setError(''); setRows([]); setResults({}); setStarted(false); setWorkbook(null)
    try {
      const book = await readWorkbook(file)
      const response = await productApi.list(undefined, undefined, true)
      if (!response.success || !response.data) throw new Error(response.message || 'Could not check existing products.')
      setProducts(response.data); setWorkbook(book)
      const first = book.worksheets[0]?.name || ''
      setSheet(first); preview(book, first, response.data)
    } catch (e) { setError((e as Error).message || 'Unable to read the workbook.') }
    finally { lock.current = false; setBusy(false) }
  }
  const run = async () => {
    if (lock.current || started || !rows.length || invalid) return
    lock.current = true; setBusy(true); setStarted(true); setError('')
    try {
      for (const row of rows) {
        setResults(previous => ({ ...previous, [row.row]: 'Importing…' }))
        try {
          const response = mode === 'products' ? await productApi.create(row.payload) : await productApi.updateStock(row.productId!, row.payload)
          if (!response.success) { setResults(previous => ({ ...previous, [row.row]: `Failed: ${response.message || 'Rejected by server'}` })); continue }
          setResults(previous => ({ ...previous, [row.row]: 'Imported' }))
        } catch (e: any) {
          const status = e.response?.status
          const uncertain = !status || status >= 500
          setResults(previous => ({ ...previous, [row.row]: uncertain ? 'Unconfirmed — check inventory before trying again' : `Failed: ${e.response?.data?.message || 'Request rejected'}` }))
          if (uncertain || status === 401 || status === 403 || status === 429) {
            setError('Import stopped. Review the results and current inventory before importing the remaining rows. Unconfirmed rows may already have been saved.')
            break
          }
        }
      }
    } finally {
      lock.current = false; setBusy(false)
      onImported()
    }
  }
  const importedCount = Object.values(results).filter(value => value === 'Imported').length
  return <Modal title="Import products & inventory" wide onClose={close} footer={<>
    <Btn variant="secondary" onClick={close} disabled={busy}>{started ? 'Done' : 'Cancel'}</Btn>
    {!started && <Btn onClick={run} disabled={busy || !rows.length || invalid > 0}>Import {rows.length || ''} rows</Btn>}
  </>}>
    <div style={{ display:'flex', flexDirection:'column', gap:14 }}>
      <label>Import type<select aria-label="Import type" value={mode} disabled={busy || started} onChange={e => { setMode(e.target.value as ImportMode); setRows([]); setWorkbook(null); setError('') }} style={{ display:'block', width:'100%', padding:10, marginTop:6 }}>
        <option value="products">New products with opening stock</option><option value="stock">Add stock to existing products</option>
      </select></label>
      <p style={{ fontSize:13 }}>{mode === 'products' ? 'Creates new products. Existing SKUs are rejected; no products are overwritten.' : 'Matches existing products by SKU and adds Quantity to their current stock. Prices and product details stay unchanged.'}</p>
      <Btn variant="secondary" disabled={busy} onClick={() => downloadTemplate(mode).catch(e => setError(e.message))}>Download Excel template</Btn>
      {!started && <label>Choose Excel workbook<input aria-label="Excel workbook" type="file" accept=".xlsx" disabled={busy} onChange={e => { upload(e.target.files?.[0]); e.target.value = '' }} style={{ display:'block', marginTop:6 }} /></label>}
      <p style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>.xlsx · up to 5 MB and 1,000 rows. Keep SKU and barcode cells formatted as Text to preserve leading zeros. Use plain values, not formulas.</p>
      {workbook && workbook.worksheets.length > 1 && <label>Worksheet<select aria-label="Worksheet" value={sheet} disabled={busy || started} onChange={e => { setSheet(e.target.value); preview(workbook, e.target.value, products) }}>{workbook.worksheets.map(ws => <option key={ws.name}>{ws.name}</option>)}</select></label>}
      {error && <p role="alert" style={{ color:'var(--b360-red)' }}>{error}</p>}
      {busy && <p role="status">{started ? `Importing… ${importedCount} saved. Keep this page open.` : 'Reading workbook and checking SKUs…'}</p>}
      {rows.length > 0 && <>
        <p role="status">{started ? `${importedCount} of ${rows.length} rows imported.` : `${rows.length} rows · ${invalid} with errors. Review before importing.`}</p>
        {invalid > 0 && <p style={{ color:'var(--b360-red)' }}>Correct all errors in Excel and upload the file again. No rows have been imported.</p>}
        <div style={{ maxHeight:320, overflow:'auto' }}><table style={{ width:'100%', fontSize:12, textAlign:'left', borderCollapse:'collapse' }}>
          <thead><tr>{['Row', 'SKU', 'Product', ...(mode === 'products' ? ['Buy / Sell (KES)'] : []), mode === 'products' ? 'Opening stock' : 'Add quantity', 'Status'].map(label => <th key={label} style={{ padding:8 }}>{label}</th>)}</tr></thead>
          <tbody>{rows.map(row => <tr key={row.row}><td style={{ padding:8 }}>{row.row}</td><td>{row.sku}</td><td>{row.name}</td>{mode === 'products' && <td>{String(row.payload.buyingPrice)} / {String(row.payload.sellingPrice)}</td>}<td>{row.quantity}</td><td style={{ maxWidth:230 }}>{results[row.row] || row.errors.join(' ') || (started ? 'Not imported' : 'Ready')}</td></tr>)}</tbody>
        </table></div>
      </>}
    </div>
  </Modal>
}
