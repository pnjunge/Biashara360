import React, { FormEvent, useEffect, useMemo, useState } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import { AlertCircle, CalendarClock, CheckCircle2, Minus, Plus, Search, ShoppingBag, Trash2, ShoppingCart, Heart, Grid2X2, List, Truck, ShieldCheck, Headphones, MapPin, Utensils, Coffee, User, Leaf } from 'lucide-react'
import { Storefront, StorefrontCheckoutResult, StorefrontProduct, storefrontApi } from '../services/api'

import './StorefrontPage.css'

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
  const [favorites, setFavorites] = useState<string[]>([])
  const [favoritesOnly, setFavoritesOnly] = useState(false)
  const [sort, setSort] = useState('featured')
  const [view, setView] = useState<'GRID' | 'LIST' | null>(null)
  const [checkoutOpen, setCheckoutOpen] = useState(false)
  const scrollTo = (id: string) => document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  const toggleFavorite = (id: string) => setFavorites(current => current.includes(id) ? current.filter(value => value !== id) : [...current, id])
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

  const orderComplete = Boolean(order && (paymentStatus === 'PAID' || (order.paymentMethod === 'COD' && !error)))

  useEffect(() => {
    if (!orderComplete) return
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
      setFavoritesOnly(false)
      setCheckoutOpen(false)
      setGuestCount(1)
      setPaymentMethod('MPESA')
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }, 2_500)
    return () => window.clearTimeout(timer)
  }, [orderComplete])

  const categories = useMemo(() => ['ALL', ...new Set(store?.products.map(product => product.category).filter(Boolean) || [])], [store])
  const visibleProducts = useMemo(() => {
    const term = search.trim().toLowerCase()
    return (store?.products || []).filter(product =>
      (!favoritesOnly || favorites.includes(product.id)) &&
      (category === 'ALL' || product.category === category) &&
      (!term || product.name.toLowerCase().includes(term) || product.description.toLowerCase().includes(term) || product.sku.toLowerCase().includes(term))
    ).sort((a, b) => sort === 'price-low' ? a.sellingPrice - b.sellingPrice : sort === 'price-high' ? b.sellingPrice - a.sellingPrice : sort === 'name' ? a.name.localeCompare(b.name) : 0)
  }, [store, search, category, favoritesOnly, favorites, sort])
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
        {orderComplete && <p>Returning to the store…</p>}
        {paymentStatus !== 'PAID' && <p>{order.customerMessage || (order.paymentMethod === 'COD' ? 'Pay when your order is delivered.' : 'Complete the M-Pesa prompt on your phone. This page checks payment automatically.')}</p>}
        {error && <div className="store-error"><AlertCircle size={16} />{error}</div>}
        {paymentStatus !== 'PAID' && order.paymentMethod === 'MPESA' && <button className="store-primary" disabled={submitting} onClick={retryPayment}>{submitting ? 'Requesting…' : 'Retry M-Pesa push'}</button>}
      </section>
    </main>
  )

  return (
    <main className="storefront-shell" style={{ '--store-primary': store.themeColor || '#0F766E' } as React.CSSProperties}>
      <header className="store-topbar">
        <a className="store-brand" href={`/shop/${encodeURIComponent(storeSlug)}`}><span className="store-brand-mark"><ShoppingCart size={40} /><Leaf size={19} /></span><span><strong>{store.businessName}</strong><small>EVERYDAY ESSENTIALS</small></span></a>
        <form className="store-search" onSubmit={event => { event.preventDefault(); scrollTo('store-catalog') }}><Search size={21} /><input aria-label="Search products" value={search} onChange={event => setSearch(event.target.value)} placeholder="Search products, e.g. food, drinks, snacks…" /><button type="submit">Search</button></form>
        <div className="store-header-actions"><button aria-pressed={favoritesOnly} onClick={() => { setFavoritesOnly(!favoritesOnly); scrollTo('store-catalog') }}><Heart size={22} fill={favoritesOnly ? 'currentColor' : 'none'} /><span>Favorites</span></button><a href="/login"><User size={22} /><span>Sign in</span></a><button className="store-header-cart" onClick={() => scrollTo('store-cart')}><span><ShoppingCart size={28} /><b>{itemCount}</b></span><span>Cart<small>{money(store.currency, total)}</small></span></button></div>
      </header>
      <nav className="store-nav" aria-label="Shop navigation"><div className="store-nav-categories">{categories.map(value => <button key={value} onClick={() => { setCategory(value); setFavoritesOnly(false); scrollTo('store-catalog') }}>{value === 'ALL' ? <Grid2X2 size={18} /> : value.toLowerCase().includes('food') ? <Utensils size={18} /> : <Coffee size={18} />}{value === 'ALL' ? 'All Products' : value}</button>)}</div><div className="store-nav-links"><button onClick={() => { setCheckoutOpen(true); scrollTo('store-cart') }}><Truck size={20} />Pickup / Delivery</button>{store.services?.length > 0 && <button onClick={() => scrollTo('store-booking')}><CalendarClock size={20} />Book a Service</button>}{store.county && <span><MapPin size={19} />{store.county}, Kenya</span>}</div></nav>
      <div className="store-layout">
        <section className="store-catalog" id="store-catalog">
          <header className={`store-hero ${store.bannerUrl ? 'has-banner' : ''}`} style={store.bannerUrl ? { backgroundImage: `linear-gradient(90deg, #edf7f1 25%, #edf7f133), url(${store.bannerUrl})` } : undefined}>
            <div className="store-hero-copy"><p className="store-eyebrow">{store.businessName}</p><h1>{store.headline || 'Shop with us online'}</h1><p>{store.description || store.welcomeMessage || 'Quality products. Great service. Closer to you.'}</p><div className="store-benefits"><span><Truck />Fast & Reliable</span><span><ShieldCheck />Secure Payments</span><span><Headphones />Customer Support</span></div></div>
            {!store.bannerUrl && <div className="store-hero-art" aria-hidden="true"><Leaf size={130} /><ShoppingBag size={100} /><span>Everyday essentials</span></div>}
          </header>
          <div className="store-tools">
            <div className="store-categories">{categories.map(value => <button key={value} className={category === value ? 'active' : ''} onClick={() => setCategory(value)}><Grid2X2 size={16} />{value === 'ALL' ? 'All Products' : value}</button>)}{favoritesOnly && <button className="active" onClick={() => setFavoritesOnly(false)}>Favorites ×</button>}</div>
            <div className="store-view-tools"><select aria-label="Sort products" value={sort} onChange={event => setSort(event.target.value)}><option value="featured">Featured</option><option value="price-low">Price: low to high</option><option value="price-high">Price: high to low</option><option value="name">Name: A to Z</option></select><button aria-label="Grid view" aria-pressed={(view || store.layout) !== 'LIST'} onClick={() => setView('GRID')}><Grid2X2 size={20} /></button><button aria-label="List view" aria-pressed={(view || store.layout) === 'LIST'} onClick={() => setView('LIST')}><List size={20} /></button></div>
          </div>
          <div className={`store-products ${(view || store.layout) === 'LIST' ? 'list' : ''}`}>
            {!visibleProducts.length && <div className="store-empty">No products match your search.</div>}
            {visibleProducts.map(product => (
              <article className="store-product" key={product.id}>
                <div className="store-product-image"><button className="store-favorite" aria-label={`Favorite ${product.name}`} aria-pressed={favorites.includes(product.id)} onClick={() => toggleFavorite(product.id)}><Heart size={19} fill={favorites.includes(product.id) ? 'currentColor' : 'none'} /></button>{product.imageUrl ? <img src={product.imageUrl} alt="" onError={event => { event.currentTarget.style.display = 'none' }} /> : <ShoppingBag size={34} />}</div>
                <div className="store-product-body"><span>{product.category || 'Product'}</span><h2>{product.name}</h2><p>{product.description || `SKU ${product.sku}`}</p><strong>{money(store.currency, product.sellingPrice)}</strong></div>
                <div className="store-quantity">
                  {cart[product.id] ? <><button aria-label={`Remove one ${product.name}`} onClick={() => changeQuantity(product, -1)}><Minus size={15} /></button><b>{cart[product.id]}</b><button aria-label={`Add one ${product.name}`} disabled={cart[product.id] >= product.availableQuantity} onClick={() => changeQuantity(product, 1)}><Plus size={15} /></button></> : <button className="store-add" disabled={product.availableQuantity <= 0} onClick={() => changeQuantity(product, 1)}><ShoppingCart size={18} />{product.availableQuantity <= 0 ? 'Out of stock' : 'Add to cart'}</button>}
                </div>
              </article>
            ))}
          </div>
        </section>
        <aside className="store-checkout" id="store-cart">
          <div className="store-cart-heading"><h2><ShoppingCart size={27} />{table ? `Order for ${table.name}` : 'Your cart'}</h2><span>{itemCount} items</span></div>
          {tableId !== null && !table && <p className="store-error">This table is unavailable. Please ask your server for a new QR code.</p>}
          {table && <p className="store-muted">{table.area} · Food goes to the kitchen; drinks stay on your bill.</p>}
          {!cartProducts.length ? <div className="store-empty-cart"><ShoppingCart size={32} /><div><strong>Your cart is empty</strong><p>Add products to get started</p></div></div> : cartProducts.map(product => <div className="store-cart-line" key={product.id}><div><strong>{product.name}</strong><span>{cart[product.id]} × {money(store.currency, product.sellingPrice)}</span></div><button aria-label={`Remove ${product.name}`} onClick={() => setCart(current => { const copy = {...current}; delete copy[product.id]; return copy })}><Trash2 size={16} /></button></div>)}
          <div className="store-total"><span>Total</span><strong>{money(store.currency, total)}</strong></div>
          {store.services?.length > 0 && <form onSubmit={bookAppointment} className="store-service-booking" id="store-booking">
            <h3><CalendarClock size={17} /> Book a service</h3>
            <p className="store-muted">Choose a service and time. The business can confirm or reschedule it from Appointments & Services.</p>
            <label>Service<select required value={appointmentServiceId} onChange={event => setAppointmentServiceId(event.target.value)}><option value="">Choose a service</option>{store.services.map(item => <option key={item.id} value={item.id}>{item.name} · {item.durationMinutes} min · {money(store.currency, item.price)}</option>)}</select></label>
            <label>Preferred date and time<input required type="datetime-local" value={appointmentStartsAt} onChange={event => setAppointmentStartsAt(event.target.value)} /></label>
            <label>Full name<input required minLength={2} maxLength={100} placeholder="Enter your full name" value={customerName} onChange={event => setCustomerName(event.target.value)} /></label>
            <label>Phone<input required inputMode="tel" placeholder="Enter your phone number" value={customerPhone} onChange={event => setCustomerPhone(event.target.value)} /></label>
            <button className="store-secondary" disabled={submitting}>{submitting ? 'Requesting…' : 'Request appointment'}</button>
            {appointmentMessage && <div className="store-muted">{appointmentMessage}</div>}
          </form>}
          {(cartProducts.length > 0 || checkoutOpen || tableId !== null) && <form onSubmit={checkout} className="store-form">
            <label>Full name<input required minLength={2} maxLength={100} placeholder="Enter your full name" value={customerName} onChange={event => setCustomerName(event.target.value)} /></label>
            <label>Customer phone<input required inputMode="tel" placeholder="0712 345 678" value={customerPhone} onChange={event => setCustomerPhone(event.target.value)} /></label>
            {table ? <label>Number of guests<input type="number" min={1} max={100} required value={guestCount} onChange={event => setGuestCount(Number(event.target.value))} /></label> : <label>Delivery or pickup location<textarea required minLength={2} maxLength={500} value={deliveryLocation} onChange={event => setDeliveryLocation(event.target.value)} /></label>}
            <fieldset className="store-payment-options"><legend>Payment method</legend>{([['MPESA','M-Pesa'],['CARD','Card'],['COD','Cash on delivery']] as const).map(([value,label]) => <label key={value} className={paymentMethod === value ? 'active' : ''}><input type="radio" name="paymentMethod" value={value} checked={paymentMethod === value} onChange={() => setPaymentMethod(value)} /><span><b>{value === 'COD' && table ? 'Pay your server' : label}</b><small>{value === 'MPESA' ? 'Pay now using an STK push' : value === 'CARD' ? 'Secure CyberSource checkout' : table ? 'Settle with your server after your meal' : 'Pay when the order arrives'}</small></span></label>)}</fieldset>
            <label>Order notes (optional)<textarea maxLength={500} value={notes} onChange={event => setNotes(event.target.value)} /></label>
            {error && <div className="store-error"><AlertCircle size={16} />{error}</div>}
            <button className="store-primary" disabled={submitting || !cartProducts.length || (tableId !== null && !table)}>{submitting ? 'Creating order…' : paymentMethod === 'COD' ? `${table ? "Place table order" : "Place COD order"} · ${money(store.currency, total)}` : paymentMethod === 'CARD' ? `Pay ${money(store.currency, total)} by card` : `Pay ${money(store.currency, total)} with M-Pesa`}</button>
          </form>}
        </aside>
      </div>
      <footer className="store-footer">Secure checkout powered by Biashara360</footer>
    </main>
  )
}
