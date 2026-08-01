import React, { useState, useEffect } from 'react'
import { PageHeader, Card, Btn, DataTable, StatusBadge, KpiCard, Modal, Input, Select } from '../components/ui'
import { ShoppingCart, Plus, Eye, Printer, RefreshCw } from 'lucide-react'
import { businessApi, orderApi, productApi, customerApi, paymentApi, BusinessProfileResponse, OrderResponse, ProductResponse, CustomerResponse } from '../services/api'
import { printOrderReceipt } from '../utils/receipt'

const PAYMENT_METHODS = ['CASH','MPESA','CARD','COD']

const emptyOrder = { customerName:'', customerPhone:'', deliveryLocation:'', paymentMethod:'MPESA', notes:'' }

interface OrderItem { productId: string; productName: string; quantity: number; unitPrice: number }

export function OrdersPage() {
  const [orders, setOrders] = useState<OrderResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [retryingOrderId, setRetryingOrderId] = useState('')
  const [receiptProfile, setReceiptProfile] = useState<BusinessProfileResponse | null>(null)

  const [showNew, setShowNew] = useState(false)
  const [viewOrder, setViewOrder] = useState<OrderResponse | null>(null)
  const [form, setForm] = useState(emptyOrder)
  const [items, setItems] = useState<OrderItem[]>([{ productId:'', productName:'', quantity:1, unitPrice:0 }])
  const [products, setProducts] = useState<ProductResponse[]>([])
  const [customers, setCustomers] = useState<CustomerResponse[]>([])
  const [selectedCustomerId, setSelectedCustomerId] = useState('')

  const loadOrders = () => {
    setLoading(true)
    orderApi.list().then(res => {
      if (res.success && res.data) setOrders(res.data.data)
    }).finally(() => setLoading(false))
  }

  useEffect(() => {
    loadOrders()
    businessApi.getProfile().then(response => {
      if (response.success && response.data) setReceiptProfile(response.data)
    }).catch(() => undefined)
  }, [])

  const openNew = () => {
    setForm(emptyOrder)
    setItems([{ productId:'', productName:'', quantity:1, unitPrice:0 }])
    setSelectedCustomerId('')
    setError('')
    setShowNew(true)
    if (products.length === 0) {
      productApi.list().then(res => { if (res.success && res.data) setProducts(res.data) })
    }
    if (customers.length === 0) {
      customerApi.list().then(res => { if (res.success && res.data) setCustomers(res.data) })
    }
  }

  const onSelectCustomer = (customerId: string) => {
    setSelectedCustomerId(customerId)
    if (!customerId) return
    const selected = customers.find(c => c.id === customerId)
    if (!selected) return
    setForm(prev => ({
      ...prev,
      customerName: selected.name,
      customerPhone: selected.phone,
    }))
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
        customerId: selectedCustomerId || null,
        items: items.map(it => ({ productId: it.productId, quantity: it.quantity, unitPrice: it.unitPrice })),
      })
      if (res.success) { setShowNew(false); loadOrders() }
      else setError(res.message || 'Failed to create order.')
    } catch (e: any) {
      setError(e.response?.data?.message || 'Network error. Please try again.')
    } finally { setSaving(false) }
  }

  const subtotal = items.reduce((s, it) => s + it.quantity * it.unitPrice, 0)
  const f = (k: keyof typeof emptyOrder) => (v: string) => setForm(prev => ({ ...prev, [k]: v }))

  const retryMpesa = async (order: OrderResponse) => {
    if (!window.confirm(`Send another M-Pesa prompt to ${order.customerPhone} for ${order.orderNumber}?`)) return
    setRetryingOrderId(order.id)
    try {
      const response = await paymentApi.initiate({ orderId: order.id, phoneNumber: order.customerPhone })
      if (!response.success) throw new Error(response.message || 'M-Pesa retry failed')
      alert(response.data?.customerMessage || 'M-Pesa prompt sent. Ask the customer to enter their PIN.')
    } catch (err: any) {
      alert(err.response?.data?.message || err.message || 'M-Pesa retry failed')
    } finally {
      setRetryingOrderId('')
    }
  }

  return (
    <div className="fade-in" style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      {showNew && (
        <Modal title="New Order" onClose={() => setShowNew(false)} wide
          footer={<><Btn variant="secondary" onClick={() => setShowNew(false)}>Cancel</Btn><Btn onClick={handleCreateOrder} disabled={saving}>{saving ? 'Creating...' : 'Create Order'}</Btn></>}>
          <div style={{ display:'flex', flexDirection:'column', gap:12 }}>
            {error && <p style={{ color:'var(--b360-red)', fontSize:12 }}>{error}</p>}
            <Select
              label="Link Existing Customer (Optional)"
              value={selectedCustomerId}
              onChange={onSelectCustomer}
              options={[
                { value: '', label: 'Walk-in / New customer' },
                ...customers.map(c => ({ value: c.id, label: `${c.name} (${c.phone})` })),
              ]}
            />
            <div className="responsive-grid responsive-grid-2" style={{ gap:12 }}>
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
          footer={<><Btn icon={<Printer size={14} />} onClick={() => printOrderReceipt(viewOrder, receiptProfile)}>Print Receipt</Btn><Btn variant="secondary" onClick={() => setViewOrder(null)}>Close</Btn></>}>
          <div style={{ display:'flex', flexDirection:'column', gap:12 }}>
            <div className="responsive-grid responsive-grid-2" style={{ gap:12 }}>
              <div><span style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>Customer</span><div style={{ fontWeight:600 }}>{viewOrder.customerName}</div></div>
              <div><span style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>Phone</span><div style={{ fontWeight:600 }}>{viewOrder.customerPhone}</div></div>
              {viewOrder.serviceType === 'RETAIL' ? (
                <div><span style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>Delivery</span><div style={{ fontWeight:600 }}>{viewOrder.deliveryLocation || '—'}</div></div>
              ) : (
                <div><span style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>Hospitality service</span><div style={{ fontWeight:600 }}>{viewOrder.serviceType?.replace(/_/g, ' ')}</div></div>
              )}
              <div><span style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>Payment</span><div><StatusBadge status={viewOrder.paymentStatus} /></div></div>
              <div><span style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>Payment Method</span><div style={{ fontWeight:600 }}>{viewOrder.paymentMethod}</div></div>
              <div><span style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>Order Channel</span><div style={{ fontWeight:600 }}>{viewOrder.salesChannel}</div></div>
              {viewOrder.serviceType === 'RETAIL' ? (
                <div><span style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>Delivery Status</span><div><StatusBadge status={viewOrder.deliveryStatus} /></div></div>
              ) : (
                <div><span style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>Tab Status</span><div><StatusBadge status={viewOrder.tabStatus || 'OPEN'} /></div></div>
              )}
              <div><span style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>Date</span><div style={{ fontWeight:600 }}>{new Date(viewOrder.createdAt).toLocaleDateString('en-KE')}</div></div>
              {viewOrder.mpesaTransactionCode && (
                <div>
                  <span style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>M-Pesa Ref</span>
                  <div style={{ fontWeight:700, color:'var(--b360-green)', fontFamily:'monospace' }}>{viewOrder.mpesaTransactionCode}</div>
                </div>
              )}
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
            
            {/* Merchant Delivery Status Override */}
            {viewOrder.serviceType === 'RETAIL' && <div style={{ borderTop:'1px solid var(--b360-border)', paddingTop:12, marginTop:4 }}>
              <label style={{ fontSize:12, fontWeight:600, color:'var(--b360-text-secondary)', display:'block', marginBottom:6 }}>
                Update Delivery Status
              </label>
              <div style={{ display:'flex', gap:8, alignItems:'center' }}>
                <select
                  value={viewOrder.deliveryStatus}
                  onChange={async e => {
                    const newStatus = e.target.value
                    try {
                      await orderApi.updateDeliveryStatus(viewOrder.id, { status: newStatus })
                      setViewOrder(prev => prev ? { ...prev, deliveryStatus: newStatus } : null)
                      loadOrders()
                    } catch (err) {
                      alert('Failed to update delivery status')
                    }
                  }}
                  style={{ padding:'8px 12px', borderRadius:8, border:'1px solid var(--b360-border)', fontSize:13, fontWeight:600 }}
                >
                  <option value="PENDING">PENDING (Awaiting Dispatch)</option>
                  <option value="PROCESSING">PROCESSING (Packing)</option>
                  <option value="SHIPPED">SHIPPED (In Transit)</option>
                  <option value="DELIVERED">DELIVERED (Fulfilled)</option>
                  <option value="CANCELLED">CANCELLED</option>
                </select>
              </div>
            </div>}

            {/* Card Payment Link if pending */}
            {viewOrder.paymentMethod === 'CARD' && viewOrder.paymentStatus === 'PENDING' && (
              <div style={{ background:'#EFF6FF', border:'1px solid #BFDBFE', borderRadius:10, padding:14, marginTop:8 }}>
                <div style={{ fontWeight:700, fontSize:13, color:'#1D4ED8', marginBottom:4 }}>💳 Card Payment Link</div>
                <p style={{ fontSize:12, color:'#3B82F6', margin:0, marginBottom:8 }}>
                  Send this link to the customer to complete their card payment via CyberSource.
                </p>
                <div style={{ display:'flex', gap:8 }}>
                  <input
                    readOnly
                    value={`${window.location.origin}/pay/card?orderId=${viewOrder.id}&businessId=${viewOrder.businessId}`}
                    style={{ flex:1, padding:'6px 10px', fontSize:12, fontFamily:'monospace', borderRadius:6, border:'1px solid #93C5FD' }}
                  />
                  <Btn small onClick={() => {
                    const url = `${window.location.origin}/pay/card?orderId=${viewOrder.id}&businessId=${viewOrder.businessId}`
                    navigator.clipboard.writeText(url)
                    alert('Card payment link copied to clipboard!')
                  }}>Copy</Btn>
                  <Btn small variant="secondary" onClick={() => {
                    const url = `${window.location.origin}/pay/card?orderId=${viewOrder.id}&businessId=${viewOrder.businessId}`
                    window.open(`https://wa.me/${viewOrder.customerPhone.replace(/[^0-9]/g,'')}?text=${encodeURIComponent(`Please complete your card payment for Order ${viewOrder.orderNumber}: ${url}`)}`, '_blank')
                  }}>WhatsApp</Btn>
                </div>
              </div>
            )}
            {viewOrder.paymentMethod === 'MPESA' && viewOrder.paymentStatus === 'PENDING' && (
              <div style={{ background:'#F0FDF4', border:'1px solid #BBF7D0', borderRadius:10, padding:14, marginTop:8 }}>
                <div style={{ fontWeight:700, fontSize:13, color:'#166534', marginBottom:4 }}>M-Pesa payment pending</div>
                <p style={{ fontSize:12, color:'#15803D', margin:'0 0 8px' }}>If the customer dismissed the prompt or did not enter their PIN, send a new STK push to the same order.</p>
                <Btn small icon={<RefreshCw size={12} />} disabled={retryingOrderId === viewOrder.id} onClick={() => retryMpesa(viewOrder)}>
                  {retryingOrderId === viewOrder.id ? 'Sending…' : 'Retry M-Pesa'}
                </Btn>
              </div>
            )}
          </div>
        </Modal>
      )}

      <PageHeader title="Orders"
        action={<Btn icon={<Plus size={14} />} onClick={openNew}>New Order</Btn>} />

      <div className="responsive-grid responsive-grid-4" style={{ gap: 12 }}>
        <KpiCard title="Total Orders"   value={String(orders.length)}                                                      change="All time"      icon={<ShoppingCart size={18} />} color="var(--b360-blue)" />
        <KpiCard title="Delivered"      value={String(orders.filter(o => o.serviceType === 'RETAIL' && o.deliveryStatus === 'DELIVERED').length)}        change="Retail completed"     icon={<ShoppingCart size={18} />} color="var(--b360-green)" />
        <KpiCard title="Open Tabs"      value={String(orders.filter(o => o.serviceType !== 'RETAIL' && ['OPEN','AWAITING_PAYMENT'].includes(o.tabStatus || '')).length)} change="Hospitality" icon={<ShoppingCart size={18} />} color="var(--b360-amber)" />
        <KpiCard title="Pending"        value={String(orders.filter(o => o.serviceType === 'RETAIL' && o.deliveryStatus === 'PENDING').length)}          change="Retail awaiting action" icon={<ShoppingCart size={18} />} color="var(--b360-red)" />
      </div>

      <Card>
        {loading ? (
          <div style={{ padding: 40, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>Loading...</div>
        ) : orders.length === 0 ? (
          <div style={{ padding: 40, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>No orders yet. Click "New Order" to get started.</div>
        ) : (
          <DataTable
            headers={['Order #', 'Customer', 'Items', 'Total', 'Method / Channel', 'Payment', 'Fulfilment / Tab', 'Date', 'Actions']}
            rows={orders.map(o => [
              <span style={{ fontFamily: 'monospace', fontWeight: 700 }}>{o.orderNumber}</span>,
              <span style={{ fontWeight: 600 }}>{o.customerName}</span>,
              o.items.length,
              <span style={{ fontWeight: 700 }}>KES {o.subtotal.toLocaleString()}</span>,
              <div><strong style={{ fontSize:11 }}>{o.paymentMethod}</strong><div style={{ fontSize:10, color:'var(--b360-text-secondary)', marginTop:3 }}>{o.salesChannel}</div></div>,
              <div>
                <StatusBadge status={o.paymentStatus} />
                {o.mpesaTransactionCode && (
                  <div style={{ fontSize:10, fontFamily:'monospace', color:'var(--b360-green)', fontWeight:700, marginTop:4 }}>
                    {o.mpesaTransactionCode}
                  </div>
                )}
              </div>,
              o.serviceType === 'RETAIL' ? <select
                value={o.deliveryStatus}
                onChange={async e => {
                  const newStatus = e.target.value
                  try {
                    await orderApi.updateDeliveryStatus(o.id, { status: newStatus })
                    setOrders(prev => prev.map(item => item.id === o.id ? { ...item, deliveryStatus: newStatus } : item))
                  } catch (err) {
                    alert('Failed to update delivery status')
                  }
                }}
                style={{ padding:'4px 8px', borderRadius:6, border:'1px solid var(--b360-border)', fontSize:12, fontWeight:600, background:'white' }}
              >
                <option value="PENDING">PENDING</option>
                <option value="PROCESSING">PROCESSING</option>
                <option value="SHIPPED">SHIPPED</option>
                <option value="DELIVERED">DELIVERED</option>
                <option value="CANCELLED">CANCELLED</option>
              </select> : <StatusBadge status={o.tabStatus || 'OPEN'} />,
              <span style={{ fontSize: 12, color: 'var(--b360-text-secondary)' }}>{new Date(o.createdAt).toLocaleDateString('en-KE')}</span>,
              <div style={{ display:'flex', gap:6 }}>
                <Btn small icon={<Eye size={12}/>} onClick={() => setViewOrder(o)}>View</Btn>
                {o.paymentMethod === 'CARD' && o.paymentStatus === 'PENDING' && (
                  <Btn small variant="secondary" onClick={() => {
                    const url = `${window.location.origin}/pay/card?orderId=${o.id}&businessId=${o.businessId}`
                    navigator.clipboard.writeText(url)
                    alert(`Card payment link copied for Order ${o.orderNumber}!`)
                  }}>💳 Link</Btn>
                )}
                {o.paymentMethod === 'MPESA' && o.paymentStatus === 'PENDING' && (
                  <Btn small variant="secondary" icon={<RefreshCw size={11} />} disabled={retryingOrderId === o.id} onClick={() => retryMpesa(o)}>
                    {retryingOrderId === o.id ? 'Sending…' : 'Retry M-Pesa'}
                  </Btn>
                )}
              </div>,
            ])}
          />
        )}
      </Card>
    </div>
  )
}

export default OrdersPage
