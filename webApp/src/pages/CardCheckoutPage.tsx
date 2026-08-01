import React, { useState, useEffect } from 'react'
import { useSearchParams } from 'react-router-dom'
import {
  Shield, CheckCircle, XCircle, Lock, CreditCard,
  AlertTriangle, ExternalLink, Share2, Copy, CheckCheck
} from 'lucide-react'
import { client, ApiResponse, OrderResponse } from '../services/api'

// ─── Types ────────────────────────────────────────────────────────────────────

interface SaInitiateResponse {
  actionUrl: string
  fields: Record<string, string>
}

// ─── Main Component ───────────────────────────────────────────────────────────

export default function CardCheckoutPage() {
  const [searchParams] = useSearchParams()

  // URL params from both the payment link and the SA-return redirect
  const orderId    = searchParams.get('orderId')    || ''
  const businessId = searchParams.get('businessId') || ''
  const status     = searchParams.get('status')     || ''           // success | declined | cancelled
  const txnId      = searchParams.get('txnId')      || ''
  const amount     = searchParams.get('amount')     || ''
  const storeSlug  = searchParams.get('storeSlug')  || ''

  const [order, setOrder]             = useState<OrderResponse | null>(null)
  const [loadingOrder, setLoadingOrder] = useState(true)
  const [orderError, setOrderError]   = useState('')
  const [redirecting, setRedirecting] = useState(false)
  const [linkCopied, setLinkCopied]   = useState(false)

  // ── Load order details ───────────────────────────────────────────────────────
  useEffect(() => {
    if (status) {
      // SA-return — no need to load order for result screens
      setLoadingOrder(false)
      return
    }
    if (!orderId || !businessId) {
      setOrderError('Invalid or missing payment link parameters.')
      setLoadingOrder(false)
      return
    }
    client.get<ApiResponse<OrderResponse>>('/public/payments/card/public-order', {
      params: { orderId, businessId }
    }).then(res => {
      if (res.data.success && res.data.data) {
        let o = res.data.data
        // Fall back to URL amount if order has no subtotal (ad-hoc links)
        if ((!o.subtotal || o.subtotal === 0) && amount && !isNaN(Number(amount))) {
          o = { ...o, subtotal: Number(amount) }
        }
        if (o.paymentStatus === 'PAID') {
          // Already paid — show success without redirecting
          setOrder(o)
        } else {
          setOrder(o)
        }
      } else {
        setOrderError(res.data.message || 'Order not found or payment link has expired.')
      }
    }).catch(() => {
      setOrderError('Failed to load payment details. Please check your link and try again.')
    }).finally(() => setLoadingOrder(false))
  }, [orderId, businessId, status, amount])

  // ── Initiate Hosted Checkout ─────────────────────────────────────────────────
  const handlePayNow = async () => {
    if (!order) return
    setRedirecting(true)
    try {
      const res = await client.post<ApiResponse<SaInitiateResponse>>('/public/payments/card/sa-initiate', {
        businessId,
        orderId: order.id,
        amount: order.subtotal,
        customerName:  order.customerName  || searchParams.get('name')  || undefined,
        customerEmail: searchParams.get('email') || undefined,
        customerPhone: order.customerPhone || searchParams.get('phone') || undefined,
        returnStoreSlug: storeSlug || undefined
      })

      if (!res.data.success || !res.data.data) {
        alert(res.data.message || 'Could not connect to card gateway. Please try again.')
        setRedirecting(false)
        return
      }

      const { actionUrl, fields } = res.data.data

      // Build an invisible form and auto-submit it to CyberSource's hosted page
      const form = document.createElement('form')
      form.method = 'POST'
      form.action = actionUrl
      form.style.display = 'none'

      Object.entries(fields).forEach(([name, value]) => {
        const input = document.createElement('input')
        input.type  = 'hidden'
        input.name  = name
        input.value = value
        form.appendChild(input)
      })

      document.body.appendChild(form)
      form.submit()
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Failed to initiate payment. Please try again.')
      setRedirecting(false)
    }
  }

  // ── Share helpers ────────────────────────────────────────────────────────────
  const currentLink = window.location.href

  const copyLink = () => {
    navigator.clipboard.writeText(currentLink).then(() => {
      setLinkCopied(true)
      setTimeout(() => setLinkCopied(false), 2000)
    })
  }

  const shareWhatsApp = () => {
    const text = encodeURIComponent(
      `Please complete your card payment of KES ${order?.subtotal.toLocaleString()} for order #${order?.orderNumber}.\n\n${currentLink}`
    )
    window.open(`https://wa.me/?text=${text}`, '_blank')
  }

  const effectiveAmount = order?.subtotal || Number(amount) || 0

  // ── Render ───────────────────────────────────────────────────────────────────

  return (
    <div style={{
      minHeight: '100vh',
      background: 'linear-gradient(135deg, #0F172A 0%, #1E293B 60%, #0F172A 100%)',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '24px 16px',
      fontFamily: "'Inter', system-ui, sans-serif"
    }}>
      {/* Brand header */}
      <div style={{ textAlign: 'center', marginBottom: 28 }}>
        <div style={{ display: 'inline-flex', alignItems: 'center', gap: 10, marginBottom: 6 }}>
          <div style={{ background: 'linear-gradient(135deg,#16A34A,#15803D)', color: '#fff', padding: 10, borderRadius: 12, display: 'flex', boxShadow: '0 4px 14px rgba(22,163,74,0.4)' }}>
            <Shield size={22} />
          </div>
          <span style={{ fontSize: 22, fontWeight: 800, color: '#fff', letterSpacing: '-0.5px' }}>
            Biashara<span style={{ color: '#4ADE80' }}>360</span> Pay
          </span>
        </div>
        <p style={{ fontSize: 12, color: '#94A3B8', margin: 0 }}>
          Powered by CyberSource Secure Acceptance · PCI DSS Level 1
        </p>
      </div>

      <div style={{ width: '100%', maxWidth: 460 }}>

        {/* ── Loading ── */}
        {loadingOrder && (
          <Card>
            <div style={{ textAlign: 'center', padding: 40 }}>
              <div style={{ width: 40, height: 40, border: '3px solid #334155', borderTopColor: '#4ADE80', borderRadius: '50%', animation: 'spin 0.9s linear infinite', margin: '0 auto 16px' }} />
              <p style={{ color: '#94A3B8', fontSize: 14, margin: 0 }}>Loading payment details…</p>
            </div>
          </Card>
        )}

        {/* ── Error ── */}
        {!loadingOrder && orderError && (
          <Card>
            <div style={{ textAlign: 'center', padding: 32 }}>
              <XCircle size={48} color="#EF4444" style={{ marginBottom: 16 }} />
              <h3 style={{ fontSize: 18, fontWeight: 700, color: '#fff', marginBottom: 8 }}>Invalid Payment Link</h3>
              <p style={{ color: '#94A3B8', fontSize: 14, lineHeight: 1.6 }}>{orderError}</p>
            </div>
          </Card>
        )}

        {/* ── Already Paid (loaded order) ── */}
        {!loadingOrder && !orderError && order?.paymentStatus === 'PAID' && !status && (
          <SuccessScreen order={order} txnId='' amount={order.subtotal} />
        )}

        {/* ── SA-Return: Success ── */}
        {status === 'success' && !loadingOrder && (
          <SuccessScreen order={order} txnId={txnId} amount={effectiveAmount} orderId={orderId} />
        )}

        {/* ── SA-Return: Declined ── */}
        {status === 'declined' && !loadingOrder && (
          <Card>
            <div style={{ textAlign: 'center', padding: 32 }}>
              <div style={{ width: 64, height: 64, borderRadius: '50%', background: 'rgba(239,68,68,0.15)', color: '#EF4444', display: 'inline-flex', alignItems: 'center', justifyContent: 'center', marginBottom: 16 }}>
                <XCircle size={36} />
              </div>
              <h2 style={{ fontSize: 20, fontWeight: 700, color: '#fff', marginBottom: 8 }}>Payment Declined</h2>
              <p style={{ color: '#94A3B8', fontSize: 14, marginBottom: 24, lineHeight: 1.6 }}>
                Your card payment was not approved by your bank.<br />Please try a different card or contact your bank.
              </p>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6, fontSize: 12, color: '#64748B' }}>
                <Lock size={12} /> Order remains unpaid — link is still active.
              </div>
            </div>
          </Card>
        )}

        {/* ── SA-Return: Cancelled ── */}
        {status === 'cancelled' && !loadingOrder && (
          <Card>
            <div style={{ textAlign: 'center', padding: 32 }}>
              <div style={{ width: 64, height: 64, borderRadius: '50%', background: 'rgba(251,191,36,0.15)', color: '#FBBF24', display: 'inline-flex', alignItems: 'center', justifyContent: 'center', marginBottom: 16 }}>
                <AlertTriangle size={36} />
              </div>
              <h2 style={{ fontSize: 20, fontWeight: 700, color: '#fff', marginBottom: 8 }}>Payment Cancelled</h2>
              <p style={{ color: '#94A3B8', fontSize: 14, marginBottom: 24, lineHeight: 1.6 }}>
                You cancelled the payment. The order is still pending.
              </p>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6, fontSize: 12, color: '#64748B' }}>
                <Lock size={12} /> You can try again using the same link.
              </div>
            </div>
          </Card>
        )}

        {/* ── Main Payment Card ── */}
        {!loadingOrder && !orderError && !status && order && order.paymentStatus !== 'PAID' && (
          <Card>
            {/* Order summary */}
            <div style={{ background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 12, padding: 18, marginBottom: 24 }}>
              <div style={{ fontSize: 11, textTransform: 'uppercase', color: '#64748B', fontWeight: 700, letterSpacing: '0.8px', marginBottom: 6 }}>
                Payment Invoice
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
                <span style={{ fontWeight: 700, fontSize: 16, color: '#F1F5F9' }}>
                  {order.orderNumber ? `Order #${order.orderNumber}` : 'Payment Request'}
                </span>
                <span style={{ fontWeight: 800, fontSize: 26, color: '#4ADE80' }}>
                  KES {order.subtotal.toLocaleString()}
                </span>
              </div>
              {order.customerName && (
                <div style={{ fontSize: 13, color: '#64748B', marginTop: 4 }}>
                  {order.customerName}{order.customerPhone ? ` · ${order.customerPhone}` : ''}
                </div>
              )}
            </div>

            {/* How it works */}
            <div style={{ background: 'rgba(74,222,128,0.06)', border: '1px solid rgba(74,222,128,0.15)', borderRadius: 10, padding: '12px 16px', marginBottom: 20, fontSize: 13, color: '#94A3B8', lineHeight: 1.7 }}>
              <div style={{ display: 'flex', alignItems: 'flex-start', gap: 8 }}>
                <Lock size={14} color="#4ADE80" style={{ marginTop: 2, flexShrink: 0 }} />
                <div>
                  Clicking <strong style={{ color: '#F1F5F9' }}>Pay Now</strong> will securely redirect you to CyberSource's
                  hosted payment page. Your card details are entered directly on CyberSource's servers —
                  never shared with this merchant.
                </div>
              </div>
            </div>

            {/* Pay button */}
            <button
              onClick={handlePayNow}
              disabled={redirecting}
              id="btn-pay-now"
              style={{
                width: '100%', padding: '16px 0',
                background: redirecting
                  ? 'rgba(74,222,128,0.4)'
                  : 'linear-gradient(135deg,#16A34A,#15803D)',
                color: '#fff', border: 'none', borderRadius: 12,
                fontWeight: 700, fontSize: 16, cursor: redirecting ? 'not-allowed' : 'pointer',
                display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 10,
                boxShadow: redirecting ? 'none' : '0 4px 20px rgba(22,163,74,0.35)',
                transition: 'all 0.2s'
              }}
            >
              {redirecting ? (
                <>
                  <div style={{ width: 18, height: 18, border: '2px solid rgba(255,255,255,0.3)', borderTopColor: '#fff', borderRadius: '50%', animation: 'spin 0.8s linear infinite' }} />
                  Connecting to CyberSource…
                </>
              ) : (
                <>
                  <CreditCard size={18} />
                  Pay KES {order.subtotal.toLocaleString()} Now
                </>
              )}
            </button>

            {/* Share options */}
            <div style={{ marginTop: 20, paddingTop: 16, borderTop: '1px solid rgba(255,255,255,0.06)' }}>
              <div style={{ fontSize: 12, color: '#64748B', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: 10 }}>
                Share Payment Link
              </div>
              <div style={{ display: 'flex', gap: 10 }}>
                <button
                  onClick={shareWhatsApp}
                  style={{ flex: 1, padding: '10px 0', background: 'rgba(37,211,102,0.1)', border: '1px solid rgba(37,211,102,0.25)', borderRadius: 10, color: '#25D366', fontWeight: 600, fontSize: 13, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6 }}
                >
                  <Share2 size={14} /> WhatsApp
                </button>
                <button
                  onClick={copyLink}
                  style={{ flex: 1, padding: '10px 0', background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 10, color: linkCopied ? '#4ADE80' : '#94A3B8', fontWeight: 600, fontSize: 13, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6 }}
                >
                  {linkCopied ? <CheckCheck size={14} /> : <Copy size={14} />}
                  {linkCopied ? 'Copied!' : 'Copy Link'}
                </button>
              </div>
            </div>

            {/* Security badges */}
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 16, marginTop: 18, fontSize: 11, color: '#475569' }}>
              <span style={{ display: 'flex', alignItems: 'center', gap: 4 }}><Lock size={10} /> 256-Bit TLS</span>
              <span style={{ display: 'flex', alignItems: 'center', gap: 4 }}><Shield size={10} /> PCI DSS Level 1</span>
              <span style={{ display: 'flex', alignItems: 'center', gap: 4 }}><ExternalLink size={10} /> CyberSource</span>
            </div>
          </Card>
        )}

      </div>

      <style>{`
        @keyframes spin { to { transform: rotate(360deg); } }
        @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700;800&display=swap');
      `}</style>
    </div>
  )
}

