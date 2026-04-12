import React, { useState, useEffect } from 'react'
import { CreditCard, Shield, CheckCircle, XCircle, Clock, RefreshCw, Trash2, Plus, ChevronRight } from 'lucide-react'
import { Card, PageHeader, StatusBadge, DataTable, Btn, KpiCard } from '../components/ui'
import { cyberSourceApi, CsTransactionRecord, SavedCardResponse } from '../services/api'

// ── Card brand logo ───────────────────────────────────────────────────────────
function CardBrand({ type }: { type: string }) {
  const colors: Record<string, string> = { VISA: '#1A1F71', MASTERCARD: '#EB001B', AMEX: '#2E77BC' }
  return (
    <span style={{ fontSize: 10, fontWeight: 900, letterSpacing: 1, color: colors[type] ?? '#666',
      background: `${colors[type] ?? '#666'}15`, padding: '2px 6px', borderRadius: 4 }}>
      {type}
    </span>
  )
}

// ── Transaction status helpers ────────────────────────────────────────────────
function TxnStatus({ status }: { status: string }) {
  const map: Record<string, [string, string, React.ReactNode]> = {
    CAPTURED:    ['var(--b360-green)', 'var(--b360-green-bg)',  <CheckCircle size={12}/>],
    AUTHORIZED:  ['var(--b360-blue)',  'var(--b360-blue-bg)',   <Clock size={12}/>],
    REFUNDED:    ['var(--b360-amber)', 'var(--b360-amber-bg)',  <RefreshCw size={12}/>],
    DECLINED:    ['var(--b360-red)',   'var(--b360-red-bg)',    <XCircle size={12}/>],
    VOIDED:      ['#9E9E9E',           '#F5F5F5',               <XCircle size={12}/>],
    ERROR:       ['var(--b360-red)',   'var(--b360-red-bg)',    <XCircle size={12}/>],
  }
  const [color, bg, icon] = map[status] ?? ['#999', '#f5f5f5', null]
  return (
    <span style={{ display:'flex', alignItems:'center', gap:4, color, background:bg,
      borderRadius:20, padding:'3px 8px', fontSize:11, fontWeight:700, width:'fit-content' }}>
      {icon}{status}
    </span>
  )
}

