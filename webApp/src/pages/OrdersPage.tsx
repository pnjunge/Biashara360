import React, { useState, useEffect } from 'react'
import { PageHeader, Card, Btn, DataTable, StatusBadge, KpiCard, Modal, Input, Select } from '../components/ui'
import { ShoppingCart, Plus, Eye } from 'lucide-react'
import { orderApi, productApi, OrderResponse, ProductResponse } from '../services/api'

const PAYMENT_METHODS = ['CASH','MPESA','CARD','COD']

const emptyOrder = { customerName:'', customerPhone:'', deliveryLocation:'', paymentMethod:'MPESA', notes:'' }

interface OrderItem { productId: string; productName: string; quantity: number; unitPrice: number }

export function OrdersPage() {
  const [orders, setOrders] = useState<OrderResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const [showNew, setShowNew] = useState(false)
  const [viewOrder, setViewOrder] = useState<OrderResponse | null>(null)
  const [form, setForm] = useState(emptyOrder)
  const [items, setItems] = useState<OrderItem[]>([{ productId:'', productName:'', quantity:1, unitPrice:0 }])
  const [products, setProducts] = useState<ProductResponse[]>([])

  const loadOrders = () => {
    setLoading(true)
    orderApi.list().then(res => {
      if (res.success && res.data) setOrders(res.data.data)
    }).finally(() => setLoading(false))
  }

  useEffect(() => { loadOrders() }, [])

  const openNew = () => {
    setForm(emptyOrder)
    setItems([{ productId:'', productName:'', quantity:1, unitPrice:0 }])
    setError('')
    setShowNew(true)
    if (products.length === 0) {
      productApi.list().then(res => { if (res.success && res.data) setProducts(res.data) })
    }
  }

  const setItemField = (idx: number, field: keyof OrderItem, value: string | number) => {
    setItems(prev => prev.map((it, i) => {
      if (i !== idx) return it
      if (field === 'productId') {
        const p = products.find(p => p.id === value)
        return { ...it, productId: value as string, productName: p?.name ?? '', unitPrice: p?.sellingPrice ?? 0 }
      }
      return { ...it, [field]: field === 'quantity' ? Number(value) : value }
    }))
  }

  const addItem = () => setItems(prev => [...prev, { productId:'', productName:'', quantity:1, unitPrice:0 }])
  const removeItem = (idx: number) => setItems(prev => prev.filter((_, i) => i !== idx))

  const handleCreateOrder = async () => {
    if (!form.customerName || !form.customerPhone) { setError('Customer name and phone are required.'); return }
    if (items.some(it => !it.productId)) { setError('Select a product for each line item.'); return }
    setSaving(true); setError('')
    try {
      const res = await orderApi.create({
        ...form,
        items: items.map(it => ({ productId: it.productId, quantity: it.quantity })),
      })
      if (res.success) { setShowNew(false); loadOrders() }
      else setError(res.message || 'Failed to create order.')
    } catch (e: any) {
      setError(e.response?.data?.message || 'Network error. Please try again.')
    } finally { setSaving(false) }
  }

  const subtotal = items.reduce((s, it) => s + it.quantity * it.unitPrice, 0)
  const f = (k: keyof typeof emptyOrder) => (v: string) => setForm(prev => ({ ...prev, [k]: v }))

  return (
    <div className="fade-in" style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      {showNew && (
        <Modal title="New Order" onClose={() => setShowNew(false)} wide
          footer={<><Btn variant="secondary" onClick={() => setShowNew(false)}>Cancel</Btn><Btn onClick={handleCreateOrder} disabled={saving}>{saving ? 'Creating...' : 'Create Order'}</Btn></>}>
          <div style={{ display:'flex', flexDirection:'column', gap:12 }}>
            {error && <p style={{ color:'var(--b360-red)', fontSize:12 }}>{error}</p>}
            <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:12 }}>
              <Input label="Customer Name *" value={form.customerName} onChange={f('customerName')} placeholder="e.g. Jane Wanjiru" />
              <Input label="Phone *" value={form.customerPhone} onChange={f('customerPhone')} placeholder="+254..." />
            </div>
            <Input label="Delivery Location" value={form.deliveryLocation} onChange={f('deliveryLocation')} placeholder="e.g. Westlands, Nairobi" />
            <Select label="Payment Method" value={form.paymentMethod} onChange={f('paymentMethod')}
              options={PAYMENT_METHODS.map(m => ({ value:m, label:m }))} />

            <div style={{ marginTop:4 }}>
              <div style={{ fontSize:12, fontWeight:600, color:'var(--b360-text-secondary)', marginBottom:8 }}>Order Items</div>
              {items.map((it, idx) => (
                <div key={idx} style={{ display:'grid', gridTemplateColumns:'1fr 80px 100px auto', gap:8, marginBottom:8, alignItems:'end' }}>
                  <div>
                    <label style={{ fontSize:12, fontWeight:500, color:'var(--b360-text-secondary)', display:'block', marginBottom:4 }}>Product</label>
                    <select value={it.productId} onChange={e => setItemField(idx, 'productId', e.target.value)}
                      style={{ width:'100%', padding:'9px 12px', border:'1px solid var(--b360-border)', borderRadius:8, fontSize:13, fontFamily:'inherit', background:'white' }}>
                      <option value="">Select product...</option>
                      {products.map(p => <option key={p.id} value={p.id}>{p.name} (KES {p.sellingPrice})</option>)}
                    </select>
                  </div>
                  <div>
                    <label style={{ fontSize:12, fontWeight:500, color:'var(--b360-text-secondary)', display:'block', marginBottom:4 }}>Qty</label>
                    <input type="number" min={1} value={it.quantity} onChange={e => setItemField(idx, 'quantity', e.target.value)}
                      style={{ width:'100%', padding:'9px 8px', border:'1px solid var(--b360-border)', borderRadius:8, fontSize:13, fontFamily:'inherit' }} />
                  </div>
                  <div style={{ fontSize:13, fontWeight:600, color:'var(--b360-green)', paddingBottom:9, paddingLeft:4 }}>
                    KES {(it.quantity * it.unitPrice).toLocaleString()}
                  </div>
                  {items.length > 1 && (
                    <button type="button" onClick={() => removeItem(idx)}
                      style={{ color:'var(--b360-red)', fontSize:18, cursor:'pointer', border:'none', background:'none', paddingBottom:4 }}>×</button>
                  )}
                </div>
              ))}
              <button type="button" onClick={addItem}
                style={{ fontSize:12, color:'var(--b360-green)', cursor:'pointer', border:'none', background:'none', fontWeight:600, padding:0 }}>+ Add Item</button>
              <div style={{ marginTop:12, textAlign:'right', fontWeight:700, fontSize:15 }}>
                Total: KES {subtotal.toLocaleString()}
              </div>
            </div>
            <Input label="Notes" value={form.notes} onChange={f('notes')} placeholder="Optional notes" />
          </div>
        </Modal>
      )}

      {viewOrder && (
        <Modal title={`Order ${viewOrder.orderNumber}`} onClose={() => setViewOrder(null)} wide
          footer={<Btn variant="secondary" onClick={() => setViewOrder(null)}>Close</Btn>}>
          <div style={{ display:'flex', flexDirection:'column', gap:12 }}>
            <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:12 }}>
              <div><span style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>Customer</span><div style={{ fontWeight:600 }}>{viewOrder.customerName}</div></div>
              <div><span style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>Phone</span><div style={{ fontWeight:600 }}>{viewOrder.customerPhone}</div></div>
              <div><span style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>Delivery</span><div style={{ fontWeight:600 }}>{viewOrder.deliveryLocation || '—'}</div></div>
              <div><span style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>Payment</span><div><StatusBadge status={viewOrder.paymentStatus} /></div></div>
              <div><span style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>Delivery Status</span><div><StatusBadge status={viewOrder.deliveryStatus} /></div></div>
              <div><span style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>Date</span><div style={{ fontWeight:600 }}>{new Date(viewOrder.createdAt).toLocaleDateString('en-KE')}</div></div>
            </div>
            <div style={{ borderTop:'1px solid var(--b360-border)', paddingTop:12 }}>
              <div style={{ fontSize:12, fontWeight:600, color:'var(--b360-text-secondary)', marginBottom:8 }}>Items</div>
              {viewOrder.items.map((it, i) => (
                <div key={i} style={{ display:'flex', justifyContent:'space-between', padding:'6px 0', borderBottom:'1px solid var(--b360-border)', fontSize:13 }}>
                  <span>{it.productName} × {it.quantity}</span>
                  <span style={{ fontWeight:600 }}>KES {it.lineTotal.toLocaleString()}</span>
                </div>
              ))}
              <div style={{ display:'flex', justifyContent:'space-between', padding:'10px 0 0', fontWeight:800, fontSize:15 }}>
                <span>Total</span><span style={{ color:'var(--b360-green)' }}>KES {viewOrder.subtotal.toLocaleString()}</span>
              </div>
            </div>
            {viewOrder.notes && <div style={{ fontSize:13, color:'var(--b360-text-secondary)' }}>Notes: {viewOrder.notes}</div>}
          </div>
        </Modal>
      )}

      <PageHeader title="Orders"
        action={<Btn icon={<Plus size={14} />} onClick={openNew}>New Order</Btn>} />

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4,1fr)', gap: 12 }}>
        <KpiCard title="Total Orders"   value={String(orders.length)}                                                      change="All time"      icon={<ShoppingCart size={18} />} color="var(--b360-blue)" />
        <KpiCard title="Delivered"      value={String(orders.filter(o => o.deliveryStatus === 'DELIVERED').length)}        change="Completed"     icon={<ShoppingCart size={18} />} color="var(--b360-green)" />
        <KpiCard title="In Progress"    value={String(orders.filter(o => o.deliveryStatus === 'PROCESSING' || o.deliveryStatus === 'SHIPPED').length)} change="Active" icon={<ShoppingCart size={18} />} color="var(--b360-amber)" />
        <KpiCard title="Pending"        value={String(orders.filter(o => o.deliveryStatus === 'PENDING').length)}          change="Awaiting action" icon={<ShoppingCart size={18} />} color="var(--b360-red)" />
      </div>

      <Card>
        {loading ? (
          <div style={{ padding: 40, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>Loading...</div>
        ) : orders.length === 0 ? (
          <div style={{ padding: 40, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>No orders yet. Click "New Order" to get started.</div>
        ) : (
          <DataTable
            headers={['Order #', 'Customer', 'Items', 'Total', 'Payment', 'Delivery', 'Date', 'Actions']}
            rows={orders.map(o => [
              <span style={{ fontFamily: 'monospace', fontWeight: 700 }}>{o.orderNumber}</span>,
              <span style={{ fontWeight: 600 }}>{o.customerName}</span>,
              o.items.length,
              <span style={{ fontWeight: 700 }}>KES {o.subtotal.toLocaleString()}</span>,
              <StatusBadge status={o.paymentStatus} />,
              <StatusBadge status={o.deliveryStatus} />,
              <span style={{ fontSize: 12, color: 'var(--b360-text-secondary)' }}>{new Date(o.createdAt).toLocaleDateString('en-KE')}</span>,
              <Btn small icon={<Eye size={12}/>} onClick={() => setViewOrder(o)}>View</Btn>,
            ])}
          />
        )}
      </Card>
    </div>
  )
}

export default OrdersPage
