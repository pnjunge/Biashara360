import React, { useState, useEffect } from 'react'
import QRCode from 'qrcode'
import {
  Smartphone,
  CheckCircle2,
  Receipt,
  QrCode,
  Sparkles,
  ShoppingBag,
  Plus,
  Minus,
  Trash2,
  RefreshCw,
  UtensilsCrossed,
  ShieldCheck,
  Printer,
  ChevronRight,
  ArrowRight
} from 'lucide-react'

interface ProductItem {
  id: string
  name: string
  category: string
  price: number
  emoji: string
}

const SAMPLE_PRODUCTS: ProductItem[] = [
  { id: '1', name: 'Unga Jogoo Maize 2kg', category: 'Pantry', price: 195, emoji: '🌽' },
  { id: '2', name: 'Fresh Highlands Milk 500ml', category: 'Dairy', price: 65, emoji: '🥛' },
  { id: '3', name: 'Supa Loaf Bread 800g', category: 'Bakery', price: 110, emoji: '🍞' },
  { id: '4', name: 'Kericho Gold Pure Tea 100s', category: 'Beverage', price: 240, emoji: '☕' },
  { id: '5', name: 'Tusker Lager Cold 500ml', category: 'Bar', price: 260, emoji: '🍺' },
  { id: '6', name: 'Ariel Washing Powder 500g', category: 'Household', price: 175, emoji: '🧼' },
]