// ── Unified Checkout Widget (simulated — in production loads CyberSource JS) ──
function UnifiedCheckoutWidget({ orderId, amount, onSuccess, onCancel }:
  { orderId: string; amount: number; onSuccess: (result: any) => void; onCancel: () => void }) {
  const [cardNum, setCardNum] = useState('')
  const [expiry, setExpiry] = useState('')
  const [cvv, setCvv] = useState('')
  const [name, setName] = useState('')
  const [saveCard, setSaveCard] = useState(false)
  const [processing, setProcessing] = useState(false)
  const [error, setError] = useState('')

  // Format card number with spaces
  const fmtCard = (v: string) => v.replace(/\D/g, '').slice(0, 16).replace(/(\d{4})/g, '$1 ').trim()
  const fmtExpiry = (v: string) => {
    const d = v.replace(/\D/g, '').slice(0, 4)
    return d.length > 2 ? `${d.slice(0,2)}/${d.slice(2)}` : d
  }

  const detectType = (n: string): string => {
    if (n.startsWith('4')) return 'VISA'
    if (n.startsWith('5') || n.startsWith('2')) return 'MASTERCARD'
    if (n.startsWith('3')) return 'AMEX'
    return ''
  }

  const cardType = detectType(cardNum.replace(/\s/g, ''))

  const handlePay = async () => {
    if (!cardNum || !expiry || !cvv || !name) { setError('All fields are required'); return }
    setProcessing(true); setError('')
    // Simulate CyberSource API call (in production: get capture context, tokenize via Flex, POST /charge)
    await new Promise(r => setTimeout(r, 1800))
    const raw = cardNum.replace(/\s/g, '')
    // Sandbox: 4111...1111 = decline, everything else = approve
    if (raw === '4111111111111111') {
      setProcessing(false); setError('Card declined. Please use a different card.')
      return
    }
    setProcessing(false)
    onSuccess({
      status: 'CAPTURED', approvalCode: 'HH' + Math.floor(Math.random()*9000+1000),
      cardLast4: raw.slice(-4), cardType, reconciliationId: Date.now().toString()
    })
  }

  return (
    <div style={{ background:'white', borderRadius:16, overflow:'hidden', boxShadow:'var(--shadow-md)', maxWidth:480, margin:'0 auto' }}>
      {/* Header */}
      <div style={{ background:'var(--b360-green)', padding:'20px 24px', color:'white' }}>
        <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:4 }}>
          <div style={{ display:'flex', alignItems:'center', gap:8 }}>
            <Shield size={18}/> <span style={{ fontWeight:700, fontSize:15 }}>Secure Card Payment</span>
          </div>
          <span style={{ fontSize:11, opacity:0.8 }}>Powered by CyberSource</span>
        </div>
        <div style={{ fontSize:13, opacity:0.9 }}>Order {orderId} · <strong>KES {amount.toLocaleString()}</strong></div>
      </div>

      <div style={{ padding:24, display:'flex', flexDirection:'column', gap:16 }}>
        {/* Card number */}
        <div>
          <label style={{ fontSize:12, fontWeight:600, color:'var(--b360-text-secondary)', display:'block', marginBottom:6 }}>
            Card Number
          </label>
          <div style={{ position:'relative' }}>
            <input
              value={cardNum} onChange={e => setCardNum(fmtCard(e.target.value))}
              placeholder="1234 5678 9012 3456" maxLength={19}
              style={{ width:'100%', padding:'11px 44px 11px 12px', border:'1.5px solid var(--b360-border)',
                borderRadius:8, fontSize:16, letterSpacing:2, fontFamily:'monospace', outline:'none',
                transition:'border-color 0.15s', boxSizing:'border-box' }}
              onFocus={e => e.target.style.borderColor = 'var(--b360-green)'}
              onBlur={e => e.target.style.borderColor = 'var(--b360-border)'}
            />
            {cardType && (
              <div style={{ position:'absolute', right:10, top:'50%', transform:'translateY(-50%)' }}>
                <CardBrand type={cardType}/>
              </div>
            )}
          </div>
        </div>

        <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:12 }}>
          <div>
            <label style={{ fontSize:12, fontWeight:600, color:'var(--b360-text-secondary)', display:'block', marginBottom:6 }}>
              Expiry (MM/YY)
            </label>
            <input value={expiry} onChange={e => setExpiry(fmtExpiry(e.target.value))}
              placeholder="12/27" maxLength={5}
              style={{ width:'100%', padding:'11px 12px', border:'1.5px solid var(--b360-border)', borderRadius:8,
                fontSize:14, fontFamily:'monospace', outline:'none', boxSizing:'border-box' }}
              onFocus={e => e.target.style.borderColor = 'var(--b360-green)'}
              onBlur={e => e.target.style.borderColor = 'var(--b360-border)'} />
          </div>
          <div>
            <label style={{ fontSize:12, fontWeight:600, color:'var(--b360-text-secondary)', display:'block', marginBottom:6 }}>
              CVV / CVC
            </label>
            <input value={cvv} onChange={e => setCvv(e.target.value.replace(/\D/,'').slice(0,4))}
              placeholder="123" type="password" maxLength={4}
              style={{ width:'100%', padding:'11px 12px', border:'1.5px solid var(--b360-border)', borderRadius:8,
                fontSize:14, fontFamily:'monospace', outline:'none', boxSizing:'border-box' }}
              onFocus={e => e.target.style.borderColor = 'var(--b360-green)'}
              onBlur={e => e.target.style.borderColor = 'var(--b360-border)'} />
          </div>
        </div>

        <div>
          <label style={{ fontSize:12, fontWeight:600, color:'var(--b360-text-secondary)', display:'block', marginBottom:6 }}>
            Cardholder Name
          </label>
          <input value={name} onChange={e => setName(e.target.value)}
            placeholder="AMINA HASSAN"
            style={{ width:'100%', padding:'11px 12px', border:'1.5px solid var(--b360-border)', borderRadius:8,
              fontSize:14, textTransform:'uppercase', outline:'none', boxSizing:'border-box' }}
            onFocus={e => e.target.style.borderColor = 'var(--b360-green)'}
            onBlur={e => e.target.style.borderColor = 'var(--b360-border)'} />
        </div>

        <label style={{ display:'flex', alignItems:'center', gap:8, fontSize:13, cursor:'pointer' }}>
          <input type="checkbox" checked={saveCard} onChange={e => setSaveCard(e.target.checked)}
            style={{ accentColor:'var(--b360-green)' }}/>
          Save card for future payments
        </label>

        {error && (
          <div style={{ display:'flex', alignItems:'center', gap:8, padding:'10px 12px',
            background:'var(--b360-red-bg)', borderRadius:8, color:'var(--b360-red)', fontSize:13 }}>
            <XCircle size={15}/> {error}
          </div>
        )}

        <div style={{ display:'flex', gap:10 }}>
          <button onClick={onCancel} style={{ flex:1, padding:12, border:'1px solid var(--b360-border)',
            borderRadius:8, fontSize:13, fontWeight:600, cursor:'pointer', background:'white' }}>
            Cancel
          </button>
          <button onClick={handlePay} disabled={processing} style={{
            flex:2, padding:12, background: processing ? '#ccc' : 'var(--b360-green)',
            color:'white', border:'none', borderRadius:8, fontSize:14, fontWeight:700,
            cursor: processing ? 'not-allowed' : 'pointer', display:'flex', alignItems:'center', justifyContent:'center', gap:8
          }}>
            {processing ? (
              <><span style={{ animation:'spin 1s linear infinite', display:'inline-block' }}>⟳</span> Processing...</>
            ) : (
              <><Shield size={15}/> Pay KES {amount.toLocaleString()}</>
            )}
          </button>
        </div>

        <div style={{ display:'flex', alignItems:'center', justifyContent:'center', gap:6, fontSize:11, color:'var(--b360-text-secondary)' }}>
          <Shield size={12}/> PCI DSS Level 1 · 256-bit TLS · CyberSource Unified Checkout
        </div>
        <div style={{ fontSize:11, color:'var(--b360-text-secondary)', textAlign:'center' }}>
          🧪 Sandbox: use any number except <code>4111 1111 1111 1111</code> to approve
        </div>
      </div>
    </div>
  )
}