// ─── Sub-components ───────────────────────────────────────────────────────────

function Card({ children }: { children: React.ReactNode }) {
  return (
    <div style={{
      background: 'rgba(255,255,255,0.04)',
      backdropFilter: 'blur(16px)',
      border: '1px solid rgba(255,255,255,0.08)',
      borderRadius: 20,
      padding: 28,
      boxShadow: '0 20px 60px rgba(0,0,0,0.4)'
    }}>
      {children}
    </div>
  )
}

function SuccessScreen({
  order, txnId, amount, orderId
}: {
  order: OrderResponse | null
  txnId: string
  amount: number
  orderId?: string
}) {
  return (
    <Card>
      <div style={{ textAlign: 'center', padding: '8px 0 16px' }}>
        <div style={{ width: 72, height: 72, borderRadius: '50%', background: 'rgba(74,222,128,0.15)', color: '#4ADE80', display: 'inline-flex', alignItems: 'center', justifyContent: 'center', marginBottom: 18, boxShadow: '0 0 30px rgba(74,222,128,0.2)' }}>
          <CheckCircle size={40} />
        </div>
        <h2 style={{ fontSize: 22, fontWeight: 800, color: '#fff', marginBottom: 6 }}>Payment Successful!</h2>
        <p style={{ color: '#94A3B8', fontSize: 14, marginBottom: 24 }}>
          Card payment of <strong style={{ color: '#4ADE80' }}>KES {(amount || order?.subtotal || 0).toLocaleString()}</strong> has been authorised and captured.
        </p>

        <div style={{ background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 12, padding: 18, textAlign: 'left', marginBottom: 20, fontSize: 13 }}>
          {(order?.orderNumber || orderId) && (
            <Row label="Order #" value={order?.orderNumber || orderId || ''} mono />
          )}
          {order?.customerName && <Row label="Customer" value={order.customerName} />}
          {txnId && <Row label="CyberSource Ref" value={txnId} mono green />}
        </div>

        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6, color: '#4ADE80', fontSize: 13, fontWeight: 600 }}>
          <Lock size={13} /> Order status updated to PAID
        </div>
      </div>
    </Card>
  )
}

function Row({ label, value, mono, green }: { label: string; value: string; mono?: boolean; green?: boolean }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8, alignItems: 'baseline' }}>
      <span style={{ color: '#64748B' }}>{label}</span>
      <span style={{
        fontWeight: 700,
        fontFamily: mono ? 'monospace' : 'inherit',
        color: green ? '#4ADE80' : '#F1F5F9',
        fontSize: mono ? 12 : 13
      }}>{value}</span>
    </div>
  )
}