export default function PosSimulator() {
  const [activeTab, setActiveTab] = useState<'POS' | 'KRA' | 'HOSPITALITY'>('POS')

  // POS State
  const [cart, setCart] = useState<{ item: ProductItem; qty: number }[]>([
    { item: SAMPLE_PRODUCTS[0], qty: 1 },
    { item: SAMPLE_PRODUCTS[1], qty: 2 },
  ])
  const [phone, setPhone] = useState('0712 345 678')
  const [stkStatus, setStkStatus] = useState<'IDLE' | 'PROMPTING' | 'PAID'>('IDLE')
  const [transCode, setTransCode] = useState('SFK89X12Q')
  const [receiptQr, setReceiptQr] = useState('')

  // KRA State
  const [kraAmount, setKraAmount] = useState<number>(3450)
  const [kraPin, setKraPin] = useState('P051982736Z')
  const [kraSigned, setKraSigned] = useState(false)
  const [kraQr, setKraQr] = useState('')

  // Hospitality State
  const [tableNumber, setTableNumber] = useState(4)
  const [guestOrder, setGuestOrder] = useState<string[]>([
    '1x 1/2 Kg Mbuzi Wet Fry (KES 650)',
    '1x Ugali Greens Special (KES 100)',
    '2x White Cap Crisp Cold (KES 520)'
  ])
  const [orderSent, setOrderSent] = useState(false)
  const [kdsStatus, setKdsStatus] = useState<'QUEUED' | 'COOKING' | 'SERVED'>('COOKING')

  // Calculate POS totals
  const subtotal = cart.reduce((sum, entry) => sum + entry.item.price * entry.qty, 0)
  const vat = Math.round(subtotal * (16 / 116))
  const net = subtotal - vat

  // Generate QR code for receipt
  useEffect(() => {
    const kraReceiptData = `https://itax.kra.go.ke/KRA-Portal/invoiceVerification.htm?invoice=CU-${transCode}-2026`
    QRCode.toDataURL(kraReceiptData, { width: 140, margin: 1, errorCorrectionLevel: 'M' })
      .then(url => setReceiptQr(url))
      .catch(() => setReceiptQr(''))
  }, [transCode])

  // Generate QR code for KRA demo
  useEffect(() => {
    const kraVerifyData = `https://itax.kra.go.ke/verification?pin=${kraPin}&inv=ETIMS-${kraAmount}&date=20261003`
    QRCode.toDataURL(kraVerifyData, { width: 150, margin: 1, errorCorrectionLevel: 'M' })
      .then(url => setKraQr(url))
      .catch(() => setKraQr(''))
  }, [kraAmount, kraPin])

  const addToCart = (product: ProductItem) => {
    setCart(prev => {
      const existing = prev.find(p => p.item.id === product.id)
      if (existing) {
        return prev.map(p => p.item.id === product.id ? { ...p, qty: p.qty + 1 } : p)
      }
      return [...prev, { item: product, qty: 1 }]
    })
    setStkStatus('IDLE')
  }

  const updateQty = (id: string, delta: number) => {
    setCart(prev => prev.map(p => {
      if (p.item.id === id) {
        const nextQty = p.qty + delta
        return nextQty > 0 ? { ...p, qty: nextQty } : null
      }
      return p
    }).filter(Boolean) as { item: ProductItem; qty: number }[])
    setStkStatus('IDLE')
  }

  const handleTriggerStk = () => {
    setStkStatus('PROMPTING')
  }

  const handleConfirmPin = () => {
    // Generate fresh transaction code
    const randomCode = 'SFK' + Math.floor(100000 + Math.random() * 900000)
    setTransCode(randomCode)
    setStkStatus('PAID')
  }

  const handleResetPos = () => {
    setCart([
      { item: SAMPLE_PRODUCTS[0], qty: 1 },
      { item: SAMPLE_PRODUCTS[1], qty: 2 },
    ])
    setStkStatus('IDLE')
  }

  return (
    <section className="m-simulator-section" id="simulator">
      <div className="m-container">
        {/* Section Header */}
        <div style={{ textAlign: 'center', maxWidth: 780, margin: '0 auto 40px' }}>
          <div className="m-badge m-badge-dark" style={{ marginBottom: 14 }}>
            <Sparkles size={14} color="#10B981" />
            <span>Interactive Live Sandbox</span>
          </div>
          <h2 className="m-heading" style={{ fontSize: 'clamp(28px, 4vw, 44px)', color: '#ffffff', marginBottom: 16 }}>
            Experience Biashara360 in Action
          </h2>
          <p style={{ color: '#94a3b8', fontSize: '17px' }}>
            Test out real checkout, simulate an authentic Safaricom M-Pesa STK push, inspect instant KRA eTIMS invoice signatures, or view table ordering.
          </p>
        </div>

        {/* Tab Controls */}
        <div className="m-sim-tabs">
          <button
            type="button"
            className={`m-sim-tab-btn ${activeTab === 'POS' ? 'active' : ''}`}
            onClick={() => setActiveTab('POS')}
          >
            <ShoppingBag size={16} />
            <span>1. POS & M-Pesa STK Checkout</span>
          </button>
          <button
            type="button"
            className={`m-sim-tab-btn ${activeTab === 'KRA' ? 'active' : ''}`}
            onClick={() => setActiveTab('KRA')}
          >
            <ShieldCheck size={16} />
            <span>2. KRA eTIMS Fiscalizer</span>
          </button>
          <button
            type="button"
            className={`m-sim-tab-btn ${activeTab === 'HOSPITALITY' ? 'active' : ''}`}
            onClick={() => setActiveTab('HOSPITALITY')}
          >
            <UtensilsCrossed size={16} />
            <span>3. Restaurant Table QR & KDS</span>
          </button>
        </div>

        {/* ── TAB 1: POS & M-PESA CHECKOUT ── */}
        {activeTab === 'POS' && (
          <div className="m-sim-window">
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: 32, alignItems: 'start' }}>
              {/* Product Shelf & Cart */}
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
                  <h3 style={{ fontSize: 18, fontWeight: 700, color: '#f8fafc' }}>
                    1. Tap items to add to cart
                  </h3>
                  <button
                    type="button"
                    onClick={handleResetPos}
                    style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12, color: '#94a3b8', background: 'transparent', cursor: 'pointer' }}
                  >
                    <RefreshCw size={13} />
                    <span>Reset</span>
                  </button>
                </div>

                <div className="m-sim-items-grid">
                  {SAMPLE_PRODUCTS.map(product => (
                    <button
                      key={product.id}
                      type="button"
                      onClick={() => addToCart(product)}
                      className="m-sim-item-card"
                    >
                      <div style={{ fontSize: 24, marginBottom: 8 }}>{product.emoji}</div>
                      <div style={{ fontWeight: 700, fontSize: 13, color: '#f1f5f9', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                        {product.name}
                      </div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 6 }}>
                        <span style={{ color: '#10b981', fontWeight: 800, fontSize: 13 }}>
                          KES {product.price}
                        </span>
                        <span style={{ fontSize: 11, background: 'rgba(255,255,255,0.1)', padding: '2px 6px', borderRadius: 4, color: '#cbd5e1' }}>
                          + Add
                        </span>
                      </div>
                    </button>
                  ))}
                </div>

                {/* Cart summary */}
                <div style={{ marginTop: 24, background: 'rgba(255,255,255,0.03)', borderRadius: 14, padding: 18, border: '1px solid rgba(255,255,255,0.08)' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 12, borderBottom: '1px solid rgba(255,255,255,0.08)', paddingBottom: 8 }}>
                    <span style={{ fontWeight: 700, color: '#e2e8f0', fontSize: 14 }}>Active Cart ({cart.length} items)</span>
                    <span style={{ color: '#94a3b8', fontSize: 12 }}>Inclusive 16% VAT</span>
                  </div>

                  {cart.length === 0 ? (
                    <p style={{ color: '#94a3b8', fontSize: 13, textAlign: 'center', padding: '14px 0' }}>
                      Cart is empty. Tap any product above to start checkout.
                    </p>
                  ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                      {cart.map(p => (
                        <div key={p.item.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <span style={{ color: '#f1f5f9', fontSize: 13, flex: 1 }}>{p.item.name}</span>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                            <button
                              type="button"
                              onClick={() => updateQty(p.item.id, -1)}
                              style={{ width: 22, height: 22, borderRadius: 4, background: 'rgba(255,255,255,0.1)', color: '#ffffff', display: 'grid', placeItems: 'center' }}
                            >
                              <Minus size={11} />
                            </button>
                            <span style={{ fontWeight: 700, fontSize: 13, minWidth: 16, textAlign: 'center' }}>{p.qty}</span>
                            <button
                              type="button"
                              onClick={() => updateQty(p.item.id, 1)}
                              style={{ width: 22, height: 22, borderRadius: 4, background: 'rgba(255,255,255,0.1)', color: '#ffffff', display: 'grid', placeItems: 'center' }}
                            >
                              <Plus size={11} />
                            </button>
                            <span style={{ color: '#10b981', fontWeight: 700, fontSize: 13, minWidth: 70, textAlign: 'right' }}>
                              KES {p.item.price * p.qty}
                            </span>
                          </div>
                        </div>
                      ))}

                      <div style={{ borderTop: '1px solid rgba(255,255,255,0.1)', paddingTop: 10, marginTop: 8 }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', color: '#94a3b8', fontSize: 12 }}>
                          <span>Net Sales:</span>
                          <span>KES {net.toLocaleString()}</span>
                        </div>
                        <div style={{ display: 'flex', justifyContent: 'space-between', color: '#94a3b8', fontSize: 12, marginTop: 2 }}>
                          <span>VAT (16% Standard Rate):</span>
                          <span>KES {vat.toLocaleString()}</span>
                        </div>
                        <div style={{ display: 'flex', justifyContent: 'space-between', color: '#ffffff', fontWeight: 800, fontSize: 18, marginTop: 6 }}>
                          <span>Total Payable:</span>
                          <span style={{ color: '#10b981' }}>KES {subtotal.toLocaleString()}</span>
                        </div>
                      </div>
                    </div>
                  )}

                  {/* Payment Trigger Form */}
                  <div style={{ marginTop: 18, paddingTop: 14, borderTop: '1px solid rgba(255,255,255,0.08)' }}>
                    <label style={{ display: 'block', fontSize: 12, color: '#cbd5e1', marginBottom: 6, fontWeight: 600 }}>
                      Customer Safaricom M-Pesa Number:
                    </label>
                    <div style={{ display: 'flex', gap: 10 }}>
                      <input
                        type="text"
                        value={phone}
                        onChange={e => setPhone(e.target.value)}
                        style={{
                          flex: 1,
                          padding: '10px 14px',
                          borderRadius: 8,
                          background: 'rgba(0,0,0,0.4)',
                          border: '1px solid rgba(255,255,255,0.2)',
                          color: '#ffffff',
                          fontSize: 14,
                          fontFamily: 'monospace'
                        }}
                      />
                      <button
                        type="button"
                        onClick={handleTriggerStk}
                        disabled={cart.length === 0 || stkStatus === 'PROMPTING'}
                        className="m-btn m-btn-primary"
                        style={{ padding: '10px 18px', fontSize: 14, whiteSpace: 'nowrap' }}
                      >
                        <Smartphone size={16} />
                        <span>Send STK Push</span>
                      </button>
                    </div>
                  </div>
                </div>
              </div>

              {/* Simulated Customer Smartphone + Real-Time M-Pesa USSD */}
              <div>
                <h3 style={{ fontSize: 18, fontWeight: 700, color: '#f8fafc', marginBottom: 16, textAlign: 'center' }}>
                  2. Watch Phone Prompt & Instant KRA Receipt
                </h3>

                <div style={{ display: 'flex', gap: 20, justifyContent: 'center', flexWrap: 'wrap' }}>
                  {/* Smartphone Mockup */}
                  <div className="m-phone-mockup">
                    <div className="m-phone-screen">
                      {/* Phone top bar */}
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11, color: '#94a3b8', marginBottom: 20 }}>
                        <span>Safaricom 5G</span>
                        <span>100% 🔋</span>
                      </div>

                      {stkStatus === 'IDLE' && (
                        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', flex: 1, textAlign: 'center', padding: 12 }}>
                          <Smartphone size={44} color="#64748b" style={{ marginBottom: 12 }} />
                          <h4 style={{ fontSize: 15, fontWeight: 700, color: '#e2e8f0', marginBottom: 6 }}>Ready for Payment</h4>
                          <p style={{ fontSize: 12, color: '#94a3b8' }}>
                            Click &ldquo;Send STK Push&rdquo; on the left to simulate a live Lipa na M-Pesa prompt.
                          </p>
                        </div>
                      )}

                      {stkStatus === 'PROMPTING' && (
                        <div style={{
                          background: '#ffffff',
                          color: '#0f172a',
                          borderRadius: 14,
                          padding: 18,
                          boxShadow: '0 10px 25px rgba(0,0,0,0.5)',
                          textAlign: 'left',
                          animation: 'modalScale 0.2s ease',
                          marginTop: 'auto',
                          marginBottom: 'auto'
                        }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 10 }}>
                            <span style={{ width: 10, height: 10, borderRadius: '50%', background: '#16a34a' }}></span>
                            <span style={{ fontSize: 12, fontWeight: 800, color: '#16a34a', textTransform: 'uppercase' }}>
                              Lipa na M-Pesa
                            </span>
                          </div>
                          <p style={{ fontSize: 13, lineHeight: 1.4, marginBottom: 12, color: '#1e293b' }}>
                            Do you want to pay <strong>KES {subtotal.toLocaleString()}</strong> to <strong>BIASHARA360 TILL 882194</strong>?
                          </p>
                          <label style={{ display: 'block', fontSize: 11, color: '#64748b', marginBottom: 4 }}>
                            Enter M-Pesa PIN:
                          </label>
                          <input
                            type="password"
                            defaultValue="••••"
                            readOnly
                            style={{
                              width: '100%',
                              padding: '8px 12px',
                              borderRadius: 6,
                              border: '1.5px solid #059669',
                              fontSize: 16,
                              letterSpacing: 4,
                              marginBottom: 12,
                              textAlign: 'center',
                              background: '#f8fafc'
                            }}
                          />
                          <button
                            type="button"
                            onClick={handleConfirmPin}
                            className="m-btn m-btn-primary"
                            style={{ width: '100%', padding: '10px 0', fontSize: 14, borderRadius: 8 }}
                          >
                            <CheckCircle2 size={16} />
                            <span>Authorize Payment</span>
                          </button>
                        </div>
                      )}

                      {stkStatus === 'PAID' && (
                        <div style={{
                          background: 'rgba(5, 150, 105, 0.15)',
                          border: '1px solid rgba(16, 185, 129, 0.4)',
                          borderRadius: 14,
                          padding: 16,
                          textAlign: 'center',
                          marginTop: 'auto',
                          marginBottom: 'auto'
                        }}>
                          <CheckCircle2 size={40} color="#10b981" style={{ margin: '0 auto 8px' }} />
                          <h4 style={{ fontSize: 15, fontWeight: 800, color: '#34d399', marginBottom: 4 }}>
                            {transCode} Confirmed
                          </h4>
                          <p style={{ fontSize: 11, color: '#cbd5e1', lineHeight: 1.4 }}>
                            KES {subtotal.toLocaleString()} sent to BIASHARA360 on {new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}.
                          </p>
                          <div style={{ marginTop: 12, fontSize: 10, color: '#a7f3d0', background: 'rgba(0,0,0,0.3)', padding: '6px 8px', borderRadius: 6 }}>
                            ✓ Zero manual matching needed<br />
                            ✓ Shift cash drawer balanced
                          </div>
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Thermal Receipt Mockup */}
                  {stkStatus === 'PAID' && (
                    <div className="m-receipt-paper" style={{ maxWidth: 240, alignSelf: 'center' }}>
                      <div style={{ textAlign: 'center', borderBottom: '1px dashed #64748b', paddingBottom: 8, marginBottom: 8 }}>
                        <div style={{ fontWeight: 800, fontSize: 14 }}>BIASHARA360 STORE</div>
                        <div style={{ fontSize: 10, color: '#475569' }}>KIMATHI ST, NAIROBI</div>
                        <div style={{ fontSize: 10, color: '#475569' }}>KRA PIN: P051293847X</div>
                        <div style={{ fontSize: 10, color: '#047857', fontWeight: 700, marginTop: 2 }}>
                          KRA eTIMS COMPLIANT
                        </div>
                      </div>

                      <div style={{ fontSize: 10, marginBottom: 6 }}>
                        <div>Date: {new Date().toLocaleDateString()} {new Date().toLocaleTimeString()}</div>
                        <div>Receipt: #INV-9824</div>
                        <div>Cashier: James (Till 1)</div>
                      </div>

                      <div style={{ borderTop: '1px dashed #64748b', borderBottom: '1px dashed #64748b', padding: '6px 0', margin: '6px 0' }}>
                        {cart.map(c => (
                          <div key={c.item.id} style={{ display: 'flex', justifyContent: 'space-between', fontSize: 10, marginBottom: 3 }}>
                            <span>{c.qty}x {c.item.name.substring(0, 14)}..</span>
                            <span>{c.item.price * c.qty}</span>
                          </div>
                        ))}
                      </div>

                      <div style={{ fontSize: 11, fontWeight: 700, display: 'flex', justifyContent: 'space-between', marginTop: 4 }}>
                        <span>TOTAL KES:</span>
                        <span>{subtotal.toLocaleString()}</span>
                      </div>
                      <div style={{ fontSize: 9, color: '#475569', display: 'flex', justifyContent: 'space-between' }}>
                        <span>M-Pesa Ref:</span>
                        <span>{transCode}</span>
                      </div>

                      <div style={{ textAlign: 'center', marginTop: 10, paddingTop: 6, borderTop: '1px dashed #64748b' }}>
                        {receiptQr ? (
                          <img src={receiptQr} alt="KRA Tax Receipt QR" style={{ width: 90, height: 90, margin: '0 auto', display: 'block' }} />
                        ) : (
                          <div style={{ width: 90, height: 90, background: '#eee', margin: '0 auto' }} />
                        )}
                        <div style={{ fontSize: 9, color: '#475569', marginTop: 4 }}>
                          CU: CU-202610-88492
                        </div>
                        <div style={{ fontSize: 8, color: '#64748b' }}>
                          Scan to verify on KRA Portal
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* ── TAB 2: KRA eTIMS FISCALIZER ── */}
        {activeTab === 'KRA' && (
          <div className="m-sim-window">
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: 32, alignItems: 'center' }}>
              <div>
                <div className="m-badge" style={{ background: '#dcfce7', color: '#15803d', border: '1px solid #86efac', marginBottom: 12 }}>
                  <ShieldCheck size={14} />
                  <span>KRA Electronic Tax Invoice Compliance</span>
                </div>
                <h3 style={{ fontSize: 24, fontWeight: 800, color: '#ffffff', marginBottom: 12 }}>
                  Never Buy a KES 50,000 ESD Device Again
                </h3>
                <p style={{ color: '#94a3b8', fontSize: 15, lineHeight: 1.6, marginBottom: 20 }}>
                  Biashara360 communicates directly with KRA servers via secure API. Every sale is signed with a valid cryptographic Control Unit Number and verification QR code in under 300ms.
                </p>

                <div style={{ background: 'rgba(255,255,255,0.04)', padding: 18, borderRadius: 12, border: '1px solid rgba(255,255,255,0.08)' }}>
                  <div style={{ marginBottom: 14 }}>
                    <label style={{ display: 'block', fontSize: 13, color: '#cbd5e1', marginBottom: 6 }}>
                      Transaction Sale Amount (KES):
                    </label>
                    <input
                      type="number"
                      value={kraAmount}
                      onChange={e => setKraAmount(Number(e.target.value) || 0)}
                      style={{
                        width: '100%',
                        padding: '10px 14px',
                        borderRadius: 8,
                        background: 'rgba(0,0,0,0.5)',
                        border: '1px solid rgba(255,255,255,0.2)',
                        color: '#ffffff',
                        fontSize: 16,
                        fontWeight: 700
                      }}
                    />
                  </div>

                  <div style={{ marginBottom: 14 }}>
                    <label style={{ display: 'block', fontSize: 13, color: '#cbd5e1', marginBottom: 6 }}>
                      Trader / Buyer KRA PIN:
                    </label>
                    <input
                      type="text"
                      value={kraPin}
                      onChange={e => setKraPin(e.target.value.toUpperCase())}
                      style={{
                        width: '100%',
                        padding: '10px 14px',
                        borderRadius: 8,
                        background: 'rgba(0,0,0,0.5)',
                        border: '1px solid rgba(255,255,255,0.2)',
                        color: '#ffffff',
                        fontSize: 14,
                        fontFamily: 'monospace'
                      }}
                    />
                  </div>

                  <div style={{ display: 'flex', gap: 10, marginTop: 16 }}>
                    <button
                      type="button"
                      onClick={() => setKraSigned(true)}
                      className="m-btn m-btn-primary"
                      style={{ flex: 1, padding: '11px', fontSize: 14 }}
                    >
                      <ShieldCheck size={16} />
                      <span>Fiscalize Invoice Now</span>
                    </button>
                  </div>
                </div>
              </div>

              {/* Fiscalization Output Visual */}
              <div style={{ background: 'rgba(0,0,0,0.5)', borderRadius: 16, padding: 24, border: '1px solid rgba(16, 185, 129, 0.3)' }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16, borderBottom: '1px solid rgba(255,255,255,0.1)', paddingBottom: 10 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span className="m-pulse-dot"></span>
                    <span style={{ fontSize: 13, fontWeight: 700, color: '#34d399' }}>KRA OSCU Live Bridge: Connected</span>
                  </div>
                  <span style={{ fontSize: 11, color: '#94a3b8' }}>Latency: 142ms</span>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 140px', gap: 16, alignItems: 'center' }}>
                  <div>
                    <div style={{ fontSize: 12, color: '#94a3b8' }}>KRA Control Number:</div>
                    <div style={{ fontSize: 14, fontWeight: 800, color: '#ffffff', fontFamily: 'monospace', margin: '3px 0 10px' }}>
                      KRA-B360-{kraPin.slice(0, 4)}-{kraAmount}
                    </div>

                    <div style={{ fontSize: 12, color: '#94a3b8' }}>Internal Fiscal Sign:</div>
                    <div style={{ fontSize: 11, color: '#38bdf8', fontFamily: 'monospace', wordBreak: 'break-all', margin: '3px 0 10px' }}>
                      SHA256: 7f8a91bc92e0324810f948...
                    </div>

                    <div style={{ fontSize: 12, color: '#94a3b8' }}>16% VAT Extracted:</div>
                    <div style={{ fontSize: 15, fontWeight: 800, color: '#10b981' }}>
                      KES {Math.round(kraAmount * (16 / 116)).toLocaleString()}
                    </div>
                  </div>

                  <div style={{ textAlign: 'center', background: '#ffffff', padding: 8, borderRadius: 10 }}>
                    {kraQr && <img src={kraQr} alt="eTIMS QR" style={{ width: 120, height: 120, display: 'block', margin: '0 auto' }} />}
                    <div style={{ fontSize: 9, color: '#0f172a', fontWeight: 700, marginTop: 4 }}>eTIMS QR Code</div>
                  </div>
                </div>

                <div style={{ marginTop: 18, padding: '10px 14px', background: 'rgba(16, 185, 129, 0.1)', borderRadius: 8, fontSize: 12, color: '#a7f3d0', display: 'flex', alignItems: 'center', gap: 8 }}>
                  <CheckCircle2 size={16} />
                  <span>Compliant with Kenya Revenue Authority eTIMS Regulation 2024.</span>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* ── TAB 3: RESTAURANT TABLE QR & KDS ── */}
        {activeTab === 'HOSPITALITY' && (
          <div className="m-sim-window">
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: 32, alignItems: 'start' }}>
              {/* Customer QR View */}
              <div>
                <div className="m-badge" style={{ background: '#fef3c7', color: '#b45309', border: '1px solid #fde68a', marginBottom: 12 }}>
                  <UtensilsCrossed size={14} />
                  <span>Self-Service QR Ordering</span>
                </div>
                <h3 style={{ fontSize: 22, fontWeight: 800, color: '#ffffff', marginBottom: 10 }}>
                  Guest Scans Table QR Code
                </h3>
                <p style={{ color: '#94a3b8', fontSize: 14, marginBottom: 16 }}>
                  Guests scan the QR code on Table {tableNumber}. No app download needed. The order instantly notifies the kitchen and opens a tab.
                </p>

                <div style={{ background: 'rgba(255,255,255,0.04)', borderRadius: 14, padding: 18, border: '1px solid rgba(255,255,255,0.08)' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 12 }}>
                    <span style={{ fontWeight: 700, color: '#ffffff', fontSize: 14 }}>Table #{tableNumber} Active Order:</span>
                    <button
                      type="button"
                      onClick={() => setTableNumber(tableNumber === 4 ? 7 : 4)}
                      style={{ fontSize: 12, color: '#38bdf8', background: 'transparent' }}
                    >
                      Switch to Table {tableNumber === 4 ? 7 : 4}
                    </button>
                  </div>

                  <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginBottom: 16 }}>
                    {guestOrder.map((dish, i) => (
                      <div key={i} style={{ background: 'rgba(255,255,255,0.03)', padding: '8px 12px', borderRadius: 6, fontSize: 13, color: '#cbd5e1' }}>
                        {dish}
                      </div>
                    ))}
                  </div>

                  <button
                    type="button"
                    onClick={() => { setOrderSent(true); setKdsStatus('COOKING') }}
                    className="m-btn m-btn-primary"
                    style={{ width: '100%', padding: '11px', fontSize: 14 }}
                  >
                    <ArrowRight size={16} />
                    <span>Send Order Directly to Kitchen</span>
                  </button>
                </div>
              </div>

              {/* Kitchen Display Screen (KDS) View */}
              <div>
                <h3 style={{ fontSize: 18, fontWeight: 700, color: '#f8fafc', marginBottom: 14, textAlign: 'center' }}>
                  Kitchen Display Screen (Chef / Bar Station)
                </h3>

                <div style={{ background: '#020617', borderRadius: 16, border: '1.5px solid #334155', padding: 20 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16, borderBottom: '1px solid #1e293b', paddingBottom: 10 }}>
                    <span style={{ fontSize: 14, fontWeight: 800, color: '#f59e0b' }}>
                      TICKET #ECOM-T{tableNumber}
                    </span>
                    <span style={{ background: kdsStatus === 'COOKING' ? '#ef4444' : '#10b981', color: '#ffffff', fontSize: 11, padding: '3px 8px', borderRadius: 9999, fontWeight: 700 }}>
                      {kdsStatus === 'COOKING' ? '🔥 In Preparation (04:12)' : '✓ Ready to Serve'}
                    </span>
                  </div>

                  <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                    <div style={{ fontSize: 14, color: '#ffffff', fontWeight: 600 }}>
                      Table {tableNumber} (Lounge Area)
                    </div>
                    <ul style={{ paddingLeft: 18, color: '#cbd5e1', fontSize: 13, lineHeight: 1.6 }}>
                      <li>1x 1/2 Kg Mbuzi Wet Fry <span style={{ color: '#f59e0b', fontSize: 11 }}>(Well Done, Extra Kachumbari)</span></li>
                      <li>1x Ugali Greens Special</li>
                      <li>2x White Cap Crisp Cold <span style={{ color: '#38bdf8', fontSize: 11 }}>(Bar Station ticket routed)</span></li>
                    </ul>
                  </div>

                  <div style={{ display: 'flex', gap: 10, marginTop: 20, paddingTop: 14, borderTop: '1px solid #1e293b' }}>
                    <button
                      type="button"
                      onClick={() => setKdsStatus('SERVED')}
                      className="m-btn m-btn-primary"
                      style={{ flex: 1, padding: '9px 14px', fontSize: 13 }}
                    >
                      <CheckCircle2 size={15} />
                      <span>Mark Ready for Server</span>
                    </button>
                    <button
                      type="button"
                      onClick={() => { setOrderSent(false); setKdsStatus('COOKING') }}
                      className="m-btn m-btn-dark"
                      style={{ padding: '9px 14px', fontSize: 13 }}
                    >
                      <span>Clear</span>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </section>
  )
}