// ── Saved Card Picker ─────────────────────────────────────────────────────────
function SavedCardPicker({ cards, onSelect, selectedId, onNew }:
  { cards: SavedCardResponse[]; onSelect: (id: string) => void; selectedId: string; onNew: () => void }) {
  return (
    <div style={{ display:'flex', flexDirection:'column', gap:8 }}>
      {cards.map(card => (
        <div key={card.id} onClick={() => onSelect(card.id)} style={{
          display:'flex', alignItems:'center', gap:12, padding:'12px 16px',
          border:`2px solid ${selectedId === card.id ? 'var(--b360-green)' : 'var(--b360-border)'}`,
          borderRadius:10, cursor:'pointer', background: selectedId === card.id ? 'var(--b360-green-bg)' : 'white',
          transition:'all 0.15s'
        }}>
          <CreditCard size={20} color={selectedId === card.id ? 'var(--b360-green)' : 'var(--b360-text-secondary)'} />
          <div style={{ flex:1 }}>
            <div style={{ display:'flex', alignItems:'center', gap:8 }}>
              <CardBrand type={card.type}/>
              <span style={{ fontWeight:600, fontSize:13 }}>•••• {card.last4}</span>
              {card.isDefault && <span style={{ fontSize:10, color:'var(--b360-green)', fontWeight:700 }}>DEFAULT</span>}
            </div>
            <div style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>{card.holder} · Exp {card.expiry}</div>
          </div>
          {selectedId === card.id && <CheckCircle size={18} color='var(--b360-green)'/>}
        </div>
      ))}
      <button onClick={onNew} style={{
        display:'flex', alignItems:'center', gap:8, padding:'12px 16px',
        border:'2px dashed var(--b360-border)', borderRadius:10, cursor:'pointer',
        background:'none', color:'var(--b360-text-secondary)', fontSize:13, fontWeight:500
      }}>
        <Plus size={16}/> Use a different card
      </button>
    </div>
  )
}

