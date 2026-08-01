import React, { useState, useEffect } from 'react'
import { PageHeader, Card, Btn, Input, Select } from '../components/ui'
import { Search, ShoppingCart, Plus, Minus, Trash2, User, CreditCard, CheckCircle, Store, Smartphone } from 'lucide-react'
import { orderApi, productApi, customerApi, paymentApi, settingsApi, ProductResponse, CustomerResponse, MpesaConfigResponse, OrderResponse } from '../services/api'

interface CartItem {
  product: ProductResponse
  quantity: number
}

export function PosPage() {
  const [products, setProducts] = useState<ProductResponse[]>([])
  const [customers, setCustomers] = useState<CustomerResponse[]>([])
  const [loading, setLoading] = useState(true)
  
  // Search & Filters
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedCategory, setSelectedCategory] = useState('All')
  
  // Cart & Customer State
  const [cart, setCart] = useState<CartItem[]>([])
  const [selectedCustomerId, setSelectedCustomerId] = useState('')
  const [customerName, setCustomerName] = useState('Walk-In Customer')
  const [customerPhone, setCustomerPhone] = useState('')
  const [paymentMethod, setPaymentMethod] = useState<'CASH' | 'MPESA' | 'CARD'>('CASH')
  const [notes, setNotes] = useState('')
  
  // Processing States
  const [isCheckingOut, setIsCheckingOut] = useState(false)
  const [error, setError] = useState('')
  const [checkoutSuccess, setCheckoutSuccess] = useState(false)
  const [createdOrderNumber, setCreatedOrderNumber] = useState('')

  // M-Pesa STK push states
  const [stkStep, setStkStep] = useState<'idle' | 'confirm_phone' | 'pushing' | 'pushed' | 'error'>('idle')
  const [stkPhone, setStkPhone] = useState('')
  const [stkOrderId, setStkOrderId] = useState('')
  const [stkError, setStkError] = useState('')
  const [stkAmount, setStkAmount] = useState(0)
  const [cartBackup, setCartBackup] = useState<CartItem[]>([])
  const [mpesaChannels, setMpesaChannels] = useState<MpesaConfigResponse[]>([])
  const [mpesaAccountType, setMpesaAccountType] = useState('')

  // Card Payment link modal state
  const [cardModalOrder, setCardModalOrder] = useState<OrderResponse | null>(null)
  const [cardPaid, setCardPaid] = useState(false)

  useEffect(() => {
    if (!cardModalOrder || cardPaid) return
    const timer = setInterval(async () => {
      try {
        const res = await orderApi.get(cardModalOrder.id)
        if (res.success && res.data && res.data.paymentStatus === 'PAID') {
          setCardPaid(true)
        }
      } catch (e) { /* ignore */ }
    }, 3000)
    return () => clearInterval(timer)
  }, [cardModalOrder, cardPaid])

  useEffect(() => {
    setLoading(true)
    Promise.all([
      productApi.list(),
      customerApi.list(),
      settingsApi.getMpesaChannels()
    ]).then(([prodRes, custRes, mpesaRes]) => {
      if (prodRes.success && prodRes.data) setProducts(prodRes.data)
      if (custRes.success && custRes.data) setCustomers(custRes.data)
      if (mpesaRes.success && mpesaRes.data) {
        setMpesaChannels(mpesaRes.data)
        setMpesaAccountType(mpesaRes.data[0]?.accountType || '')
      }
    }).catch(err => {
      console.error("Failed to load POS resources", err)
    }).finally(() => setLoading(false))
  }, [])

  const categories = ['All', ...Array.from(new Set(products.map(p => p.category).filter(Boolean)))]

  const filteredProducts = products.filter(p => {
    const matchesSearch = p.name.toLowerCase().includes(searchQuery.toLowerCase()) || 
                          p.sku.toLowerCase().includes(searchQuery.toLowerCase())
    const matchesCategory = selectedCategory === 'All' || p.category === selectedCategory
    return matchesSearch && matchesCategory
  })

  const handleSelectCustomer = (custId: string) => {
    setSelectedCustomerId(custId)
    if (custId === '') {
      setCustomerName('Walk-In Customer')
      setCustomerPhone('')
    } else {
      const cust = customers.find(c => c.id === custId)
      if (cust) {
        setCustomerName(cust.name)
        setCustomerPhone(cust.phone)
      }
    }
  }

  const addToCart = (product: ProductResponse) => {
    setError('')
    setCart(prev => {
      const existing = prev.find(item => item.product.id === product.id)
      if (existing) {
        if (existing.quantity >= product.currentStock) {
          setError(`Cannot add more. Only ${product.currentStock} items in stock.`)
          return prev
        }
        return prev.map(item => 
          item.product.id === product.id 
            ? { ...item, quantity: item.quantity + 1 } 
            : item
        )
      }
      return [...prev, { product, quantity: 1 }]
    })
  }

  const updateCartQty = (productId: string, delta: number) => {
    setError('')
    setCart(prev => {
      return prev.map(item => {
        if (item.product.id !== productId) return item
        const newQty = item.quantity + delta
        if (newQty <= 0) return null
        if (delta > 0 && newQty > item.product.currentStock) {
          setError(`Insufficient stock. Only ${item.product.currentStock} available.`)
          return item
        }
        return { ...item, quantity: newQty }
      }).filter(Boolean) as CartItem[]
    })
  }

  const removeFromCart = (productId: string) => {
    setCart(prev => prev.filter(item => item.product.id !== productId))
  }

  const clearCart = () => {
    setCart([])
    setError('')
  }

  const subtotal = cart.reduce((sum, item) => sum + item.quantity * item.product.sellingPrice, 0)
  const tax = subtotal * 0.16
  const total = subtotal + tax

  // ── STK Push helpers ─────────────────────────────────────────────────────────

  const sendStkPush = async () => {
    if (!stkPhone.trim()) { setStkError('Phone number is required.'); return }
    setStkStep('pushing')
    setStkError('')
    try {
      const res = await paymentApi.initiate({
        orderId: stkOrderId,
        phoneNumber: stkPhone.trim(),
        ...(mpesaAccountType ? { accountType: mpesaAccountType } : {})
      })
      if (res.success) {
        setStkStep('pushed')
      } else {
        setStkError(res.message || 'STK push failed. Please try again.')
        setStkStep('error')
      }
    } catch (e: any) {
      setStkError(e.response?.data?.message || 'Network error. Could not send M-Pesa prompt.')
      setStkStep('error')
    }
  }

  const dismissStk = () => {
    setStkStep('idle')
    setStkPhone('')
    setStkOrderId('')
    setStkError('')
    setStkAmount(0)
  }

  const handleCancelCheckout = async () => {
    const oid = stkOrderId
    dismissStk()
    setCheckoutSuccess(false)
    if (oid) {
      try {
        await orderApi.cancel(oid)
      } catch (err) {
        console.error("Failed to cancel order:", err)
      }
    }
    // Restore cart & refresh stock
    setCart(cartBackup)
    setCartBackup([])
    const prodRes = await productApi.list()
    if (prodRes.success && prodRes.data) {
      setProducts(prodRes.data)
    }
  }

  // ── Checkout ─────────────────────────────────────────────────────────────────

  const handleCheckout = async () => {
    if (cart.length === 0) { setError('Your shopping cart is empty.'); return }
    if (!customerName.trim() || !customerPhone.trim()) { setError('Please provide customer name and phone number.'); return }

    setIsCheckingOut(true)
    setError('')
    try {
      const payload = {
        customerName,
        customerPhone,
        deliveryLocation: 'In-Store POS',
        paymentMethod,
        notes: notes || 'POS Checkout Sale',
        customerId: selectedCustomerId || null,
        items: cart.map(item => ({
          productId: item.product.id,
          quantity: item.quantity,
          unitPrice: item.product.sellingPrice
        }))
      }

      const res = await orderApi.create(payload)
      if (res.success && res.data) {
        setCreatedOrderNumber(res.data.orderNumber)
        setStkAmount(total)
        setCartBackup([...cart])
        clearCart()
        setNotes('')
        // Refresh stock
        const updatedProds = await productApi.list()
        if (updatedProds.success && updatedProds.data) setProducts(updatedProds.data)

        if (paymentMethod === 'MPESA') {
          // Trigger STK push flow
          setStkOrderId(res.data.id)
          setStkPhone(customerPhone.startsWith('+254') ? customerPhone : customerPhone)
          setStkStep('confirm_phone')
        } else if (paymentMethod === 'CARD') {
          setCardModalOrder(res.data)
          setCardPaid(false)
        } else {
          setCheckoutSuccess(true)
        }
      } else {
        setError(res.message || 'Checkout transaction failed.')
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Network error occurred during checkout processing.')
    } finally {
      setIsCheckingOut(false)
    }
  }

  return (
    <div className="fade-in pos-page-layout" style={{ display: 'flex', flexDirection: 'column', gap: 20, height: 'calc(100vh - 120px)' }}>
      <PageHeader title="Point of Sale" />
      <p style={{ color: 'var(--b360-text-secondary)', fontSize: 13, marginTop: -15, marginBottom: 10 }}>Process in-store checkout orders instantly</p>

      {/* ── Card Payment Link Modal ── */}
      {cardModalOrder && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div style={{ background: 'white', borderRadius: 16, padding: 32, width: 420, boxShadow: '0 20px 60px rgba(0,0,0,0.2)', position: 'relative' }}>
            <button
              onClick={() => {
                setCardModalOrder(null)
                setCheckoutSuccess(true)
              }}
              style={{ position: 'absolute', right: 20, top: 16, border: 'none', background: 'none', fontSize: 24, cursor: 'pointer', color: 'var(--b360-text-secondary)', fontWeight: 'bold' }}
            >
              ×
            </button>

            <div style={{ textAlign: 'center', marginBottom: 20 }}>
              <div style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center', width: 56, height: 56, background: cardPaid ? '#DCFCE7' : '#EFF6FF', borderRadius: '50%', marginBottom: 12 }}>
                <CreditCard size={28} color={cardPaid ? 'var(--b360-green)' : '#2563EB'} />
              </div>
              <h3 style={{ fontWeight: 700, fontSize: 18, marginBottom: 4 }}>
                {cardPaid ? 'Card Payment Received!' : 'Card Payment Link Generated'}
              </h3>
              <p style={{ fontSize: 13, color: 'var(--b360-text-secondary)' }}>
                {cardPaid ? 'Payment callback received and verified via CyberSource.' : 'Send this payment link to the customer to complete card payment.'}
              </p>
            </div>

            <div style={{ background: 'var(--b360-surface)', borderRadius: 10, padding: 14, marginBottom: 16, fontSize: 13 }}>
              <strong>📋 Order:</strong> {cardModalOrder.orderNumber}<br />
              <strong>💰 Amount:</strong> KES {cardModalOrder.subtotal.toLocaleString()}<br />
              <strong>👤 Customer:</strong> {cardModalOrder.customerName} ({cardModalOrder.customerPhone})
            </div>

            {!cardPaid ? (
              <>
                <div style={{ marginBottom: 16 }}>
                  <label style={{ fontSize: 11, fontWeight: 600, color: 'var(--b360-text-secondary)', display: 'block', marginBottom: 4 }}>Customer Payment Link</label>
                  <input
                    readOnly
                    value={`${window.location.origin}/pay/card?orderId=${cardModalOrder.id}&businessId=${cardModalOrder.businessId}`}
                    style={{ width: '100%', padding: '10px 12px', fontSize: 12, fontFamily: 'monospace', borderRadius: 8, border: '1px solid var(--b360-border)', background: '#F8FAFC' }}
                  />
                </div>
                <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
                  <div style={{ flex: 1 }}>
                    <Btn small onClick={() => {
                      const url = `${window.location.origin}/pay/card?orderId=${cardModalOrder.id}&businessId=${cardModalOrder.businessId}`
                      navigator.clipboard.writeText(url)
                      alert('Card payment link copied to clipboard!')
                    }}>📋 Copy Link</Btn>
                  </div>
                  <div style={{ flex: 1 }}>
                    <Btn small variant="secondary" onClick={() => {
                      const url = `${window.location.origin}/pay/card?orderId=${cardModalOrder.id}&businessId=${cardModalOrder.businessId}`
                      window.open(`https://wa.me/${cardModalOrder.customerPhone.replace(/[^0-9]/g, '')}?text=${encodeURIComponent(`Please complete your card payment for Order ${cardModalOrder.orderNumber}: ${url}`)}`, '_blank')
                    }}>💬 WhatsApp</Btn>
                  </div>
                </div>
                <div style={{ textAlign: 'center', fontSize: 12, color: '#2563EB', background: '#EFF6FF', padding: 10, borderRadius: 8, fontWeight: 600 }}>
                  ⏳ Waiting for customer card payment callback...
                </div>
              </>
            ) : (
              <div style={{ textAlign: 'center', padding: 12, background: '#DCFCE7', color: '#15803D', borderRadius: 8, fontWeight: 700, fontSize: 14 }}>
                ✓ Order status automatically updated to PAID!
              </div>
            )}

            <button
              onClick={() => {
                setCardModalOrder(null)
                setCheckoutSuccess(true)
              }}
              style={{ width: '100%', padding: '12px 0', background: 'var(--b360-green)', color: 'white', border: 'none', borderRadius: 8, fontWeight: 700, fontSize: 14, cursor: 'pointer', marginTop: 16 }}
            >
              ✓ Done — Return to POS
            </button>
          </div>
        </div>
      )}

      {/* ── M-Pesa STK Push Modal ── */}
      {stkStep !== 'idle' && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div style={{ background: 'white', borderRadius: 16, padding: 32, width: 380, boxShadow: '0 20px 60px rgba(0,0,0,0.2)', position: 'relative' }}>
            <button
              onClick={() => {
                if (stkStep === 'pushed') {
                  dismissStk()
                  setCheckoutSuccess(true)
                } else {
                  handleCancelCheckout()
                }
              }}
              style={{ position: 'absolute', right: 20, top: 16, border: 'none', background: 'none', fontSize: 24, cursor: 'pointer', color: 'var(--b360-text-secondary)', fontWeight: 'bold' }}
            >
              ×
            </button>

            {/* Confirm Phone */}
            {stkStep === 'confirm_phone' && (
              <>
                <div style={{ textAlign: 'center', marginBottom: 20 }}>
                  <div style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center', width: 56, height: 56, background: '#E8F5E9', borderRadius: '50%', marginBottom: 12 }}>
                    <Smartphone size={28} color="var(--b360-green)" />
                  </div>
                  <h3 style={{ fontWeight: 700, fontSize: 18, marginBottom: 4 }}>M-Pesa Payment</h3>
                  <p style={{ fontSize: 13, color: 'var(--b360-text-secondary)' }}>
                    Confirm the phone number to send the M-Pesa prompt to.
                  </p>
                </div>
                <div style={{ background: 'var(--b360-surface)', borderRadius: 10, padding: '10px 14px', textAlign: 'center', marginBottom: 16 }}>
                  <div style={{ fontSize: 12, color: 'var(--b360-text-secondary)' }}>Amount</div>
                  <div style={{ fontWeight: 800, fontSize: 22, color: 'var(--b360-green)' }}>KES {stkAmount.toLocaleString()}</div>
                </div>
                <Input
                  label="M-Pesa Phone Number"
                  value={stkPhone}
                  onChange={setStkPhone}
                  placeholder="+254 7XX XXX XXX"
                />
                {mpesaChannels.length > 1 && (
                  <Select
                    label="M-Pesa channel"
                    value={mpesaAccountType}
                    onChange={setMpesaAccountType}
                    options={mpesaChannels.map(channel => ({
                      value: channel.accountType,
                      label: `${channel.accountType === 'paybill' ? 'Paybill' : 'Till'} — ${channel.shortCode}`
                    }))}
                  />
                )}
                {stkError && <p style={{ color: 'var(--b360-red)', fontSize: 12, marginTop: 6 }}>{stkError}</p>}
                <div style={{ display: 'flex', gap: 10, marginTop: 16 }}>
                  <Btn variant="secondary" onClick={handleCancelCheckout}>Cancel</Btn>
                  <button onClick={sendStkPush} style={{ flex: 1, padding: '10px 16px', background: 'var(--b360-green)', color: 'white', border: 'none', borderRadius: 8, fontWeight: 700, fontSize: 13, cursor: 'pointer' }}>Send M-Pesa Prompt →</button>
                </div>
              </>
            )}

            {/* Pushing */}
            {stkStep === 'pushing' && (
              <div style={{ textAlign: 'center', padding: '20px 0' }}>
                <div style={{ fontSize: 40, marginBottom: 16 }}>⏳</div>
                <h3 style={{ fontWeight: 700, fontSize: 16, marginBottom: 8 }}>Sending M-Pesa Prompt…</h3>
                <p style={{ fontSize: 13, color: 'var(--b360-text-secondary)' }}>Contacting Safaricom Daraja API…</p>
              </div>
            )}

            {/* Pushed — waiting for PIN */}
            {stkStep === 'pushed' && (
              <>
                <div style={{ textAlign: 'center', marginBottom: 20 }}>
                  <div style={{ fontSize: 48, marginBottom: 12 }}>📱</div>
                  <h3 style={{ fontWeight: 700, fontSize: 18, marginBottom: 6 }}>Check Your Phone!</h3>
                  <p style={{ fontSize: 13, color: 'var(--b360-text-secondary)' }}>
                    An M-Pesa payment prompt has been sent to <strong>{stkPhone}</strong>.
                    Ask the customer to enter their M-Pesa PIN to complete the payment.
                  </p>
                </div>
                <div style={{ background: '#E8F5E9', borderRadius: 10, padding: 14, marginBottom: 16, fontSize: 13 }}>
                  <strong>📋 Order:</strong> {createdOrderNumber}<br />
                  <strong>💰 Amount:</strong> KES {stkAmount.toLocaleString()}<br />
                  <strong>📞 Sent to:</strong> {stkPhone}
                </div>
                <p style={{ fontSize: 11, color: 'var(--b360-text-secondary)', textAlign: 'center', marginBottom: 12 }}>
                  The payment will be automatically reconciled once the customer confirms.
                </p>
                <button onClick={() => { dismissStk(); setCheckoutSuccess(true) }} style={{ width: '100%', padding: '12px 0', background: 'var(--b360-green)', color: 'white', border: 'none', borderRadius: 8, fontWeight: 700, fontSize: 14, cursor: 'pointer' }}>
                  ✓ Done — New Sale
                </button>
              </>
            )}

            {/* STK Error */}
            {stkStep === 'error' && (
              <>
                <div style={{ textAlign: 'center', marginBottom: 20 }}>
                  <div style={{ fontSize: 40, marginBottom: 12 }}>⚠️</div>
                  <h3 style={{ fontWeight: 700, fontSize: 18, marginBottom: 6 }}>STK Push Failed</h3>
                  <p style={{ fontSize: 13, color: 'var(--b360-red)' }}>{stkError}</p>
                </div>
                <p style={{ fontSize: 12, color: 'var(--b360-text-secondary)', marginBottom: 16 }}>
                  The order <strong>{createdOrderNumber}</strong> was created. You can retry the M-Pesa push or collect payment manually via the Payments menu.
                </p>
                <div style={{ display: 'flex', gap: 10 }}>
                  <Btn variant="secondary" onClick={() => setStkStep('confirm_phone')}>Retry</Btn>
                  <button onClick={handleCancelCheckout} style={{ flex: 1, padding: '10px 0', background: 'var(--b360-green)', color: 'white', border: 'none', borderRadius: 8, fontWeight: 700, fontSize: 13, cursor: 'pointer' }}>Close</button>
                </div>
              </>
            )}
          </div>
        </div>
      )}

      {checkoutSuccess ? (
        <Card style={{ maxWidth: 500, margin: '40px auto', textAlign: 'center', padding: 40 }}>
          <div style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center', width: 64, height: 64, borderRadius: '50%', backgroundColor: 'var(--b360-green-light)', color: 'var(--b360-green)', marginBottom: 20 }}>
            <CheckCircle size={36} />
          </div>
          <h2 style={{ fontSize: 22, fontWeight: 700, color: 'var(--b360-sidebar-bg)', marginBottom: 12 }}>Checkout Successful!</h2>
          <p style={{ color: 'var(--b360-text-secondary)', marginBottom: 8 }}>Transaction has been successfully created and recorded.</p>
          <div style={{ padding: 12, backgroundColor: 'var(--b360-surface)', borderRadius: 8, fontFamily: 'monospace', fontWeight: 700, fontSize: 16, display: 'inline-block', marginBottom: 24 }}>
            Order: {createdOrderNumber}
          </div>
          <div>
            <Btn onClick={() => setCheckoutSuccess(false)} icon={<Store size={14} />}>Open New Session</Btn>
          </div>
        </Card>
      ) : (
        <div className="split-layout" style={{ flex: 1, minHeight: 0 }}>
          
          {/* Left Column: Product Catalog */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16, minHeight: 0 }}>
            
            <Card style={{ padding: 16 }}>
              <div style={{ display: 'flex', gap: 12, alignItems: 'center', flexWrap: 'wrap' }}>
                <div style={{ position: 'relative', flex: 1, minWidth: 200 }}>
                  <Search size={16} style={{ position: 'absolute', left: 12, top: '50%', transform: 'translateY(-50%)', color: 'gray' }} />
                  <input
                    style={{ width: '100%', padding: '10px 12px 10px 36px', border: '1px solid var(--b360-border)', borderRadius: 8, fontSize: 14 }}
                    placeholder="Search by SKU or product name..."
                    value={searchQuery}
                    onChange={e => setSearchQuery(e.target.value)}
                  />
                </div>
                <div style={{ display: 'flex', gap: 6, overflowX: 'auto', paddingBottom: 4 }}>
                  {categories.map(cat => (
                    <button
                      key={cat}
                      onClick={() => setSelectedCategory(cat)}
                      style={{
                        padding: '8px 14px',
                        borderRadius: 20,
                        border: 'none',
                        backgroundColor: selectedCategory === cat ? 'var(--b360-green)' : 'var(--b360-border)',
                        color: selectedCategory === cat ? 'white' : 'var(--b360-sidebar-bg)',
                        fontSize: 13,
                        fontWeight: 600,
                        cursor: 'pointer',
                        whiteSpace: 'nowrap',
                        transition: 'all 0.2s'
                      }}
                    >
                      {cat}
                    </button>
                  ))}
                </div>
              </div>
            </Card>

            <div style={{ flex: 1, overflowY: 'auto', paddingRight: 4 }}>
              {loading ? (
                <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: 200, color: 'var(--b360-text-secondary)' }}>Loading catalog...</div>
              ) : filteredProducts.length === 0 ? (
                <div style={{ textAlign: 'center', padding: 40, color: 'var(--b360-text-secondary)' }}>No active products match your criteria.</div>
              ) : (
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))', gap: 16 }}>
                  {filteredProducts.map(p => {
                    const isOutOfStock = p.currentStock <= 0
                    return (
                      <div
                        key={p.id}
                        onClick={() => !isOutOfStock && addToCart(p)}
                        style={{
                          backgroundColor: 'white',
                          border: '1px solid var(--b360-border)',
                          borderRadius: 12,
                          padding: 14,
                          display: 'flex',
                          flexDirection: 'column',
                          gap: 8,
                          cursor: isOutOfStock ? 'not-allowed' : 'pointer',
                          transition: 'all 0.2s',
                          opacity: isOutOfStock ? 0.6 : 1,
                          boxShadow: '0 2px 4px rgba(0,0,0,0.02)'
                        }}
                        onMouseEnter={e => { if(!isOutOfStock) e.currentTarget.style.borderColor = 'var(--b360-green)' }}
                        onMouseLeave={e => { if(!isOutOfStock) e.currentTarget.style.borderColor = 'var(--b360-border)' }}
                      >
                        <div style={{ height: 100, backgroundColor: 'var(--b360-surface)', borderRadius: 8, display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--b360-green)', fontWeight: 800, fontSize: 24 }}>
                          {p.name.substring(0, 2).toUpperCase()}
                        </div>
                        <div>
                          <div style={{ fontSize: 12, color: 'gray', fontFamily: 'monospace' }}>{p.sku}</div>
                          <div style={{ fontWeight: 600, color: 'var(--b360-sidebar-bg)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }} title={p.name}>
                            {p.name}
                          </div>
                        </div>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 'auto' }}>
                          <span style={{ fontWeight: 700, color: 'var(--b360-green)', fontSize: 14 }}>
                            KES {p.sellingPrice.toLocaleString()}
                          </span>
                          <span style={{
                            fontSize: 11, fontWeight: 600, padding: '2px 6px', borderRadius: 4,
                            backgroundColor: isOutOfStock ? '#FEE2E2' : p.isLowStock ? '#FEF3C7' : '#D1FAE5',
                            color: isOutOfStock ? '#EF4444' : p.isLowStock ? '#D97706' : 'var(--b360-green-dark)'
                          }}>
                            {isOutOfStock ? 'Out of Stock' : `${p.currentStock} left`}
                          </span>
                        </div>
                      </div>
                    )
                  })}
                </div>
              )}
            </div>
          </div>

          {/* Right Column: Checkout Cart */}
          <div style={{ display: 'flex', flexDirection: 'column', minHeight: 0 }}>
            <Card style={{ display: 'flex', flexDirection: 'column', height: '100%', padding: 20, gap: 16 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid var(--b360-border)', paddingBottom: 12 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontWeight: 700, color: 'var(--b360-sidebar-bg)', fontSize: 16 }}>
                  <ShoppingCart size={18} />
                  Checkout Cart ({cart.reduce((sum, i) => sum + i.quantity, 0)})
                </div>
                {cart.length > 0 && (
                  <button onClick={clearCart} style={{ color: 'var(--b360-red)', border: 'none', background: 'none', fontSize: 12, fontWeight: 600, cursor: 'pointer' }}>
                    Clear
                  </button>
                )}
              </div>

              {error && (
                <div style={{ backgroundColor: '#FEF2F2', border: '1px solid #FCA5A5', color: '#B91C1C', padding: 10, borderRadius: 8, fontSize: 12, fontWeight: 500 }}>
                  {error}
                </div>
              )}

              <div style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: 10 }}>
                {cart.length === 0 ? (
                  <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', color: 'var(--b360-text-secondary)', gap: 8 }}>
                    <ShoppingCart size={32} style={{ opacity: 0.3 }} />
                    <span style={{ fontSize: 13 }}>Cart is currently empty.</span>
                    <span style={{ fontSize: 11, textAlign: 'center', opacity: 0.7 }}>Click on catalog products to assemble an order.</span>
                  </div>
                ) : (
                  cart.map(item => (
                    <div key={item.product.id} style={{ display: 'flex', gap: 12, alignItems: 'center', padding: '10px 0', borderBottom: '1px solid var(--b360-surface)' }}>
                      <div style={{ flex: 1 }}>
                        <div style={{ fontWeight: 600, fontSize: 13, color: 'var(--b360-sidebar-bg)' }}>{item.product.name}</div>
                        <div style={{ fontSize: 11, color: 'gray' }}>KES {item.product.sellingPrice.toLocaleString()}</div>
                      </div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                        <button onClick={() => updateCartQty(item.product.id, -1)} style={{ width: 24, height: 24, borderRadius: 12, border: '1px solid var(--b360-border)', background: 'white', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}>
                          <Minus size={12} />
                        </button>
                        <span style={{ fontWeight: 700, fontSize: 13, minWidth: 20, textAlign: 'center' }}>{item.quantity}</span>
                        <button onClick={() => updateCartQty(item.product.id, 1)} style={{ width: 24, height: 24, borderRadius: 12, border: '1px solid var(--b360-border)', background: 'white', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}>
                          <Plus size={12} />
                        </button>
                      </div>
                      <div style={{ fontWeight: 700, fontSize: 13, minWidth: 70, textAlign: 'right', color: 'var(--b360-sidebar-bg)' }}>
                        KES {(item.quantity * item.product.sellingPrice).toLocaleString()}
                      </div>
                      <button onClick={() => removeFromCart(item.product.id)} style={{ border: 'none', background: 'none', color: 'var(--b360-text-secondary)', cursor: 'pointer' }}>
                        <Trash2 size={14} />
                      </button>
                    </div>
                  ))
                )}
              </div>

              <div style={{ borderTop: '1px solid var(--b360-border)', paddingTop: 12, display: 'flex', flexDirection: 'column', gap: 10 }}>
                <Select
                  label="Select Customer"
                  value={selectedCustomerId}
                  onChange={handleSelectCustomer}
                  options={[
                    { value: '', label: 'Walk-In / Anonymous Customer' },
                    ...customers.map(c => ({ value: c.id, label: `${c.name} (${c.phone})` }))
                  ]}
                />
                
                {selectedCustomerId === '' && (
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
                    <Input label="Name" value={customerName} onChange={setCustomerName} />
                    <Input label="Phone" value={customerPhone} onChange={setCustomerPhone} />
                  </div>
                )}

                {/* Payment Method Selector */}
                <div>
                  <label style={{ fontSize: 12, fontWeight: 600, color: 'var(--b360-text-secondary)', display: 'block', marginBottom: 6 }}>
                    Payment Method
                  </label>
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 8 }}>
                    {(['CASH', 'MPESA', 'CARD'] as const).map(method => (
                      <button
                        key={method}
                        type="button"
                        onClick={() => setPaymentMethod(method)}
                        style={{
                          padding: '10px 0',
                          borderRadius: 8,
                          border: paymentMethod === method ? '2px solid var(--b360-green)' : '1px solid var(--b360-border)',
                          backgroundColor: paymentMethod === method ? 'var(--b360-surface)' : 'white',
                          color: 'var(--b360-sidebar-bg)',
                          fontWeight: 700,
                          fontSize: 12,
                          cursor: 'pointer',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          gap: 6
                        }}
                      >
                        {method === 'MPESA' && <Smartphone size={13} />}
                        {method === 'CARD' && <CreditCard size={13} />}
                        {method}
                      </button>
                    ))}
                  </div>
                  {paymentMethod === 'MPESA' && (
                    <p style={{ fontSize: 11, color: 'var(--b360-green)', marginTop: 6, fontWeight: 500 }}>
                      📱 An STK push will be sent to the customer's phone after checkout.
                    </p>
                  )}
                </div>

                <Input label="Sale Notes" placeholder="Optional notes..." value={notes} onChange={setNotes} />
              </div>

              <div style={{ borderTop: '1px solid var(--b360-border)', paddingTop: 12, display: 'flex', flexDirection: 'column', gap: 6 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13, color: 'var(--b360-text-secondary)' }}>
                  <span>Subtotal</span>
                  <span>KES {subtotal.toLocaleString()}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13, color: 'var(--b360-text-secondary)' }}>
                  <span>VAT (16%)</span>
                  <span>KES {tax.toLocaleString()}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 16, fontWeight: 800, color: 'var(--b360-sidebar-bg)', borderTop: '1px dashed var(--b360-border)', paddingTop: 8 }}>
                  <span>Grand Total</span>
                  <span style={{ color: 'var(--b360-green)' }}>KES {total.toLocaleString()}</span>
                </div>
              </div>

              <button
                type="button"
                onClick={handleCheckout}
                disabled={cart.length === 0 || isCheckingOut}
                style={{
                  width: '100%',
                  height: 48,
                  fontSize: 15,
                  fontWeight: 700,
                  backgroundColor: paymentMethod === 'MPESA' ? '#16A34A' : 'var(--b360-green)',
                  color: 'white',
                  borderRadius: 8,
                  border: 'none',
                  cursor: (cart.length === 0 || isCheckingOut) ? 'not-allowed' : 'pointer',
                  marginTop: 8,
                  opacity: (cart.length === 0 || isCheckingOut) ? 0.6 : 1,
                  transition: 'opacity 0.2s'
                }}
              >
                {isCheckingOut
                  ? 'Processing...'
                  : paymentMethod === 'MPESA'
                    ? '📱 Checkout & Send M-Pesa Prompt'
                    : 'Complete Checkout Sale'}
              </button>
            </Card>
          </div>

        </div>
      )}
    </div>
  )
}

export default PosPage
