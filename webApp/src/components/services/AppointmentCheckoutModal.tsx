import React, { useEffect, useMemo, useState } from 'react'
import { CheckCircle2, Mail, Printer } from 'lucide-react'
import { Btn, Input, Modal, Select } from '../ui'
import { BusinessProfileResponse, OrderResponse, ProductResponse, ServiceAppointment, ServiceCatalogItem, businessApi, orderApi, paymentApi, servicesApi } from '../../services/api'
import { printOrderReceipt } from '../../utils/receipt'

type Method = 'CASH' | 'MPESA' | 'CARD'

export function AppointmentCheckoutModal({ appointment, service, products, customerEmail, onClose, onComplete }: {
  appointment: ServiceAppointment
  service: ServiceCatalogItem
  products: ProductResponse[]
  customerEmail?: string | null
  onClose: () => void
  onComplete: () => Promise<void> | void
}) {
  const [method, setMethod] = useState<Method>('CASH')
  const [phone, setPhone] = useState(appointment.customerPhone || '')
  const [email, setEmail] = useState(customerEmail || '')
  const [discount, setDiscount] = useState('0')
  const [quantities, setQuantities] = useState<Record<string, number>>({})
  const [order, setOrder] = useState<OrderResponse | null>(null)
  const [profile, setProfile] = useState<BusinessProfileResponse | null>(null)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const availableProducts = products.filter(item => item.isActive !== false && item.currentStock > 0)
  const addOnTotal = useMemo(() => availableProducts.reduce((sum, item) => sum + item.sellingPrice * (quantities[item.id] || 0), 0), [availableProducts, quantities])
  const gross = service.price + addOnTotal
  const discountAmount = Number(discount) || 0
  const total = Math.max(0, gross - discountAmount)
  const paid = order?.paymentStatus === 'PAID'

  useEffect(() => { businessApi.getProfile().then(result => { if (result.success && result.data) setProfile(result.data) }).catch(() => undefined) }, [])

  const waitForPayment = async (orderId: string) => {
    for (let attempt = 0; attempt < 30; attempt += 1) {
      await new Promise(resolve => window.setTimeout(resolve, 2000))
      const result = await orderApi.get(orderId)
      if (result.success && result.data?.paymentStatus === 'PAID') return result.data
    }
    return null
  }

  const submit = async () => {
    setError('')
    if (!Number.isFinite(discountAmount) || discountAmount < 0) return setError('Enter a valid discount amount.')
    if (discountAmount > gross) return setError('Discount cannot exceed the checkout total.')
    if (method === 'MPESA' && !phone.trim()) return setError('Enter the customer M-Pesa phone number.')
    setSaving(true)
    try {
      const result = await servicesApi.checkoutAppointment(appointment.id, {
        paymentMethod: method,
        discountAmount,
        addOns: Object.entries(quantities).filter(([, quantity]) => quantity > 0).map(([productId, quantity]) => ({ productId, quantity })),
      })
      if (!result.success || !result.data) throw new Error(result.message || 'Could not create the checkout.')
      const created = result.data
      setOrder(created)
      if (created.paymentStatus === 'PAID') { await onComplete(); return }
      if (method === 'CARD') {
        window.location.assign(`/pay/card?orderId=${encodeURIComponent(created.id)}&businessId=${encodeURIComponent(created.businessId)}`)
        return
      }
      const push = await paymentApi.initiate({ orderId: created.id, phoneNumber: phone.trim() })
      if (!push.success) throw new Error(push.message || 'Could not send the M-Pesa prompt.')
      const confirmed = await waitForPayment(created.id)
      if (!confirmed) throw new Error('M-Pesa prompt sent. Confirmation is still pending; reopen Checkout to check or retry payment.')
      setOrder(confirmed)
      await onComplete()
    } catch (e: any) { setError(e.response?.data?.message || e.message || 'Payment could not be processed.') }
    finally { setSaving(false) }
  }

  const emailReceipt = () => {
    if (!order || !email.trim()) return setError('Enter an email address for the receipt.')
    const body = [`Receipt ${order.orderNumber}`, `Customer: ${order.customerName || appointment.customerName}`, ...order.items.map(item => `${item.productName} × ${item.quantity}: KES ${item.lineTotal.toLocaleString()}`), `Total paid: KES ${order.subtotal.toLocaleString()}`, `Payment: ${order.paymentMethod}`].join('\n')
    window.location.href = `mailto:${encodeURIComponent(email.trim())}?subject=${encodeURIComponent(`Receipt ${order.orderNumber}`)}&body=${encodeURIComponent(body)}`
  }

  return <Modal wide title={`Checkout · ${appointment.customerName}`} onClose={onClose} footer={paid ? <><Btn variant="secondary" onClick={onClose}>Close</Btn><Btn variant="secondary" icon={<Mail size={14}/>} onClick={emailReceipt}>Email receipt</Btn><Btn icon={<Printer size={14}/>} onClick={() => order && printOrderReceipt(order, profile)}>Print receipt</Btn></> : <><Btn variant="secondary" onClick={onClose}>Cancel</Btn><Btn disabled={saving} onClick={submit}>{saving ? (method === 'MPESA' ? 'Waiting for confirmation…' : 'Processing…') : method === 'MPESA' ? 'Send M-Pesa prompt' : method === 'CARD' ? 'Continue to card payment' : 'Confirm cash payment'}</Btn></>}>
    <div style={{display:'grid',gap:16}}>
      {paid && <div role="status" style={{display:'flex',gap:10,alignItems:'center',padding:14,borderRadius:10,background:'var(--b360-green-bg)',color:'var(--b360-green-dark)',fontWeight:700}}><CheckCircle2 size={22}/> Payment confirmed. The appointment is complete.</div>}
      {error && <div role="alert" style={{padding:11,borderRadius:8,background:'var(--b360-red-bg)',color:'var(--b360-red)',fontSize:12}}>{error}</div>}
      <div style={{padding:14,border:'1px solid var(--b360-border)',borderRadius:10}}><div style={{display:'flex',justifyContent:'space-between'}}><strong>{service.name}</strong><strong>KES {service.price.toLocaleString()}</strong></div><small style={{color:'var(--b360-text-secondary)'}}>{service.durationMinutes} minutes</small></div>
      {!order && <>
        <div><strong style={{fontSize:13}}>Optional products / add-ons</strong>{availableProducts.length === 0 ? <p style={{fontSize:12,color:'var(--b360-text-secondary)'}}>No in-stock products are available.</p> : <div style={{display:'grid',gap:8,marginTop:8,maxHeight:190,overflow:'auto'}}>{availableProducts.map(product => <div key={product.id} style={{display:'grid',gridTemplateColumns:'1fr 90px',gap:10,alignItems:'center',padding:9,border:'1px solid var(--b360-border)',borderRadius:8}}><div><strong style={{fontSize:13}}>{product.name}</strong><div style={{fontSize:11,color:'var(--b360-text-secondary)'}}>KES {product.sellingPrice.toLocaleString()} · {product.currentStock} in stock</div></div><Input type="number" value={String(quantities[product.id] || 0)} onChange={value => setQuantities(current => ({...current,[product.id]:Math.min(product.currentStock, Math.max(0, Math.floor(Number(value) || 0)))}))}/></div>)}</div>}</div>
        <div className="responsive-grid responsive-grid-2"><Input label="Discount (KES)" type="number" value={discount} onChange={setDiscount}/><Select label="Payment method" value={method} onChange={value => setMethod(value as Method)} options={[{value:'CASH',label:'Cash'},{value:'MPESA',label:'M-Pesa'},{value:'CARD',label:'Card'}]}/></div>
        {method === 'MPESA' && <Input label="M-Pesa phone" value={phone} onChange={setPhone} placeholder="07… or 254…"/>}
      </>}
      {paid && <Input label="Receipt email" type="email" value={email} onChange={setEmail} placeholder="customer@example.com"/>}
      <div style={{padding:14,background:'var(--b360-bg)',borderRadius:10,display:'grid',gap:6}}><div style={{display:'flex',justifyContent:'space-between'}}><span>Service and add-ons</span><span>KES {(order ? order.subtotal + (order.items[0]?.discountAmount || 0) : gross).toLocaleString()}</span></div>{!order && discountAmount > 0 && <div style={{display:'flex',justifyContent:'space-between',color:'var(--b360-text-secondary)'}}><span>Discount</span><span>− KES {discountAmount.toLocaleString()}</span></div>}<div style={{display:'flex',justifyContent:'space-between',fontSize:20,fontWeight:800,borderTop:'1px solid var(--b360-border)',paddingTop:8}}><span>Total</span><span>KES {(order?.subtotal ?? total).toLocaleString()}</span></div></div>
      {!paid && method === 'CARD' && <small style={{color:'var(--b360-text-secondary)'}}>You will continue to secure hosted card payment. The appointment completes after the gateway confirms payment.</small>}
    </div>
  </Modal>
}
