import React, { useState, useEffect } from 'react'
import { TrendingUp, AlertTriangle, Plus, Search, Edit, Package, Users } from 'lucide-react'
import { KpiCard, StatusBadge, PageHeader, Card, Btn, DataTable, AlertBanner, Modal, Input, Select } from '../components/ui'
import { productApi, orderApi, customerApi, reportApi, ProductResponse, OrderResponse, CustomerResponse, ProfitSummaryResponse } from '../services/api'

function getCurrentMonthRange() {
  const now = new Date()
  const startDate = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`
  const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate()
  const endDate = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`
  return { startDate, endDate }
}

// ── Dashboard ─────────────────────────────────────────────────────────────────
export default function DashboardPage() {
  const [profitSummary, setProfitSummary] = useState<ProfitSummaryResponse | null>(null)
  const [recentOrders, setRecentOrders] = useState<OrderResponse[]>([])
  const [lowStockProducts, setLowStockProducts] = useState<ProductResponse[]>([])
  const [customerCount, setCustomerCount] = useState<number>(0)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const { startDate, endDate } = getCurrentMonthRange()
    Promise.all([
      reportApi.profitSummary(startDate, endDate),
      orderApi.list(undefined, undefined, 5),
      productApi.list(undefined, true),
      customerApi.list(),
    ]).then(([ps, ord, prods, custs]) => {
      if (ps.success && ps.data) setProfitSummary(ps.data)
      if (ord.success && ord.data) setRecentOrders(ord.data.data)
      if (prods.success && prods.data) setLowStockProducts(prods.data)
      if (custs.success && custs.data) setCustomerCount(custs.data.length)
    }).catch(() => {}).finally(() => setLoading(false))
  }, [])

  const fmt = (v: number) => `KES ${v.toLocaleString()}`

  return (
    <div className="fade-in" style={{ display:'flex', flexDirection:'column', gap:20 }}>
      <PageHeader title="Dashboard" />

      {loading ? (
        <div style={{ textAlign:'center', padding:40, color:'var(--b360-text-secondary)' }}>Loading...</div>
      ) : (
        <>
          <div style={{ display:'grid', gridTemplateColumns:'repeat(4, 1fr)', gap:16 }}>
            <KpiCard title="Monthly Revenue" value={profitSummary ? fmt(profitSummary.totalRevenue) : 'KES 0'} change={profitSummary ? `${(profitSummary.grossMargin * 100).toFixed(1)}% gross margin` : 'Current month'} icon={<TrendingUp size={18}/>} color="var(--b360-green)" />
            <KpiCard title="Net Profit"      value={profitSummary ? fmt(profitSummary.netProfit) : 'KES 0'} change={profitSummary ? `${(profitSummary.netMargin * 100).toFixed(1)}% net margin` : 'Current month'} icon={<TrendingUp size={18}/>} color="var(--b360-blue)" />
            <KpiCard title="Customers"       value={String(customerCount)} change="Total registered" icon={<Users size={18}/>} color="var(--b360-amber)" />
            <KpiCard title="Low Stock Items" value={String(lowStockProducts.length)} change={lowStockProducts.length > 0 ? 'Need restocking' : 'All items stocked'} icon={<AlertTriangle size={18}/>} color="var(--b360-red)" />
          </div>

          {lowStockProducts.length > 0 && (
            <div style={{ display:'flex', gap:16, flexWrap:'wrap' }}>
              <AlertBanner message={`${lowStockProducts.length} product${lowStockProducts.length > 1 ? 's are' : ' is'} running low on stock`} icon={<AlertTriangle size={15}/>} color="var(--b360-amber)" />
            </div>
          )}

          <div style={{ display:'grid', gridTemplateColumns:'1.6fr 1fr', gap:16 }}>
            <Card style={{ padding:20 }}>
              <h3 style={{ fontWeight:700, marginBottom:4 }}>Current Month Summary</h3>
              <p style={{ fontSize:12, color:'var(--b360-text-secondary)', marginBottom:16 }}>Profit & Loss overview</p>
              {profitSummary ? (
                <div style={{ display:'flex', flexDirection:'column', gap:0 }}>
                  {[
                    ['Total Revenue',   profitSummary.totalRevenue,   'var(--b360-green)'],
                    ['Cost of Goods',   profitSummary.totalCostOfGoods, 'var(--b360-red)'],
                    ['Gross Profit',    profitSummary.grossProfit,    'var(--b360-green)'],
                    ['Total Expenses',  profitSummary.totalExpenses,  'var(--b360-red)'],
                    ['Net Profit',      profitSummary.netProfit,      'var(--b360-blue)'],
                  ].map(([label, value, color], i, arr) => (
                    <div key={label as string} style={{ display:'flex', justifyContent:'space-between', alignItems:'center', padding:'10px 0', borderBottom: i < arr.length - 1 ? '1px solid var(--b360-border)' : 'none' }}>
                      <span style={{ fontSize:13, color:'var(--b360-text-secondary)' }}>{label}</span>
                      <span style={{ fontWeight: i === arr.length - 1 ? 800 : 600, fontSize: i === arr.length - 1 ? 15 : 13, color: color as string }}>
                        KES {(value as number).toLocaleString()}
                      </span>
                    </div>
                  ))}
                  <div style={{ marginTop:12, fontSize:12, color:'var(--b360-text-secondary)', textAlign:'center' }}>
                    {profitSummary.period}
                  </div>
                </div>
              ) : (
                <div style={{ padding:40, textAlign:'center', color:'var(--b360-text-secondary)', fontSize:13 }}>
                  Data will appear as orders are recorded
                </div>
              )}
            </Card>

            <Card style={{ padding:20 }}>
              <h3 style={{ fontWeight:700, marginBottom:16 }}>Recent Orders</h3>
              {recentOrders.length === 0 ? (
                <div style={{ padding:30, textAlign:'center', color:'var(--b360-text-secondary)', fontSize:13 }}>No orders yet</div>
              ) : (
                <div style={{ display:'flex', flexDirection:'column', gap:0 }}>
                  {recentOrders.slice(0, 4).map((o, i) => (
                    <div key={o.id}>
                      <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', padding:'10px 0' }}>
                        <div>
                          <div style={{ fontSize:12, fontWeight:700, color:'var(--b360-green)' }}>{o.orderNumber}</div>
                          <div style={{ fontSize:13, fontWeight:500 }}>{o.customerName}</div>
                        </div>
                        <div style={{ textAlign:'right' }}>
                          <div style={{ fontWeight:700, fontSize:13 }}>KES {o.subtotal.toLocaleString()}</div>
                          <StatusBadge status={o.paymentStatus} />
                        </div>
                      </div>
                      {i < Math.min(recentOrders.length, 4) - 1 && <div style={{ borderTop:'1px solid var(--b360-border)' }} />}
                    </div>
                  ))}
                </div>
              )}
            </Card>
          </div>
        </>
      )}
    </div>
  )
}

