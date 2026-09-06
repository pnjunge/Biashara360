import React, { FormEvent, useEffect, useMemo, useState } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import { AlertCircle, CalendarClock, CheckCircle2, Minus, Plus, Search, ShoppingBag, Trash2 } from 'lucide-react'
import { Storefront, StorefrontCheckoutResult, StorefrontProduct, storefrontApi } from '../services/api'

type CustomerStore = Storefront & { tables?: Array<{ id: string; name: string; area: string }> }

type Cart = Record<string, number>

const money = (currency: string, value: number) =>
  new Intl.NumberFormat('en-KE', { style: 'currency', currency, maximumFractionDigits: 2 }).format(value)

const transactionReference = () =>
  `store-${typeof crypto.randomUUID === 'function' ? crypto.randomUUID() : `${Date.now()}-${Math.random()}`}`

export default function StorefrontPage() {
  const { storeSlug = '' } = useParams()
  const [searchParams] = useSearchParams()
  const tableId = searchParams.get('table')
  const [guestCount, setGuestCount] = useState(1)
  const [store, setStore] = useState<CustomerStore | null>(null)
  const [cart, setCart] = useState<Cart>({})
  const [search, setSearch] = useState('')
  const [category, setCategory] = useState('ALL')
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [customerName, setCustomerName] = useState('')
  const [customerPhone, setCustomerPhone] = useState('')
  const [deliveryLocation, setDeliveryLocation] = useState('')
  const [notes, setNotes] = useState('')
  const [clientReference, setClientReference] = useState(transactionReference)
  const [order, setOrder] = useState<StorefrontCheckoutResult | null>(null)
  const [paymentStatus, setPaymentStatus] = useState('')
  const [appointmentServiceId, setAppointmentServiceId] = useState('')
  const [appointmentStartsAt, setAppointmentStartsAt] = useState('')
  const [appointmentMessage, setAppointmentMessage] = useState('')
  const table = store?.tables?.find(item => item.id === tableId)
  const [paymentMethod, setPaymentMethod] = useState<'MPESA' | 'COD' | 'CARD'>('MPESA')

  useEffect(() => {
    let active = true
    setLoading(true)
    storefrontApi.get(storeSlug)
      .then(response => {
        if (!active) return
        if (response.success && response.data) setStore(response.data)
        else setError(response.message || 'This store is unavailable.')
      })
      .catch((err: any) => active && setError(err.response?.data?.message || 'This store is unavailable.'))
      .finally(() => active && setLoading(false))
    return () => { active = false }
  }, [storeSlug])

  useEffect(() => {
    if (!order || order.paymentMethod !== 'MPESA' || paymentStatus === 'PAID') return
    const poll = async () => {
      try {
        const response = await storefrontApi.orderStatus(storeSlug, order.orderId, order.clientReference)
        if (response.success && response.data) setPaymentStatus(response.data.paymentStatus)
      } catch { /* polling is best effort */ }
    }
    poll()
    const timer = window.setInterval(poll, 5_000)
    return () => window.clearInterval(timer)
  }, [storeSlug, order, paymentStatus])

  useEffect(() => {
    if (paymentStatus !== 'PAID') return
    const timer = window.setTimeout(() => {
      setCart({})
      setOrder(null)
      setPaymentStatus('')
      setError('')
      setCustomerName('')
      setCustomerPhone('')
      setDeliveryLocation('')
      setNotes('')
      setClientReference(transactionReference())
      setSearch('')
      setCategory('ALL')
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }, 2_500)
    return () => window.clearTimeout(timer)
  }, [paymentStatus])

  const categories = useMemo(() => ['ALL', ...new Set(store?.products.map(product => product.category).filter(Boolean) || [])], [store])
  const visibleProducts = useMemo(() => {
    const term = search.trim().toLowerCase()
    return (store?.products || []).filter(product =>
      (category === 'ALL' || product.category === category) &&
      (!term || product.name.toLowerCase().includes(term) || product.description.toLowerCase().includes(term) || product.sku.toLowerCase().includes(term))
    )
  }, [store, search, category])
  const cartProducts = useMemo(() => (store?.products || []).filter(product => cart[product.id]), [store, cart])
  const itemCount = Object.values(cart).reduce((sum, quantity) => sum + quantity, 0)
  const total = cartProducts.reduce((sum, product) => sum + product.sellingPrice * cart[product.id], 0)

  const changeQuantity = (product: StorefrontProduct, delta: number) => {
    setCart(current => {
      const next = Math.min(product.availableQuantity, Math.max(0, (current[product.id] || 0) + delta))
      if (next === 0) {
        const copy = { ...current }
        delete copy[product.id]
        return copy
      }
      return { ...current, [product.id]: next }
    })
  }

  const checkout = async (event: FormEvent) => {
    event.preventDefault()
    if (!store) return
    const storeBusinessId = store.businessId
    if (!cartProducts.length) return setError('Add at least one product to your cart.')
    setSubmitting(true)
    setError('')
    try {
      if (tableId !== null && !table) throw new Error('This table is unavailable. Ask your server for the correct QR code.')
      const request = {
        clientReference,
        customerName,
        customerPhone,
        deliveryLocation: table ? table.name : deliveryLocation,
        tableId: table?.id,
        guestCount,
        paymentMethod,
        notes,
        items: cartProducts.map(product => ({ productId: product.id, quantity: cart[product.id] }))
      }
      const response = await storefrontApi.checkout(storeSlug, request)
      if (response.success && response.data) {
        if (response.data.paymentMethod === 'CARD') {
          window.location.assign(`/pay/card?orderId=${encodeURIComponent(response.data.orderId)}&businessId=${encodeURIComponent(storeBusinessId)}&storeSlug=${encodeURIComponent(storeSlug)}`)
          return
        }
        setOrder(response.data)
        setPaymentStatus(response.data.paymentStatus)
      } else setError(response.message || 'Checkout could not be completed.')
    } catch (err: any) {
      const response = err.response?.data
      if (response?.data?.orderId) {
        setOrder(response.data)
        setPaymentStatus(response.data.paymentStatus)
      }
      setError(response?.message || err.message || 'Checkout could not be completed.')
    } finally {
      setSubmitting(false)
    }
  }

  const bookAppointment = async (event: FormEvent) => {
    event.preventDefault()
    if (!store || !appointmentServiceId || !appointmentStartsAt || !customerName.trim() || !customerPhone.trim()) {
      setAppointmentMessage('Choose a service, date and time, name, and phone number.')
      return
    }
    const startsAt = new Date(appointmentStartsAt)
    if (Number.isNaN(startsAt.getTime())) { setAppointmentMessage('Choose a valid date and time.'); return }
    setSubmitting(true); setAppointmentMessage('')
    try {
      const response = await storefrontApi.bookAppointment(storeSlug, { serviceId: appointmentServiceId, customerName, customerPhone, startsAt: startsAt.toISOString(), notes })
      if (response.success) {
        setAppointmentMessage('Appointment requested. The business will confirm it shortly.')
        setAppointmentStartsAt('')
      } else setAppointmentMessage(response.message || 'Could not book the appointment.')
    } catch (err: any) { setAppointmentMessage(err.response?.data?.message || 'Could not book the appointment.') }
    finally { setSubmitting(false) }
  }

  const retryPayment = async () => {
    if (!order) return
    setSubmitting(true)
    setError('')
    try {
      const response = await storefrontApi.retryMpesa(storeSlug, order.orderId, {
        clientReference: order.clientReference,
        customerPhone
      })
      if (response.success && response.data) setOrder(response.data)
      else setError(response.message || 'Unable to retry M-Pesa payment.')
    } catch (err: any) {
      setError(err.response?.data?.message || 'Unable to retry M-Pesa payment.')
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) return <div className="store-state">Loading store…</div>
  if (!store) return <div className="store-state"><AlertCircle size={30} /><h2>Store unavailable</h2><p>{error}</p></div>

  if (order) return (
    <main className="storefront-shell" style={{ '--store-primary': store.themeColor || '#0F766E' } as React.CSSProperties}>
      <section className="store-result">
        {paymentStatus === 'PAID' ? <CheckCircle2 size={58} color="#16a34a" /> : <ShoppingBag size={58} color="#0f766e" />}
        <p className="store-eyebrow">{store.businessName}</p>
        <h1>{paymentStatus === 'PAID' ? 'Payment received' : order.paymentMethod === 'COD' ? 'Order placed' : 'Order created'}</h1>
        <p>Order <strong>{order.orderNumber}</strong> · {money(store.currency, order.amount)}</p>
        <div className={`store-payment-status ${paymentStatus === 'PAID' ? 'paid' : ''}`}>{paymentStatus || 'PENDING'}</div>
        {paymentStatus === 'PAID' && <p>Returning to the store…</p>}
        {paymentStatus !== 'PAID' && <p>{order.customerMessage || (order.paymentMethod === 'COD' ? 'Pay when your order is delivered.' : 'Complete the M-Pesa prompt on your phone. This page checks payment automatically.')}</p>}
        {error && <div className="store-error"><AlertCircle size={16} />{error}</div>}
        {paymentStatus !== 'PAID' && order.paymentMethod === 'MPESA' && <button className="store-primary" disabled={submitting} onClick={retryPayment}>{submitting ? 'Requesting…' : 'Retry M-Pesa push'}</button>}
      </section>
    </main>
  )

  return (
    <main className="storefront-shell" style={{ '--store-primary': store.themeColor || '#0F766E' } as React.CSSProperties}>
      <header className={`store-hero ${store.bannerUrl ? 'has-banner' : ''}`} style={store.bannerUrl ? { backgroundImage:`linear-gradient(120deg, #0009, #0005), url(${store.bannerUrl})` } : undefined}>
        <div><p className="store-eyebrow">{store.businessName}</p><h1>{store.headline || store.businessName}</h1><p>{store.description || store.welcomeMessage}</p></div>
        <div className="store-cart-count"><ShoppingBag size={20} />{itemCount} item{itemCount === 1 ? '' : 's'}</div>
      </header>
      <div className="store-layout">
        <section>
          <div className="store-tools">
            <label className="store-search"><Search size={18} /><input value={search} onChange={event => setSearch(event.target.value)} placeholder="Search products" /></label>
            <div className="store-categories">{categories.map(value => <button key={value} className={category === value ? 'active' : ''} onClick={() => setCategory(value)}>{value === 'ALL' ? 'All' : value}</button>)}</div>
          </div>
          <div className={`store-products ${store.layout === 'LIST' ? 'list' : ''}`}>
            {!visibleProducts.length && <div className="store-empty">No products match your search.</div>}
            {visibleProducts.map(product => (
              <article className="store-product" key={product.id}>
                <div className="store-product-image">{product.imageUrl ? <img src={product.imageUrl} alt="" onError={event => { event.currentTarget.style.display = 'none' }} /> : <ShoppingBag size={34} />}</div>
                <div className="store-product-body"><span>{product.category || 'Product'}</span><h2>{product.name}</h2><p>{product.description || `SKU ${product.sku}`}</p><strong>{money(store.currency, product.sellingPrice)}</strong></div>
                <div className="store-quantity">
                  {cart[product.id] ? <><button aria-label={`Remove one ${product.name}`} onClick={() => changeQuantity(product, -1)}><Minus size={15} /></button><b>{cart[product.id]}</b><button aria-label={`Add one ${product.name}`} disabled={cart[product.id] >= product.availableQuantity} onClick={() => changeQuantity(product, 1)}><Plus size={15} /></button></> : <button className="store-add" onClick={() => changeQuantity(product, 1)}>Add to cart</button>}
                </div>
              </article>
            ))}
          </div>
        </section>
        <aside className="store-checkout">
          <h2>{table ? `Order for ${table.name}` : 'Your cart'}</h2>
          {tableId !== null && !table && <p className="store-error">This table is unavailable. Please ask your server for a new QR code.</p>}
          {table && <p className="store-muted">{table.area} · Food goes to the kitchen; drinks stay on your bill.</p>}
          {!cartProducts.length ? <p className="store-muted">Your cart is empty.</p> : cartProducts.map(product => <div className="store-cart-line" key={product.id}><div><strong>{product.name}</strong><span>{cart[product.id]} × {money(store.currency, product.sellingPrice)}</span></div><button aria-label={`Remove ${product.name}`} onClick={() => setCart(current => { const copy = {...current}; delete copy[product.id]; return copy })}><Trash2 size={16} /></button></div>)}
          <div className="store-total"><span>Total</span><strong>{money(store.currency, total)}</strong></div>
          {store.services?.length > 0 && <form onSubmit={bookAppointment} className="store-service-booking">
            <h3><CalendarClock size={17} /> Book a service</h3>
            <p className="store-muted">Choose a service and time. The business can confirm or reschedule it from Appointments & Services.</p>
            <label>Service<select required value={appointmentServiceId} onChange={event => setAppointmentServiceId(event.target.value)}><option value="">Choose a service</option>{store.services.map(item => <option key={item.id} value={item.id}>{item.name} · {item.durationMinutes} min · {money(store.currency, item.price)}</option>)}</select></label>
            <label>Preferred date and time<input required type="datetime-local" value={appointmentStartsAt} onChange={event => setAppointmentStartsAt(event.target.value)} /></label>
            <label>Full name<input required minLength={2} maxLength={100} value={customerName} onChange={event => setCustomerName(event.target.value)} /></label>
            <label>Phone<input required inputMode="tel" value={customerPhone} onChange={event => setCustomerPhone(event.target.value)} /></label>
            <button className="store-secondary" disabled={submitting}>{submitting ? 'Requesting…' : 'Request appointment'}</button>
            {appointmentMessage && <div className="store-muted">{appointmentMessage}</div>}
          </form>}
          <form onSubmit={checkout} className="store-form">
            <label>Full name<input required minLength={2} maxLength={100} value={customerName} onChange={event => setCustomerName(event.target.value)} /></label>
            <label>Customer phone<input required inputMode="tel" placeholder="0712 345 678" value={customerPhone} onChange={event => setCustomerPhone(event.target.value)} /></label>
            {table ? <label>Number of guests<input type="number" min={1} max={100} required value={guestCount} onChange={event => setGuestCount(Number(event.target.value))} /></label> : <label>Delivery or pickup location<textarea required minLength={2} maxLength={500} value={deliveryLocation} onChange={event => setDeliveryLocation(event.target.value)} /></label>}
            <fieldset className="store-payment-options"><legend>Payment method</legend>{([['MPESA','M-Pesa'],['CARD','Card'],['COD','Cash on delivery']] as const).map(([value,label]) => <label key={value} className={paymentMethod === value ? 'active' : ''}><input type="radio" name="paymentMethod" value={value} checked={paymentMethod === value} onChange={() => setPaymentMethod(value)} /><span><b>{value === 'COD' && table ? 'Pay your server' : label}</b><small>{value === 'MPESA' ? 'Pay now using an STK push' : value === 'CARD' ? 'Secure CyberSource checkout' : table ? 'Settle with your server after your meal' : 'Pay when the order arrives'}</small></span></label>)}</fieldset>
            <label>Order notes (optional)<textarea maxLength={500} value={notes} onChange={event => setNotes(event.target.value)} /></label>
            {error && <div className="store-error"><AlertCircle size={16} />{error}</div>}
            <button className="store-primary" disabled={submitting || !cartProducts.length || (tableId !== null && !table)}>{submitting ? 'Creating order…' : paymentMethod === 'COD' ? `${table ? "Place table order" : "Place COD order"} · ${money(store.currency, total)}` : paymentMethod === 'CARD' ? `Pay ${money(store.currency, total)} by card` : `Pay ${money(store.currency, total)} with M-Pesa`}</button>
          </form>
        </aside>
      </div>
      <footer className="store-footer">Secure checkout powered by Biashara360</footer>
      <style>{STORE_STYLES}</style>
    </main>
  )
}

const STORE_STYLES = `
  body { margin: 0; background: #f4f7f6; }
  .storefront-shell { min-height: 100vh; color: #132a27; font-family: Inter, system-ui, sans-serif; }
  .store-hero { padding: 42px max(24px, calc((100vw - 1180px)/2)); color: white; background: linear-gradient(120deg,#132a27,var(--store-primary)); display:flex; align-items:center; justify-content:space-between; gap:24px; }
  .store-hero.has-banner { background-size:cover; background-position:center; min-height:180px; }
  .store-hero h1 { margin:4px 0 8px; font-size:clamp(30px,5vw,54px); letter-spacing:-1.5px; } .store-hero p { margin:0; opacity:.86; }
  .store-eyebrow { text-transform:uppercase; letter-spacing:2px; font-size:11px; font-weight:800; }
  .store-cart-count { display:flex; gap:9px; align-items:center; padding:12px 18px; border:1px solid #ffffff42; border-radius:999px; background:#ffffff18; white-space:nowrap; }
  .store-layout { max-width:1180px; margin:0 auto; padding:30px 24px 60px; display:grid; grid-template-columns:minmax(0,1fr) 360px; gap:28px; align-items:start; }
  .store-tools { display:flex; flex-direction:column; gap:14px; margin-bottom:20px; } .store-search { display:flex; align-items:center; gap:10px; padding:12px 14px; background:white; border:1px solid #dbe5e3; border-radius:12px; }
  .store-search input { border:0; outline:0; font:inherit; width:100%; } .store-categories { display:flex; gap:8px; overflow:auto; padding-bottom:2px; }
  .store-categories button { border:1px solid #dbe5e3; background:white; border-radius:999px; padding:8px 14px; cursor:pointer; white-space:nowrap; } .store-categories button.active { background:var(--store-primary); color:white; border-color:var(--store-primary); }
  .store-products { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:16px; } .store-product { background:white; border:1px solid #dbe5e3; border-radius:16px; overflow:hidden; display:grid; grid-template-rows:150px 1fr auto; box-shadow:0 8px 25px #163d3610; }
  .store-empty { grid-column:1/-1; padding:44px 20px; text-align:center; color:#72817e; background:white; border:1px dashed #cbdad7; border-radius:16px; }
  .store-product-image { background:linear-gradient(135deg,#e7f4f1,#f8fbfa); display:flex; align-items:center; justify-content:center; color:var(--store-primary); } .store-product-image img { width:100%; height:100%; object-fit:cover; }
  .store-product-body { padding:17px; } .store-product-body span { color:var(--store-primary); text-transform:uppercase; font-size:10px; font-weight:800; letter-spacing:1px; } .store-product-body h2 { font-size:18px; margin:6px 0; } .store-product-body p { font-size:13px; line-height:1.5; color:#61706e; min-height:39px; } .store-product-body strong { font-size:18px; }
  .store-products.list { grid-template-columns:1fr; } .store-products.list .store-product { grid-template-columns:180px 1fr 170px; grid-template-rows:auto; align-items:center; } .store-products.list .store-product-image { height:100%; min-height:150px; }
  .store-quantity { padding:0 17px 17px; display:flex; align-items:center; gap:12px; } .store-quantity button { width:34px; height:34px; border:1px solid #cbdad7; background:white; border-radius:9px; display:grid; place-items:center; cursor:pointer; } .store-quantity .store-add { width:100%; background:#e8f5f2; border-color:#b8ddd5; color:#075e56; font-weight:750; }
  .store-checkout { position:sticky; top:18px; background:white; border:1px solid #dbe5e3; border-radius:18px; padding:22px; box-shadow:0 12px 35px #163d3614; } .store-checkout h2 { margin-top:0; }
  .store-cart-line { display:flex; align-items:center; justify-content:space-between; gap:10px; padding:12px 0; border-bottom:1px solid #edf2f1; } .store-cart-line strong,.store-cart-line span { display:block; font-size:13px; } .store-cart-line span { color:#657572; margin-top:4px; } .store-cart-line button { border:0; background:none; color:#b42318; cursor:pointer; }
  .store-total { display:flex; justify-content:space-between; padding:18px 0; font-size:18px; } .store-form { display:flex; flex-direction:column; gap:12px; } .store-form label { font-size:12px; font-weight:700; color:#52635f; }
  .store-form input,.store-form textarea { box-sizing:border-box; display:block; width:100%; margin-top:5px; padding:11px 12px; border:1px solid #cbdad7; border-radius:9px; font:inherit; color:#132a27; } .store-form textarea { resize:vertical; min-height:62px; }
  .store-payment-options { border:0; padding:0; margin:2px 0; display:grid; gap:8px; } .store-payment-options legend { font-size:12px; font-weight:800; color:#52635f; margin-bottom:7px; } .store-payment-options label { display:flex; align-items:center; gap:10px; padding:10px 11px; border:1px solid #cbdad7; border-radius:10px; cursor:pointer; } .store-payment-options label.active { border-color:var(--store-primary); background:#f0f8f6; } .store-payment-options input { width:auto; margin:0; } .store-payment-options span,.store-payment-options small { display:block; } .store-payment-options small { margin-top:2px; color:#72817e; font-weight:500; }
  .store-primary { border:0; border-radius:10px; padding:13px 18px; background:var(--store-primary); color:white; font-weight:800; cursor:pointer; width:100%; } .store-primary:disabled { opacity:.55; cursor:not-allowed; }
  .store-service-booking { display:flex; flex-direction:column; gap:10px; margin:8px 0 18px; padding:15px; border:1px solid #b8ddd5; border-radius:12px; background:#f0f8f6; } .store-service-booking h3 { display:flex; align-items:center; gap:7px; margin:0; color:#075e56; font-size:15px; } .store-service-booking label { font-size:12px; font-weight:700; color:#52635f; } .store-service-booking input,.store-service-booking select { box-sizing:border-box; display:block; width:100%; margin-top:5px; padding:10px 11px; border:1px solid #cbdad7; border-radius:9px; font:inherit; background:white; } .store-secondary { border:1px solid var(--store-primary); border-radius:10px; padding:11px 16px; background:white; color:#075e56; font-weight:800; cursor:pointer; } .store-secondary:disabled { opacity:.55; cursor:not-allowed; }
  .store-error { display:flex; align-items:flex-start; gap:8px; padding:10px 12px; color:#b42318; background:#fff0ee; border-radius:9px; font-size:12px; }
  .store-muted { color:#72817e; font-size:13px; } .store-footer { text-align:center; padding:20px; color:#72817e; font-size:12px; }
  .store-state,.store-result { min-height:70vh; display:flex; flex-direction:column; justify-content:center; align-items:center; text-align:center; gap:10px; padding:30px; } .store-result { max-width:520px; margin:auto; }
  .store-result h1 { font-size:34px; margin:0; } .store-payment-status { padding:7px 13px; border-radius:999px; background:#fff3cd; color:#8a5a00; font-weight:800; font-size:12px; } .store-payment-status.paid { background:#e6f6ed; color:#117a42; }
  @media(max-width:850px){ .store-layout{grid-template-columns:1fr}.store-checkout{position:static}.store-hero{align-items:flex-start;flex-direction:column}.store-products{grid-template-columns:repeat(2,minmax(0,1fr))} }
  @media(max-width:560px){ .store-products{grid-template-columns:1fr}.store-layout{padding:20px 14px 40px}.store-hero{padding:30px 18px}.store-product{grid-template-rows:130px 1fr auto}.store-products.list .store-product{grid-template-columns:1fr;grid-template-rows:130px 1fr auto}.store-products.list .store-product-image{min-height:130px} }
`
