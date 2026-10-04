import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { PageHeader, Card, Btn, DataTable, StatusBadge, KpiCard, Modal } from '../components/ui'
import { ShoppingCart, Eye, Printer, RefreshCw, Store, Receipt, ExternalLink } from 'lucide-react'
import { OrderActions } from '../components/OrderActions'
import { accessApi, businessApi, orderApi, paymentApi, BusinessProfileResponse, OrderResponse } from '../services/api'
import { printOrderReceipt } from '../utils/receipt'

export function OrdersPage() {
  const navigate = useNavigate()
  const [permissions,setPermissions]=useState<string[]>([])
  useEffect(()=>{accessApi.me().then(r=>setPermissions(r.data?.permissions||[])).catch(()=>setPermissions([]))},[])
  const [orders, setOrders] = useState<OrderResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [retryingOrderId, setRetryingOrderId] = useState('')
  const [receiptProfile, setReceiptProfile] = useState<BusinessProfileResponse | null>(null)
  const [viewOrder, setViewOrder] = useState<OrderResponse | null>(null)

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

    const onBranchChanged = () => loadOrders()
    window.addEventListener('branch-changed', onBranchChanged)
    return () => window.removeEventListener('branch-changed', onBranchChanged)
  }, [])

  const changed = (order?: OrderResponse) => {
    if(order) {setOrders(rows=>rows.map(row=>row.id===order.id?order:row));setViewOrder(current=>current?.id===order.id?order:current)}
    else loadOrders()
  }
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
      {viewOrder && (
        <Modal title={`Order ${viewOrder.orderNumber}`} onClose={() => setViewOrder(null)} wide
          footer={<>
            <Btn icon={<ExternalLink size={14} />} onClick={() => window.open(`/receipt/${viewOrder.id}`, '_blank')}>e-Receipt</Btn>
            <Btn variant="secondary" icon={<Printer size={14} />} onClick={() => printOrderReceipt(viewOrder, receiptProfile)}>Print Slip</Btn>
            <OrderActions order={viewOrder} permissions={permissions} onChanged={changed} />
            <Btn variant="secondary" onClick={() => setViewOrder(null)}>Close</Btn>
          </>}>
          <div style={{ display:'flex', flexDirection:'column', gap:12 }}>
            <div className="responsive-grid responsive-grid-2" style={{ gap:12 }}>
              <div><span style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>Customer</span><div style={{ fontWeight:600 }}>{viewOrder.customerName}</div></div>
              <div><span style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>Phone</span><div style={{ fontWeight:600 }}>{viewOrder.customerPhone}</div></div>
              <div><span style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>Branch</span><div style={{ fontWeight:600 }}>{viewOrder.branchName || 'Head Office'}</div></div>
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
                  disabled={['CANCELLED','REFUNDED'].includes(viewOrder.paymentStatus)||viewOrder.deliveryStatus==='CANCELLED'}
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
                  <option value="CANCELLED" disabled>CANCELLED</option>

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
        action={
          <div style={{ display: 'flex', gap: 8 }}>
            <Btn variant="secondary" icon={<RefreshCw size={13} />} onClick={loadOrders}>Refresh</Btn>
            <Btn icon={<Store size={14} />} onClick={() => navigate('/pos')}>Point of Sale (POS)</Btn>
          </div>
        }
      />

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
          <div style={{ padding: 40, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>No orders placed yet. Orders are recorded via Point of Sale (POS).</div>
        ) : (
          <DataTable
            headers={['Order #', 'Branch', 'Customer', 'Items', 'Total', 'Method / Channel', 'Payment', 'Fulfilment / Tab', 'Date', 'Actions']}
            rows={orders.map(o => [
              <span style={{ fontFamily: 'monospace', fontWeight: 700 }}>{o.orderNumber}</span>,
              <span style={{
                display: 'inline-flex',
                alignItems: 'center',
                fontSize: 11,
                fontWeight: 600,
                padding: '2px 7px',
                borderRadius: 6,
                background: 'rgba(100, 116, 139, 0.1)',
                color: '#475569'
              }}>
                {o.branchName || 'Main'}
              </span>,
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
                disabled={['CANCELLED','REFUNDED'].includes(o.paymentStatus)||o.deliveryStatus==='CANCELLED'}
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
                <option value="CANCELLED" disabled>CANCELLED</option>

              </select> : <StatusBadge status={o.tabStatus || 'OPEN'} />,
              <span style={{ fontSize: 12, color: 'var(--b360-text-secondary)' }}>{new Date(o.createdAt).toLocaleDateString('en-KE')}</span>,
              <div style={{ display:'flex', gap:6, flexWrap:'wrap' }}>
                <OrderActions order={o} permissions={permissions} onChanged={changed} />
                <Btn small icon={<Eye size={12}/>} onClick={() => setViewOrder(o)}>View</Btn>
                <Btn small variant="secondary" icon={<Receipt size={12}/>} onClick={() => window.open(`/receipt/${o.id}`, '_blank')}>e-Receipt</Btn>
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