// ── Payment Result Banner ─────────────────────────────────────────────────────
function PaymentResult({ result, onClose }: { result: any; onClose: () => void }) {
  const success = result.status === 'CAPTURED' || result.status === 'AUTHORIZED'
  return (
    <div style={{ textAlign:'center', padding:32, background:'white', borderRadius:16, boxShadow:'var(--shadow-md)' }}>
      <div style={{ fontSize:56, marginBottom:16 }}>{success ? '✅' : '❌'}</div>
      <h2 style={{ fontWeight:800, fontSize:20, marginBottom:8, color: success ? 'var(--b360-green)' : 'var(--b360-red)' }}>
        {success ? 'Payment Successful!' : 'Payment Failed'}
      </h2>
      {success && (
        <div style={{ display:'flex', flexDirection:'column', gap:8, margin:'16px auto', maxWidth:300,
          padding:16, background:'var(--b360-surface)', borderRadius:10, textAlign:'left' }}>
          <div style={{ display:'flex', justifyContent:'space-between', fontSize:13 }}>
            <span style={{ color:'var(--b360-text-secondary)' }}>Status</span>
            <TxnStatus status={result.status}/>
          </div>
          <div style={{ display:'flex', justifyContent:'space-between', fontSize:13 }}>
            <span style={{ color:'var(--b360-text-secondary)' }}>Approval Code</span>
            <span style={{ fontWeight:700, fontFamily:'monospace' }}>{result.approvalCode}</span>
          </div>
          <div style={{ display:'flex', justifyContent:'space-between', fontSize:13 }}>
            <span style={{ color:'var(--b360-text-secondary)' }}>Card</span>
            <span style={{ fontWeight:600 }}><CardBrand type={result.cardType}/> •••• {result.cardLast4}</span>
          </div>
          <div style={{ display:'flex', justifyContent:'space-between', fontSize:13 }}>
            <span style={{ color:'var(--b360-text-secondary)' }}>Reconciliation ID</span>
            <span style={{ fontSize:11, fontFamily:'monospace', color:'var(--b360-text-secondary)' }}>{result.reconciliationId}</span>
          </div>
        </div>
      )}
      <button onClick={onClose} style={{ padding:'10px 24px', background:'var(--b360-green)', color:'white',
        border:'none', borderRadius:8, fontWeight:700, cursor:'pointer', marginTop:8 }}>
        Done
      </button>
    </div>
  )
}

