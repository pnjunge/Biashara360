const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const Module = require('node:module')
const ts = require('typescript')
const ExcelJS = require('exceljs')
function load(file) {
  const filename = path.resolve(__dirname, '../src/utils', file)
  const mod = new Module(filename, module)
  mod.filename = filename; mod.paths = module.paths
  mod._compile(ts.transpileModule(fs.readFileSync(filename, 'utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 } }).outputText, filename)
  return mod.exports
}
const { validateWorkbook, headers } = load('productImport.ts')
const { dailyRevenueFromOrders, loadPaidRevenue } = load('revenueTrend.ts')
function book(mode, rows) { const wb = new ExcelJS.Workbook(); const ws = wb.addWorksheet('Import'); ws.addRow(headers[mode]); rows.forEach(row => ws.addRow(row)); return wb }
const product = ['001-SKU','Product','Other',100,150,20,0,'','001234','']
test('Excel workbook round-trip preserves opening stock, zero threshold and text identifiers', async () => {
  const original = book('products', [product]); const wb = new ExcelJS.Workbook(); await wb.xlsx.load(await original.xlsx.writeBuffer())
  const [row] = validateWorkbook(wb, 'products', [])
  assert.deepEqual(row.errors, []); assert.equal(row.payload.currentStock, 20); assert.equal(row.payload.lowStockThreshold, 0); assert.equal(row.payload.barcode, '001234')
})
test('duplicate SKUs within file and disabled existing products are rejected', () => {
  const rows = validateWorkbook(book('products', [product, product]), 'products', [{sku:'001-SKU',isActive:false}])
  assert.match(rows[0].errors.join(' '), /already exists/); assert.match(rows[1].errors.join(' '), /Duplicate SKU/)
})
test('negative, nonfinite and fractional stock are rejected', () => {
  for(const stock of [-1, 'Infinity', 1.5]) {
    const row = [...product]; row[5] = stock
    assert.match(validateWorkbook(book('products', [row]), 'products', [])[0].errors.join(' '), /Current Stock/)
  }
})
test('formulas are rejected even with cached results', () => {
  const row = [...product]; row[4] = { formula:'100+50', result:150 }
  assert.match(validateWorkbook(book('products', [row]), 'products', [])[0].errors.join(' '), /plain values/)
})
test('restock matches SKU and adds quantity without overwriting prices', () => {
  const [row] = validateWorkbook(book('stock', [['001-SKU',10,'Delivery']]), 'stock', [{id:'p1',sku:'001-SKU',name:'Product',currentStock:20,isActive:true}])
  assert.deepEqual(row.errors, []); assert.equal(row.productId, 'p1'); assert.deepEqual(row.payload, { type:'STOCK_IN', quantity:10, note:'Delivery' })
})
test('unknown SKU, zero quantity and missing columns block import', () => {
  assert.ok(validateWorkbook(book('stock', [['missing',0,'']]), 'stock', [])[0].errors.length >= 2)
  assert.throws(() => validateWorkbook(book('stock', []), 'products', []), /Missing columns/)
})
test('headers accept API field names and ignore blank rows', () => {
  const wb = book('products', [product]); wb.getWorksheet(1).getRow(1).values = headers.products.map(s => s.replaceAll(' ', ''))
  wb.getWorksheet(1).addRow(['','','','','','','','','',''])
  assert.equal(validateWorkbook(wb, 'products', []).length, 1)
})
test('daily revenue uses Nairobi dates, excludes unpaid and zero fills missing days', () => {
  const series = dailyRevenueFromOrders('2026-09-11','2026-09-12',[
    {createdAt:'2026-09-11T22:00:00Z',subtotal:500,paymentStatus:'PAID'},
    {createdAt:'2026-09-12T10:00:00Z',subtotal:200,paymentStatus:'PENDING'},
    {createdAt:'2026-09-10T10:00:00Z',subtotal:900,paymentStatus:'PAID'},
  ])
  assert.deepEqual(series,[{date:'2026-09-11',revenue:0},{date:'2026-09-12',revenue:500}])
})
test('revenue fallback reads all pages', async () => {
  const calls = []
  const series = await loadPaidRevenue('2026-09-12','2026-09-12',async page => {
    calls.push(page); return { success:true,data:{page,hasMore:page===1,data:[{createdAt:'2026-09-12T10:00:00Z',subtotal:page*100,paymentStatus:'PAID'}]} }
  })
  assert.deepEqual(calls,[1,2]); assert.equal(series[0].revenue,300)
})
test('revenue fallback refuses partial or invalid pagination', async () => {
  await assert.rejects(() => loadPaidRevenue('2026-09-12','2026-09-12', async () => ({success:false,message:'Report unavailable'})), /Report unavailable/)
  await assert.rejects(() => loadPaidRevenue('2026-09-12','2026-09-12', async () => ({success:true,data:{page:1,hasMore:true,data:[]}})), /complete revenue/)
})