// ── Inventory ─────────────────────────────────────────────────────────────────
const CATEGORIES = ['Electronics','Clothing','Food & Beverage','Health & Beauty','Home & Garden','Stationery','Other']

const emptyProduct = { name:'', sku:'', category:'Other', buyingPrice:'', sellingPrice:'', currentStock:'', lowStockThreshold:'10', description:'' }

export function InventoryPage() {
  const [search, setSearch] = useState('')
  const [lowOnly, setLowOnly] = useState(false)
  const [products, setProducts] = useState<ProductResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  const [showAdd, setShowAdd] = useState(false)
  const [editProduct, setEditProduct] = useState<ProductResponse | null>(null)
  const [stockProduct, setStockProduct] = useState<ProductResponse | null>(null)
  const [form, setForm] = useState(emptyProduct)
  const [stockQty, setStockQty] = useState('')
  const [error, setError] = useState('')

  const loadProducts = () => {
    setLoading(true)
    productApi.list().then(res => {
      if (res.success && res.data) setProducts(res.data)
    }).finally(() => setLoading(false))
  }

  useEffect(() => { loadProducts() }, [])

  const stockStatus = (p: ProductResponse) => p.isOutOfStock ? 'OUT' : p.isLowStock ? 'LOW' : 'OK'
  const stockColor = (s: string) => s === 'OUT' ? 'var(--b360-red)' : s === 'LOW' ? 'var(--b360-amber)' : 'var(--b360-green)'

  const filtered = products.filter(p =>
    (p.name.toLowerCase().includes(search.toLowerCase()) || p.sku.toLowerCase().includes(search.toLowerCase()))
    && (!lowOnly || stockStatus(p) !== 'OK')
  )

  const openAdd = () => { setForm(emptyProduct); setError(''); setShowAdd(true) }
  const openEdit = (p: ProductResponse) => {
    setForm({ name:p.name, sku:p.sku, category:p.category, buyingPrice:String(p.buyingPrice),
      sellingPrice:String(p.sellingPrice), currentStock:String(p.currentStock),
      lowStockThreshold:String(p.lowStockThreshold), description:p.description })
    setError(''); setEditProduct(p)
  }
  const openStock = (p: ProductResponse) => { setStockQty(''); setError(''); setStockProduct(p) }

  const handleSaveProduct = async () => {
    if (!form.name || !form.sku || !form.buyingPrice || !form.sellingPrice || !form.currentStock) {
      setError('Please fill in all required fields.'); return
    }
    setSaving(true); setError('')
    try {
      const payload = {
        name: form.name, sku: form.sku, category: form.category,
        buyingPrice: Number(form.buyingPrice), sellingPrice: Number(form.sellingPrice),
        currentStock: Number(form.currentStock), lowStockThreshold: Number(form.lowStockThreshold) || 10,
        description: form.description,
      }
      const res = editProduct
        ? await productApi.update(editProduct.id, payload)
        : await productApi.create(payload)
      if (res.success) {
        setShowAdd(false); setEditProduct(null); loadProducts()
      } else {
        setError(res.message || 'Failed to save product.')
      }
    } catch (e: any) {
      setError(e.response?.data?.message || 'Network error. Please try again.')
    } finally { setSaving(false) }
  }

  const handleUpdateStock = async () => {
    if (!stockQty || isNaN(Number(stockQty))) { setError('Enter a valid quantity.'); return }
    if (!stockProduct) return
    setSaving(true); setError('')
    try {
      const res = await productApi.updateStock(stockProduct.id, { quantityToAdd: Number(stockQty) })
      if (res.success) { setStockProduct(null); loadProducts() }
      else setError(res.message || 'Failed to update stock.')
    } catch (e: any) {
      setError(e.response?.data?.message || 'Network error. Please try again.')
    } finally { setSaving(false) }
  }

  const f = (k: keyof typeof emptyProduct) => (v: string) => setForm(prev => ({ ...prev, [k]: v }))

  const productModal = (title: string, onClose: () => void) => (
    <Modal title={title} onClose={onClose}
      footer={<><Btn variant="secondary" onClick={onClose}>Cancel</Btn><Btn onClick={handleSaveProduct} disabled={saving}>{saving ? 'Saving...' : 'Save Product'}</Btn></>}>
      <div style={{ display:'flex', flexDirection:'column', gap:12 }}>
        {error && <p style={{ color:'var(--b360-red)', fontSize:12 }}>{error}</p>}
        <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:12 }}>
          <Input label="Product Name *" value={form.name} onChange={f('name')} placeholder="e.g. Men's Shirt" />
          <Input label="SKU *" value={form.sku} onChange={f('sku')} placeholder="e.g. SHIRT-001" />
        </div>
        <Select label="Category" value={form.category} onChange={f('category')}
          options={CATEGORIES.map(c => ({ value:c, label:c }))} />
        <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:12 }}>
          <Input label="Buying Price (KES) *" value={form.buyingPrice} onChange={f('buyingPrice')} type="number" placeholder="0" />
          <Input label="Selling Price (KES) *" value={form.sellingPrice} onChange={f('sellingPrice')} type="number" placeholder="0" />
        </div>
        <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:12 }}>
          <Input label="Current Stock *" value={form.currentStock} onChange={f('currentStock')} type="number" placeholder="0" />
          <Input label="Low Stock Threshold" value={form.lowStockThreshold} onChange={f('lowStockThreshold')} type="number" placeholder="10" />
        </div>
        <Input label="Description" value={form.description} onChange={f('description')} placeholder="Optional product description" />
      </div>
    </Modal>
  )

  return (
    <div className="fade-in" style={{ display:'flex', flexDirection:'column', gap:20 }}>
      {showAdd && productModal('Add Product', () => setShowAdd(false))}
      {editProduct && productModal('Edit Product', () => setEditProduct(null))}
      {stockProduct && (
        <Modal title={`Update Stock — ${stockProduct.name}`} onClose={() => setStockProduct(null)}
          footer={<><Btn variant="secondary" onClick={() => setStockProduct(null)}>Cancel</Btn><Btn onClick={handleUpdateStock} disabled={saving}>{saving ? 'Updating...' : 'Update Stock'}</Btn></>}>
          <div style={{ display:'flex', flexDirection:'column', gap:12 }}>
            {error && <p style={{ color:'var(--b360-red)', fontSize:12 }}>{error}</p>}
            <p style={{ fontSize:13, color:'var(--b360-text-secondary)' }}>Current stock: <strong>{stockProduct.currentStock}</strong></p>
            <Input label="Quantity to Add" value={stockQty} onChange={setStockQty} type="number" placeholder="e.g. 50" />
          </div>
        </Modal>
      )}

      <PageHeader title="Inventory"
        action={<Btn icon={<Plus size={14}/>} onClick={openAdd}>Add Product</Btn>} />

      <div style={{ display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:12 }}>
        <KpiCard title="Total Products"  value={`${products.length}`}  change="Active items"        icon={<Package size={18}/>} color="var(--b360-blue)" />
        <KpiCard title="Low Stock"       value={`${products.filter(p=>stockStatus(p)==='LOW').length}`} change="Need restocking"  icon={<AlertTriangle size={18}/>} color="var(--b360-amber)" />
        <KpiCard title="Out of Stock"    value={`${products.filter(p=>stockStatus(p)==='OUT').length}`} change="Unavailable"      icon={<AlertTriangle size={18}/>} color="var(--b360-red)" />
        <KpiCard title="Total SKUs"      value={`${products.length}`}  change="Unique products"     icon={<Package size={18}/>} color="var(--b360-green)" />
      </div>

      <Card>
        <div style={{ padding:'16px 20px', borderBottom:'1px solid var(--b360-border)', display:'flex', gap:12, alignItems:'center' }}>
          <div style={{ position:'relative', flex:1, maxWidth:300 }}>
            <Search size={14} style={{ position:'absolute', left:10, top:'50%', transform:'translateY(-50%)', color:'var(--b360-text-secondary)' }} />
            <input value={search} onChange={e=>setSearch(e.target.value)} placeholder="Search products or SKU..."
              style={{ width:'100%', padding:'8px 12px 8px 32px', border:'1px solid var(--b360-border)', borderRadius:8, fontSize:13, outline:'none', fontFamily:'inherit' }} />
          </div>
          <label style={{ display:'flex', alignItems:'center', gap:6, fontSize:13, cursor:'pointer' }}>
            <input type="checkbox" checked={lowOnly} onChange={e=>setLowOnly(e.target.checked)} />
            Low stock only
          </label>
        </div>
        {loading ? (
          <div style={{ padding:40, textAlign:'center', color:'var(--b360-text-secondary)' }}>Loading...</div>
        ) : filtered.length === 0 ? (
          <div style={{ padding:40, textAlign:'center', color:'var(--b360-text-secondary)' }}>No products yet. Click "Add Product" to get started.</div>
        ) : (
          <DataTable
            headers={['Product', 'SKU', 'Category', 'Buy Price', 'Sell Price', 'Profit', 'Stock', 'Status', 'Actions']}
            rows={filtered.map(p => {
              const st = stockStatus(p)
              return [
                <strong>{p.name}</strong>,
                <span style={{ fontFamily:'monospace', fontSize:12, color:'var(--b360-text-secondary)' }}>{p.sku}</span>,
                p.category,
                `KES ${p.buyingPrice.toLocaleString()}`,
                <span style={{ fontWeight:600, color:'var(--b360-green)' }}>KES {p.sellingPrice.toLocaleString()}</span>,
                <span style={{ color:'var(--b360-blue)', fontWeight:600 }}>KES {p.profitPerItem.toLocaleString()}</span>,
                <span style={{ fontWeight:700, color:stockColor(st) }}>{p.currentStock}</span>,
                <StatusBadge status={st} />,
                <div style={{ display:'flex', gap:6 }}>
                  <Btn variant="secondary" small icon={<Edit size={12}/>} onClick={() => openEdit(p)}>Edit</Btn>
                  <Btn variant="secondary" small icon={<Plus size={12}/>} onClick={() => openStock(p)}>Stock</Btn>
                </div>
              ]
            })}
          />
        )}
      </Card>
    </div>
  )
}