// ── Main CyberSource Payments Page ────────────────────────────────────────────
export default function CyberSourcePage() {
  const [activeTab, setActiveTab] = useState<'charge' | 'transactions' | 'saved'>('charge')
  const [payMethod, setPayMethod] = useState<'new' | 'saved'>('saved')
  const [selectedCard, setSelectedCard] = useState('')
  const [amount, setAmount] = useState('4500')
  const [orderId, setOrderId] = useState('B360-0042')
  const [showWidget, setShowWidget] = useState(false)
  const [payResult, setPayResult] = useState<any>(null)
  const [transactions, setTransactions] = useState<CsTransactionRecord[]>([])
  const [savedCards, setSavedCards] = useState<SavedCardResponse[]>([])
  const [refundModal, setRefundModal] = useState<CsTransactionRecord | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([
      cyberSourceApi.getTransactions(),
      cyberSourceApi.getSavedCards(),
    ]).then(([txns, cards]) => {
      if (txns.success && txns.data) setTransactions(txns.data)
      if (cards.success && cards.data) {
        setSavedCards(cards.data)
        const def = cards.data.find(c => c.isDefault)
        if (def) setSelectedCard(def.id)
        else if (cards.data.length > 0) setSelectedCard(cards.data[0].id)
      }
    }).finally(() => setLoading(false))
  }, [])

  async function deleteCard(id: string) {
    await cyberSourceApi.deleteSavedCard(id)
    setSavedCards(prev => prev.filter(c => c.id !== id))
  }

  const captured   = transactions.filter(t => t.status === 'CAPTURED').reduce((s, t) => s + t.amount, 0)
  const authorized = transactions.filter(t => t.status === 'AUTHORIZED').reduce((s, t) => s + t.amount, 0)
  const declined   = transactions.filter(t => t.status === 'DECLINED').length
  const refunded   = transactions.filter(t => t.status === 'REFUNDED').reduce((s, t) => s + t.amount, 0)

  const Tab = ({ id, label }: { id: typeof activeTab; label: string }) => (
    <button onClick={() => setActiveTab(id)} style={{
      padding:'8px 20px', borderRadius:8, fontSize:13, fontWeight:600, cursor:'pointer', border:'none',
      background: activeTab === id ? 'var(--b360-green)' : 'white',
      color: activeTab === id ? 'white' : 'var(--b360-text-secondary)',
      boxShadow: activeTab === id ? 'var(--shadow-sm)' : 'none'
    }}>{label}</button>
  )

  return (
    <div className="fade-in" style={{ display:'flex', flexDirection:'column', gap:20 }}>
      <PageHeader title="Card Payments — CyberSource" />

      {/* KPI Row */}
      <div style={{ display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:14 }}>
        <KpiCard title="Captured (Settled)"  value={`KES ${captured.toLocaleString()}`}  change="Ready to withdraw"   icon={<CheckCircle size={18}/>} color="var(--b360-green)" />
        <KpiCard title="Authorized (Pending)" value={`KES ${authorized.toLocaleString()}`} change="Awaiting capture"    icon={<Clock size={18}/>}       color="var(--b360-blue)" />
        <KpiCard title="Refunded"            value={`KES ${refunded.toLocaleString()}`}   change="Returned to customers" icon={<RefreshCw size={18}/>}  color="var(--b360-amber)" />
        <KpiCard title="Declined"            value={`${declined} txns`}                   change="Review with customers" icon={<XCircle size={18}/>}    color="var(--b360-red)" />
      </div>

      {/* Tabs */}
      <div style={{ display:'flex', gap:6, background:'var(--b360-surface)', padding:4, borderRadius:10, width:'fit-content', border:'1px solid var(--b360-border)' }}>
        <Tab id="charge"       label="💳  Charge a Card"/>
        <Tab id="transactions" label="📋  Transaction History"/>
        <Tab id="saved"        label="🔐  Saved Cards"/>
      </div>

      {/* ── Charge Tab ── */}
      {activeTab === 'charge' && !showWidget && !payResult && (
        <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:20, alignItems:'start' }}>
          {/* Left: Order details */}
          <Card style={{ padding:24 }}>
            <h3 style={{ fontWeight:700, marginBottom:20 }}>Order Details</h3>
            <div style={{ display:'flex', flexDirection:'column', gap:14 }}>
              <div>
                <label style={{ fontSize:12, fontWeight:500, color:'var(--b360-text-secondary)', display:'block', marginBottom:5 }}>Order ID</label>
                <input value={orderId} onChange={e => setOrderId(e.target.value)}
                  style={{ width:'100%', padding:'9px 12px', border:'1px solid var(--b360-border)', borderRadius:8, fontSize:13, outline:'none', boxSizing:'border-box' }} />
              </div>
              <div>
                <label style={{ fontSize:12, fontWeight:500, color:'var(--b360-text-secondary)', display:'block', marginBottom:5 }}>Amount (KES)</label>
                <input value={amount} onChange={e => setAmount(e.target.value)} type="number"
                  style={{ width:'100%', padding:'9px 12px', border:'1px solid var(--b360-border)', borderRadius:8, fontSize:18, fontWeight:700, outline:'none', boxSizing:'border-box' }} />
              </div>

              <div style={{ display:'flex', gap:8 }}>
                <button onClick={() => setPayMethod('saved')} style={{
                  flex:1, padding:10, border:`2px solid ${payMethod==='saved'?'var(--b360-green)':'var(--b360-border)'}`,
                  borderRadius:8, background: payMethod==='saved'?'var(--b360-green-bg)':'white',
                  color: payMethod==='saved'?'var(--b360-green)':'var(--b360-text)', cursor:'pointer', fontWeight:600, fontSize:12
                }}>🔐 Saved Card</button>
                <button onClick={() => setPayMethod('new')} style={{
                  flex:1, padding:10, border:`2px solid ${payMethod==='new'?'var(--b360-green)':'var(--b360-border)'}`,
                  borderRadius:8, background: payMethod==='new'?'var(--b360-green-bg)':'white',
                  color: payMethod==='new'?'var(--b360-green)':'var(--b360-text)', cursor:'pointer', fontWeight:600, fontSize:12
                }}>💳 New Card</button>
              </div>

              {payMethod === 'saved' && (
                <SavedCardPicker cards={savedCards} selectedId={selectedCard}
                  onSelect={setSelectedCard} onNew={() => setPayMethod('new')} />
              )}

              <button onClick={() => setShowWidget(true)} style={{
                padding:'13px 0', background:'var(--b360-green)', color:'white', border:'none',
                borderRadius:10, fontSize:15, fontWeight:700, cursor:'pointer',
                display:'flex', alignItems:'center', justifyContent:'center', gap:8
              }}>
                <Shield size={16}/> {payMethod === 'saved' ? 'Charge Saved Card' : 'Open Secure Checkout'}
                <ChevronRight size={16}/>
              </button>
            </div>
          </Card>

          {/* Right: How it works */}
          <Card style={{ padding:24 }}>
            <h3 style={{ fontWeight:700, marginBottom:16 }}>How CyberSource Works</h3>
            {[
              { step:'1', title:'Capture Context', desc:'Backend requests a short-lived JWT from CyberSource (GET /capture-context). This initializes the secure hosted fields.', icon:'🔑' },
              { step:'2', title:'Unified Checkout Widget', desc:'Customer enters card details in CyberSource-hosted iframe. Card data never touches your server — zero PCI scope.', icon:'💳' },
              { step:'3', title:'Flex Tokenization', desc:'CyberSource returns a transient token (flexToken). Your frontend sends this token — not raw card data — to your Ktor backend.', icon:'🔐' },
              { step:'4', title:'Backend Charges', desc:'Ktor backend calls POST /pts/v2/payments with the flexToken. CyberSource authorizes and optionally captures the payment.', icon:'⚡' },
              { step:'5', title:'Settlement', desc:'Captured funds settle to your merchant account within 1-2 business days. Reconciliation IDs link to your orders.', icon:'✅' },
            ].map(item => (
              <div key={item.step} style={{ display:'flex', gap:12, marginBottom:14 }}>
                <div style={{ width:28, height:28, borderRadius:'50%', background:'var(--b360-green)', color:'white',
                  display:'flex', alignItems:'center', justifyContent:'center', fontSize:12, fontWeight:800, flexShrink:0 }}>
                  {item.step}
                </div>
                <div>
                  <div style={{ fontWeight:600, fontSize:13 }}>{item.icon} {item.title}</div>
                  <div style={{ fontSize:12, color:'var(--b360-text-secondary)', marginTop:2 }}>{item.desc}</div>
                </div>
              </div>
            ))}

            <div style={{ padding:12, background:'var(--b360-amber-bg)', borderRadius:8, fontSize:12, color:'var(--b360-amber)', marginTop:4 }}>
              ⚠️ <strong>Sandbox mode.</strong> Get live credentials from your CyberSource Business Center and set <code>CS_MERCHANT_ID</code>, <code>CS_MERCHANT_KEY_ID</code>, <code>CS_MERCHANT_SECRET_KEY</code> in your environment.
            </div>
          </Card>
        </div>
      )}

      {/* Widget or result */}
      {activeTab === 'charge' && showWidget && !payResult && (
        <UnifiedCheckoutWidget
          orderId={orderId} amount={parseFloat(amount) || 0}
          onSuccess={r => { setShowWidget(false); setPayResult(r) }}
          onCancel={() => setShowWidget(false)}
        />
      )}
      {activeTab === 'charge' && payResult && (
        <div style={{ maxWidth:500, margin:'0 auto' }}>
          <PaymentResult result={payResult} onClose={() => { setPayResult(null); setShowWidget(false) }} />
        </div>
      )}

      {/* ── Transaction History Tab ── */}
      {activeTab === 'transactions' && (
        <Card>
          {loading ? (
            <div style={{ padding:40, textAlign:'center', color:'var(--b360-text-secondary)' }}>Loading...</div>
          ) : transactions.length === 0 ? (
            <div style={{ padding:40, textAlign:'center', color:'var(--b360-text-secondary)' }}>No transactions yet</div>
          ) : (
          <DataTable
            headers={['CS Transaction ID', 'Order', 'Type', 'Card', 'Amount', 'Status', 'Approval', 'Reconciliation ID', 'Date', 'Actions']}
            rows={transactions.map(t => [
              <span style={{ fontFamily:'monospace', fontSize:11, color:'var(--b360-text-secondary)' }}>
                {t.csTransactionId.slice(0, 14)}…
              </span>,
              <span style={{ fontWeight:700, color:'var(--b360-green)', fontSize:12 }}>{t.orderId}</span>,
              <span style={{ fontSize:11, fontWeight:600, color:'var(--b360-text-secondary)' }}>{t.type}</span>,
              <span style={{ display:'flex', alignItems:'center', gap:6 }}>
                <CardBrand type={t.cardType}/> <span style={{ fontSize:12 }}>••{t.cardLast4}</span>
              </span>,
              <span style={{ fontWeight:700 }}>KES {t.amount.toLocaleString()}</span>,
              <TxnStatus status={t.status}/>,
              <span style={{ fontFamily:'monospace', fontSize:12, fontWeight:600 }}>{t.approvalCode || '—'}</span>,
              <span style={{ fontFamily:'monospace', fontSize:10, color:'var(--b360-text-secondary)' }}>
                {t.reconciliationId ? t.reconciliationId.slice(0,16)+'…' : '—'}
              </span>,
              <span style={{ color:'var(--b360-text-secondary)', fontSize:12 }}>{new Date(t.createdAt).toLocaleDateString('en-KE')}</span>,
              <div style={{ display:'flex', gap:4 }}>
                {t.status === 'AUTHORIZED' && (
                  <Btn variant="secondary" small icon={<CheckCircle size={11}/>}>Capture</Btn>
                )}
                {t.status === 'CAPTURED' && (
                  <Btn variant="secondary" small icon={<RefreshCw size={11}/>} onClick={() => setRefundModal(t)}>Refund</Btn>
                )}
                {t.status === 'AUTHORIZED' && (
                  <Btn variant="danger" small icon={<XCircle size={11}/>}>Void</Btn>
                )}
              </div>
            ])}
          />
          )}
        </Card>
      )}

      {/* Refund modal */}
      {refundModal && (
        <div style={{ position:'fixed', inset:0, background:'rgba(0,0,0,0.4)', display:'flex', alignItems:'center', justifyContent:'center', zIndex:1000 }}>
          <Card style={{ padding:28, width:380 }}>
            <h3 style={{ fontWeight:800, marginBottom:16 }}>Refund Payment</h3>
            <div style={{ fontSize:13, marginBottom:16, color:'var(--b360-text-secondary)' }}>
              Order <strong>{refundModal.orderId}</strong> · Card ••{refundModal.cardLast4}
            </div>
            <div style={{ marginBottom:16 }}>
              <label style={{ fontSize:12, fontWeight:500, display:'block', marginBottom:5 }}>Refund Amount (KES)</label>
              <input defaultValue={refundModal.amount} type="number"
                style={{ width:'100%', padding:10, border:'1px solid var(--b360-border)', borderRadius:8, fontSize:14, fontWeight:700, outline:'none', boxSizing:'border-box' }}/>
            </div>
            <div style={{ display:'flex', gap:8 }}>
              <Btn variant="secondary" onClick={() => setRefundModal(null)}>Cancel</Btn>
              <Btn icon={<RefreshCw size={13}/>} onClick={() => setRefundModal(null)}>Process Refund</Btn>
            </div>
          </Card>
        </div>
      )}

      {/* ── Saved Cards Tab ── */}
      {activeTab === 'saved' && (
        <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:20, alignItems:'start' }}>
          <Card style={{ padding:20 }}>
            <h3 style={{ fontWeight:700, marginBottom:16 }}>Tokenized Cards (CyberSource TMS)</h3>
            {loading ? (
              <div style={{ padding:30, textAlign:'center', color:'var(--b360-text-secondary)' }}>Loading...</div>
            ) : (
            <div style={{ display:'flex', flexDirection:'column', gap:10 }}>
              {savedCards.length === 0 && (
                <div style={{ padding:20, textAlign:'center', color:'var(--b360-text-secondary)', fontSize:13 }}>No saved cards yet</div>
              )}
              {savedCards.map(card => (
                <div key={card.id} style={{ display:'flex', alignItems:'center', gap:12, padding:'12px 16px',
                  border:'1px solid var(--b360-border)', borderRadius:10, background:'white' }}>
                  <CreditCard size={20} color='var(--b360-text-secondary)'/>
                  <div style={{ flex:1 }}>
                    <div style={{ display:'flex', alignItems:'center', gap:8 }}>
                      <CardBrand type={card.type}/>
                      <span style={{ fontWeight:700 }}>•••• •••• •••• {card.last4}</span>
                      {card.isDefault && <span style={{ fontSize:10, color:'var(--b360-green)', fontWeight:700, background:'var(--b360-green-bg)', padding:'2px 6px', borderRadius:10 }}>DEFAULT</span>}
                    </div>
                    <div style={{ fontSize:12, color:'var(--b360-text-secondary)', marginTop:2 }}>{card.holder} · Exp {card.expiry}</div>
                  </div>
                  <button onClick={() => deleteCard(card.id)} style={{ color:'var(--b360-red)', background:'var(--b360-red-bg)', border:'none',
                    borderRadius:6, padding:'6px 8px', cursor:'pointer' }}>
                    <Trash2 size={13}/>
                  </button>
                </div>
              ))}
              <Btn variant="secondary" icon={<Plus size={14}/>}>Add New Card</Btn>
            </div>
            )}
          </Card>
          <Card style={{ padding:20 }}>
            <h3 style={{ fontWeight:700, marginBottom:12 }}>About Card Tokenization</h3>
            <div style={{ fontSize:13, color:'var(--b360-text-secondary)', lineHeight:1.7 }}>
              <p style={{ marginBottom:10 }}>Card numbers are never stored on Biashara360 servers. CyberSource Token Management Service (TMS) securely stores card data and returns a customer token ID.</p>
              <p style={{ marginBottom:10 }}>When charging a saved card, the backend sends the customer token to CyberSource — no raw card data is ever transmitted through your infrastructure.</p>
              <p>This means you operate with <strong style={{ color:'var(--b360-green)' }}>zero PCI DSS scope</strong> for card storage.</p>
            </div>
            <div style={{ marginTop:16, padding:12, background:'var(--b360-green-bg)', borderRadius:8, fontSize:12, color:'var(--b360-green)' }}>
              🔐 PCI DSS Level 1 Compliant · CyberSource Handles All Card Data
            </div>
          </Card>
        </div>
      )}
    </div>
  )
}
