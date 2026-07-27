import React, { useState, useEffect } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Shield, CheckCircle, CreditCard, Lock, AlertTriangle } from 'lucide-react'
import { cyberSourceApi, client, ApiResponse, OrderResponse } from '../services/api'

export default function CardCheckoutPage() {
  const [searchParams] = useSearchParams()
  const orderId = searchParams.get('orderId') || ''
  const businessId = searchParams.get('businessId') || ''

  const [order, setOrder] = useState<OrderResponse | null>(null)
  const [loadingOrder, setLoadingOrder] = useState(true)
  const [orderError, setOrderError] = useState('')

  // Form states
  const [cardNumber, setCardNumber] = useState('')
  const [expiryMonth, setExpiryMonth] = useState('12')
  const [expiryYear, setExpiryYear] = useState('2028')
  const [cardCvv, setCardCvv] = useState('')
  const [cardholderName, setCardholderName] = useState('')
  const [billingEmail, setBillingEmail] = useState('')
  const [billingPhone, setBillingPhone] = useState('')

  const [isProcessing, setIsProcessing] = useState(false)
  const [paymentError, setPaymentError] = useState('')
  const [paymentSuccess, setPaymentSuccess] = useState(false)
  const [txnId, setTxnId] = useState('')

  useEffect(() => {
    if (!orderId || !businessId) {
      setOrderError('Invalid or missing order parameters in payment link.')
      setLoadingOrder(false)
      return
    }

    setLoadingOrder(true)
    client.get<ApiResponse<OrderResponse>>('/payments/card/public-order', {
      params: { orderId, businessId }
    }).then(res => {
      if (res.data.success && res.data.data) {
        setOrder(res.data.data)
        if (res.data.data.customerName) setCardholderName(res.data.data.customerName)
        if (res.data.data.customerPhone) setBillingPhone(res.data.data.customerPhone)
        if (res.data.data.paymentStatus === 'PAID') {
          setPaymentSuccess(true)
        }
      } else {
        setOrderError(res.data.message || 'Order not found or link has expired.')
      }
    }).catch(err => {
      setOrderError(err.response?.data?.message || 'Failed to load order details.')
    }).finally(() => setLoadingOrder(false))
  }, [orderId, businessId])

  const handlePay = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!order) return
    if (!cardNumber || cardNumber.length < 13) { setPaymentError('Please enter a valid card number'); return }
    if (!cardCvv || cardCvv.length < 3) { setPaymentError('Please enter a valid 3 or 4 digit CVV'); return }
    if (!cardholderName.trim()) { setPaymentError('Please enter the name on the card'); return }

    setIsProcessing(true)
    setPaymentError('')
    try {
      const res = await cyberSourceApi.guestCharge({
        businessId,
        orderId: order.id,
        amount: order.subtotal,
        currency: 'KES',
        cardNumber: cardNumber.replace(/\s+/g, ''),
        cardExpiryMonth: expiryMonth,
        cardExpiryYear: expiryYear,
        cardCvv,
        cardholderName,
        billingEmail,
        billingPhone
      })

      if (res.success && res.data && (res.data.status === 'CAPTURED' || res.data.status === 'AUTHORIZED')) {
        setPaymentSuccess(true)
        setTxnId(res.data.transactionId || res.data.csTransactionId || '')
      } else {
        setPaymentError(res.message || res.data?.errorMessage || 'Card payment declined. Please try another card.')
      }
    } catch (err: any) {
      setPaymentError(err.response?.data?.message || 'Payment processing error. Please check card details and try again.')
    } finally {
      setIsProcessing(false)
    }
  }

  return (
    <div style={{
      minHeight: '100vh',
      backgroundColor: '#F8FAFC',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '24px 16px',
      fontFamily: 'Inter, system-ui, sans-serif'
    }}>
      {/* Brand Header */}
      <div style={{ textAlign: 'center', marginBottom: 24 }}>
        <div style={{ display: 'inline-flex', alignItems: 'center', gap: 10, marginBottom: 8 }}>
          <div style={{ background: '#16A34A', color: 'white', padding: 8, borderRadius: 10, display: 'flex' }}>
            <Shield size={24} />
          </div>
          <span style={{ fontSize: 24, fontWeight: 800, color: '#0F172A', letterSpacing: '-0.5px' }}>
            Biashara<span style={{ color: '#16A34A' }}>360</span> Pay
          </span>
        </div>
        <p style={{ fontSize: 13, color: '#64748B', margin: 0 }}>Secure Card Checkout Rail · Powered by CyberSource</p>
      </div>

      <div style={{ width: '100%', maxWidth: 460 }}>
        {loadingOrder ? (
          <div style={{ background: 'white', padding: 40, borderRadius: 16, textAlign: 'center', boxShadow: '0 4px 20px rgba(0,0,0,0.06)' }}>
            <div style={{ fontSize: 32, marginBottom: 12 }}>⌛</div>
            <p style={{ color: '#64748B', fontSize: 14 }}>Loading payment invoice...</p>
          </div>
        ) : orderError ? (
          <div style={{ background: 'white', padding: 32, borderRadius: 16, textAlign: 'center', boxShadow: '0 4px 20px rgba(0,0,0,0.06)' }}>
            <AlertTriangle size={48} color="#EF4444" style={{ marginBottom: 16 }} />
            <h3 style={{ fontSize: 18, fontWeight: 700, color: '#0F172A', marginBottom: 8 }}>Unable to Load Invoice</h3>
            <p style={{ color: '#EF4444', fontSize: 14, marginBottom: 16 }}>{orderError}</p>
            <p style={{ fontSize: 12, color: '#94A3B8' }}>Please request a new card payment link from the merchant.</p>
          </div>
        ) : paymentSuccess ? (
          <div style={{ background: 'white', padding: 36, borderRadius: 16, textAlign: 'center', boxShadow: '0 4px 20px rgba(0,0,0,0.06)' }}>
            <div style={{ width: 64, height: 64, borderRadius: '50%', background: '#DCFCE7', color: '#16A34A', display: 'inline-flex', alignItems: 'center', justifyContent: 'center', marginBottom: 16 }}>
              <CheckCircle size={36} />
            </div>
            <h2 style={{ fontSize: 22, fontWeight: 700, color: '#0F172A', marginBottom: 6 }}>Payment Successful!</h2>
            <p style={{ color: '#475569', fontSize: 14, marginBottom: 20 }}>
              Your card payment of <strong>KES {order?.subtotal.toLocaleString()}</strong> has been verified.
            </p>

            <div style={{ background: '#F8FAFC', borderRadius: 12, padding: 16, textAlign: 'left', marginBottom: 24, fontSize: 13, border: '1px solid #E2E8F0' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                <span style={{ color: '#64748B' }}>Order Number:</span>
                <span style={{ fontWeight: 700, fontFamily: 'monospace' }}>{order?.orderNumber}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                <span style={{ color: '#64748B' }}>Customer:</span>
                <span style={{ fontWeight: 600 }}>{order?.customerName}</span>
              </div>
              {txnId && (
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: '#64748B' }}>Transaction Ref:</span>
                  <span style={{ fontWeight: 600, fontFamily: 'monospace', color: '#16A34A' }}>{txnId}</span>
                </div>
              )}
            </div>

            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6, color: '#16A34A', fontSize: 13, fontWeight: 600 }}>
              <Lock size={14} /> Receipt emailed & order status updated to PAID.
            </div>
          </div>
        ) : (
          <div style={{ background: 'white', borderRadius: 16, padding: 28, boxShadow: '0 4px 20px rgba(0,0,0,0.06)', border: '1px solid #E2E8F0' }}>
            {/* Order Summary Box */}
            <div style={{ background: '#F1F5F9', borderRadius: 12, padding: 16, marginBottom: 24 }}>
              <div style={{ fontSize: 12, textTransform: 'uppercase', color: '#64748B', fontWeight: 700, letterSpacing: '0.5px', marginBottom: 4 }}>
                Payment Invoice
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
                <span style={{ fontWeight: 700, fontSize: 18, color: '#0F172A' }}>Order #{order?.orderNumber}</span>
                <span style={{ fontWeight: 800, fontSize: 22, color: '#16A34A' }}>
                  KES {order?.subtotal.toLocaleString()}
                </span>
              </div>
              <div style={{ fontSize: 13, color: '#475569', marginTop: 4 }}>
                Customer: <strong>{order?.customerName}</strong> ({order?.customerPhone})
              </div>
            </div>

            <form onSubmit={handlePay} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              <h3 style={{ fontSize: 15, fontWeight: 700, color: '#0F172A', margin: 0, display: 'flex', alignItems: 'center', gap: 8 }}>
                <CreditCard size={18} color="#16A34A" /> Card Details
              </h3>

              {paymentError && (
                <div style={{ background: '#FEF2F2', border: '1px solid #FCA5A5', color: '#991B1B', padding: 12, borderRadius: 8, fontSize: 13, fontWeight: 500 }}>
                  {paymentError}
                </div>
              )}

              <div>
                <label style={{ display: 'block', fontSize: 12, fontWeight: 600, color: '#475569', marginBottom: 4 }}>
                  Cardholder Name *
                </label>
                <input
                  type="text"
                  required
                  value={cardholderName}
                  onChange={e => setCardholderName(e.target.value)}
                  placeholder="e.g. Jane Wanjiru"
                  style={{ width: '100%', padding: '11px 14px', borderRadius: 8, border: '1px solid #CBD5E1', fontSize: 14, outline: 'none' }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: 12, fontWeight: 600, color: '#475569', marginBottom: 4 }}>
                  Card Number *
                </label>
                <input
                  type="text"
                  required
                  maxLength={19}
                  value={cardNumber}
                  onChange={e => setCardNumber(e.target.value)}
                  placeholder="4532 •••• •••• ••••"
                  style={{ width: '100%', padding: '11px 14px', borderRadius: 8, border: '1px solid #CBD5E1', fontSize: 14, fontFamily: 'monospace', outline: 'none' }}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 10 }}>
                <div>
                  <label style={{ display: 'block', fontSize: 12, fontWeight: 600, color: '#475569', marginBottom: 4 }}>Month</label>
                  <select
                    value={expiryMonth}
                    onChange={e => setExpiryMonth(e.target.value)}
                    style={{ width: '100%', padding: '11px 8px', borderRadius: 8, border: '1px solid #CBD5E1', fontSize: 14, background: 'white' }}
                  >
                    {Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0')).map(m => (
                      <option key={m} value={m}>{m}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: 12, fontWeight: 600, color: '#475569', marginBottom: 4 }}>Year</label>
                  <select
                    value={expiryYear}
                    onChange={e => setExpiryYear(e.target.value)}
                    style={{ width: '100%', padding: '11px 8px', borderRadius: 8, border: '1px solid #CBD5E1', fontSize: 14, background: 'white' }}
                  >
                    {['2025','2026','2027','2028','2029','2030','2031','2032'].map(y => (
                      <option key={y} value={y}>{y}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: 12, fontWeight: 600, color: '#475569', marginBottom: 4 }}>CVV *</label>
                  <input
                    type="password"
                    required
                    maxLength={4}
                    value={cardCvv}
                    onChange={e => setCardCvv(e.target.value)}
                    placeholder="123"
                    style={{ width: '100%', padding: '11px 8px', borderRadius: 8, border: '1px solid #CBD5E1', fontSize: 14, fontFamily: 'monospace', outline: 'none', textAlign: 'center' }}
                  />
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: 12, fontWeight: 600, color: '#475569', marginBottom: 4 }}>
                  Billing Email (Optional for digital receipt)
                </label>
                <input
                  type="email"
                  value={billingEmail}
                  onChange={e => setBillingEmail(e.target.value)}
                  placeholder="jane@example.com"
                  style={{ width: '100%', padding: '11px 14px', borderRadius: 8, border: '1px solid #CBD5E1', fontSize: 14, outline: 'none' }}
                />
              </div>

              <button
                type="submit"
                disabled={isProcessing}
                style={{
                  width: '100%',
                  padding: '14px 0',
                  background: '#16A34A',
                  color: 'white',
                  border: 'none',
                  borderRadius: 10,
                  fontWeight: 700,
                  fontSize: 15,
                  cursor: isProcessing ? 'not-allowed' : 'pointer',
                  opacity: isProcessing ? 0.7 : 1,
                  marginTop: 8,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: 8,
                  boxShadow: '0 4px 12px rgba(22, 163, 74, 0.25)'
                }}
              >
                {isProcessing ? 'Authorizing Payment...' : `💳 Pay KES ${order?.subtotal.toLocaleString()} Now`}
              </button>

              <div style={{ textAlign: 'center', fontSize: 12, color: '#64748B', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 4, marginTop: 4 }}>
                <Lock size={12} /> 256-Bit TLS Encrypted · CyberSource Gateway
              </div>
            </form>
          </div>
        )}
      </div>
    </div>
  )
}
