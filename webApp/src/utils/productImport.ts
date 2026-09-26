import type { CellValue, Workbook } from 'exceljs'
import type { ProductResponse } from '../services/api'
export type ImportMode = 'products' | 'stock'
export type ImportRow = { row: number; sku: string; name: string; quantity: number; productId?: string; payload: Record<string, string | number | null>; errors: string[] }
export const headers = {
  products: ['SKU', 'Name', 'Category', 'Buying Price', 'Selling Price', 'Current Stock', 'Low Stock Threshold', 'Description', 'Barcode', 'Image URL'],
  stock: ['SKU', 'Quantity', 'Note'],
}
const normalize = (value: string) => value.toLowerCase().replace(/[\s_-]/g, '')
const text = (value: CellValue): string => {
  if (value == null) return ''
  if (typeof value === 'string' || typeof value === 'number') return String(value).trim()
  if (typeof value === 'object' && 'richText' in value) return value.richText.map(part => part.text).join('').trim()
  throw new Error('Use plain values, not formulas, dates, links or errors.')
}

export function validateWorkbook(workbook: Workbook, mode: ImportMode, existing: ProductResponse[], sheetName?: string): ImportRow[] {
  const sheet = sheetName ? workbook.getWorksheet(sheetName) : workbook.worksheets[0]
  if (!sheet) throw new Error('The workbook has no worksheet.')
  if (sheet.rowCount > 1001 || sheet.columnCount > 30) throw new Error('Use at most 1,000 rows and 30 columns per worksheet.')
  const columns = new Map<string, number>()
  sheet.getRow(1).eachCell((cell, index) => {
    const header = normalize(text(cell.value))
    if (!header) return
    if (columns.has(header)) throw new Error(`Duplicate column: ${cell.text}`)
    columns.set(header, index)
  })
  const required = mode === 'products' ? ['SKU', 'Name', 'Buying Price', 'Selling Price', 'Current Stock'] : ['SKU', 'Quantity']
  const missing = required.filter(header => !columns.has(normalize(header)))
  if (missing.length) throw new Error(`Missing columns: ${missing.join(', ')}. Download the template for this import type.`)
  const seen = new Set<string>(), barcodes = new Set<string>()
  const rows: ImportRow[] = []
  sheet.eachRow((source, index) => {
    if (index === 1 || !source.hasValues) return
    const errors: string[] = []
    const get = (header: string) => {
      const column = columns.get(normalize(header))
      if (!column) return ''
      try { return text(source.getCell(column).value) } catch (e) { errors.push(`${header}: ${(e as Error).message}`); return '' }
    }
    const values = Object.fromEntries(headers[mode].map(header => [header, get(header)]))
    if (Object.values(values).every(value => value === '') && errors.length === 0) return
    const sku = values.SKU, key = sku.toLowerCase()
    if (!/^[A-Za-z0-9_-]{1,50}$/.test(sku)) errors.push('SKU must be 1–50 letters, numbers, hyphens or underscores.')
    if (seen.has(key)) errors.push('Duplicate SKU in this worksheet.')
    seen.add(key)
    const product = existing.find(p => p.sku.toLowerCase() === key)
    const number = (header: string, max: number, integer = false, fallback?: number) => {
      const raw = values[header]
      const value = raw === '' && fallback !== undefined ? fallback : Number(raw)
      if ((raw === '' && fallback === undefined) || !/^\d+(\.\d+)?$/.test(raw || String(fallback ?? '')) || !Number.isFinite(value) || value < 0 || value > max || (integer && !Number.isInteger(value))) {
        errors.push(`${header} must be ${integer ? 'a whole number' : 'a number'} from 0 to ${max.toLocaleString()}.`)
      }
      return value
    }
    if (mode === 'stock') {
      const quantity = number('Quantity', 1000000, true)
      if (quantity === 0) errors.push('Quantity must be greater than zero.')
      if (!product) errors.push('SKU does not exist. Use New products to create it first.')
      else if (product.isActive === false) errors.push('Product is disabled. Enable it before adding stock.')
      if (product && product.currentStock + quantity > 1000000) errors.push('Resulting stock exceeds 1,000,000 units.')
      if (values.Note.length > 500) errors.push('Note must be at most 500 characters.')
      rows.push({ row: index, sku, name: product?.name || '', quantity, productId: product?.id, payload: { type: 'STOCK_IN', quantity, note: values.Note || 'Excel stock import' }, errors })
      return
    }
    if (product) errors.push('SKU already exists. Use Add stock for existing products.')
    const name = values.Name, buyingPrice = number('Buying Price', 10000000), sellingPrice = number('Selling Price', 10000000)
    const quantity = number('Current Stock', 1000000, true), lowStockThreshold = number('Low Stock Threshold', 1000, true, 5)
    if (name.length < 2 || name.length > 255) errors.push('Name must be 2–255 characters.')
    if (sellingPrice < buyingPrice) errors.push('Selling Price must be at least Buying Price.')
    const category = values.Category || 'Other'
    if (category.length > 80 || !/^[A-Za-z0-9][A-Za-z0-9 &/_-]*$/.test(category)) errors.push('Category contains unsupported characters or is longer than 80 characters.')
    if (values.Description.length > 1000) errors.push('Description must be at most 1,000 characters.')
    const barcode = values.Barcode
    if (barcode && (!/^[A-Za-z0-9-]{1,100}$/.test(barcode) || barcodes.has(barcode) || existing.some(p => p.barcode === barcode))) errors.push('Barcode is invalid or already exists.')
    if (barcode) barcodes.add(barcode)
    const imageUrl = values['Image URL']
    if (imageUrl && (imageUrl.length > 500 || !/^https?:\/\/[^\s]+$/i.test(imageUrl))) errors.push('Image URL must be an HTTP(S) URL of at most 500 characters.')
    rows.push({ row: index, sku, name, quantity, payload: { sku, name, buyingPrice, sellingPrice, currentStock: quantity, lowStockThreshold, category, description: values.Description, barcode: barcode || null, imageUrl: imageUrl || null }, errors })
  })
  if (!rows.length) throw new Error('No product rows found. Enter data below the header row.')
  return rows
}

export async function readWorkbook(file: File) {
  if (!/\.xlsx$/i.test(file.name)) throw new Error('Choose an Excel .xlsx file. Save older .xls files as .xlsx first.')
  if (file.size > 5 * 1024 * 1024) throw new Error('Choose a workbook smaller than 5 MB.')
  const { Workbook } = await import('exceljs')
  const workbook = new Workbook()
  await workbook.xlsx.load(await file.arrayBuffer())
  return workbook
}

export async function downloadTemplate(mode: ImportMode) {
  const { Workbook } = await import('exceljs')
  const workbook = new Workbook()
  const sheet = workbook.addWorksheet(mode === 'products' ? 'Products' : 'Stock')
  sheet.addRow(headers[mode])
  sheet.getRow(1).font = { bold: true }
  sheet.views = [{ state: 'frozen', ySplit: 1 }]
  sheet.columns.forEach(column => { column.width = 22 })
  sheet.getColumn(1).numFmt = '@'
  if (mode === 'products') sheet.getColumn(9).numFmt = '@'
  const bytes = await workbook.xlsx.writeBuffer()
  const url = URL.createObjectURL(new Blob([bytes], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }))
  const link = document.createElement('a')
  link.href = url; link.download = `${mode}-import-template.xlsx`; link.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
