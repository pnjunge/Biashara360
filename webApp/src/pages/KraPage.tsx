import React, { useState, useEffect } from 'react'
import {
  CheckCircle, AlertTriangle, Clock, Upload, Download, RefreshCw,
  Shield, Wifi, WifiOff, FileText, ExternalLink, Info, ChevronRight,
  Zap, BarChart2, Settings
} from 'lucide-react'
import { PageHeader, Card, Btn, KpiCard } from '../components/ui'
import { kraApi, KraComplianceStatus, EtimsInvoiceResponse, TaxReturnResponse } from '../services/api'

// ── Helpers ───────────────────────────────────────────────────────────────────
const ETIMS_STATUS_STYLE: Record<string, { bg: string; color: string; label: string }> = {
  TRANSMITTED: { bg:'#E8F5E9', color:'#1B8B34', label:'Transmitted' },
  PENDING:     { bg:'#FFF8E1', color:'#FF8F00', label:'Pending' },
  REJECTED:    { bg:'#FFEBEE', color:'#C62828', label:'Rejected' },
  ERROR:       { bg:'#FFEBEE', color:'#C62828', label:'Error' },
}
const RETURN_STATUS_STYLE: Record<string, { bg: string; color: string }> = {
  DRAFT:        { bg:'#F5F5F5', color:'#757575' },
  GENERATED:    { bg:'#E3F2FD', color:'#1565C0' },
  SUBMITTED:    { bg:'#FFF8E1', color:'#FF8F00' },
  ACKNOWLEDGED: { bg:'#E8F5E9', color:'#1B8B34' },
}
const G = '#1B8B34'   // B360 green

function pill(bg: string, color: string, text: string) {
  return <span style={{ background:bg, color, borderRadius:20, padding:'3px 12px', fontSize:11, fontWeight:700 }}>{text}</span>
}

function ScoreRing({ score }: { score: number }) {
  const color = score >= 80 ? G : score >= 50 ? '#FF8F00' : '#C62828'
  const r = 38, circ = 2 * Math.PI * r
  const dash = (score / 100) * circ
  return (
    <svg width={96} height={96} viewBox="0 0 96 96">
      <circle cx={48} cy={48} r={r} fill="none" stroke="#E0E0E0" strokeWidth={8} />
      <circle cx={48} cy={48} r={r} fill="none" stroke={color} strokeWidth={8}
        strokeDasharray={`${dash} ${circ}`} strokeLinecap="round"
        transform="rotate(-90 48 48)" />
      <text x={48} y={48} textAnchor="middle" dominantBaseline="central"
        style={{ fontSize:20, fontWeight:900, fill:color }}>{score}</text>
      <text x={48} y={62} textAnchor="middle"
        style={{ fontSize:9, fill:'#888' }}>/100</text>
    </svg>
  )
}

// ── Tab: Compliance Overview ──────────────────────────────────────────────────
function ComplianceTab() {
  const [compliance, setCompliance] = useState<KraComplianceStatus | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    kraApi.getCompliance().then(res => {
      if (res.success && res.data) setCompliance(res.data)
    }).finally(() => setLoading(false))
  }, [])

  if (loading) return <div style={{ padding:40, textAlign:'center', color:'#888' }}>Loading...</div>

  const c = compliance || {
    pin: null, isEtimsRegistered: false, isVatRegistered: false,
    complianceScore: 0, etimsTransmissionRate: 0,
    pendingReturns: [], overdueReturns: [], recommendations: [], lastEtimsTransmission: null
  }

  return (
    <div style={{ display:'flex', flexDirection:'column', gap:16 }}>

      {/* Header row */}
      <div style={{ display:'grid', gridTemplateColumns:'auto 1fr', gap:16 }}>
        <Card style={{ padding:20, display:'flex', alignItems:'center', gap:20, minWidth:260 }}>
          <ScoreRing score={c.complianceScore} />
          <div>
            <div style={{ fontWeight:800, fontSize:16 }}>Compliance Score</div>
            <div style={{ fontSize:12, color:'#666', marginTop:4 }}>
              {c.complianceScore >= 80 ? '✅ Good standing' : c.complianceScore >= 50 ? '⚠️ Needs attention' : '🚨 Action required'}
            </div>
            <div style={{ marginTop:10, display:'flex', gap:8, flexWrap:'wrap' }}>
              {c.isVatRegistered && pill('#E8F5E9', G, 'VAT Registered')}
              {c.isEtimsRegistered ? pill('#E8F5E9', G, 'eTIMS Active') : pill('#FFEBEE','#C62828','eTIMS Not Set Up')}
            </div>
          </div>
        </Card>

        <div style={{ display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:12 }}>
          <KpiCard title="KRA PIN" value={c.pin ?? 'Not set'} change="Taxpayer ID" icon={<Shield size={18}/>} color={G} />
          <KpiCard title="eTIMS Rate" value={`${(c.etimsTransmissionRate*100).toFixed(0)}%`} change="Invoices transmitted" icon={<Wifi size={18}/>} color="#1565C0" />
          <KpiCard title="Last Transmission" value={c.lastEtimsTransmission ? 'Today 14:22' : 'Never'} change="eTIMS invoice sent" icon={<Zap size={18}/>} color="#6A1B9A" />
        </div>
      </div>

      {/* Recommendations */}
      {c.recommendations.length > 0 && (
        <Card style={{ padding:20 }}>
          <div style={{ fontWeight:700, fontSize:14, marginBottom:12 }}>Action Items</div>
          {c.recommendations.map((r, i) => (
            <div key={i} style={{ display:'flex', alignItems:'flex-start', gap:10, padding:'10px 0', borderBottom: i < c.recommendations.length-1 ? '1px solid #F0F0F0' : 'none' }}>
              <div style={{ flex:1, fontSize:13 }}>{r}</div>
              <ChevronRight size={16} color="#999" style={{ marginTop:2, flexShrink:0 }} />
            </div>
          ))}
        </Card>
      )}

      {/* Pending returns */}
      {(c.pendingReturns.length > 0 || c.overdueReturns.length > 0) && (
        <Card style={{ padding:20 }}>
          <div style={{ fontWeight:700, fontSize:14, marginBottom:12 }}>Pending Returns</div>
          {[...c.overdueReturns.map(r => ({...r, overdue:true})), ...c.pendingReturns.map(r => ({...r, overdue:false}))].map((r, i) => (
            <div key={i} style={{ display:'flex', justifyContent:'space-between', alignItems:'center', padding:'12px 0', borderBottom:'1px solid #F0F0F0' }}>
              <div style={{ display:'flex', alignItems:'center', gap:12 }}>
                {r.overdue ? <AlertTriangle size={18} color="#C62828"/> : <Clock size={18} color="#FF8F00"/>}
                <div>
                  <div style={{ fontWeight:600, fontSize:13 }}>{r.returnType} — {r.period}</div>
                  <div style={{ fontSize:11, color:'#888' }}>Due {r.dueDate}</div>
                </div>
              </div>
              <div style={{ display:'flex', alignItems:'center', gap:12 }}>
                <div style={{ textAlign:'right' }}>
                  <div style={{ fontSize:11, color:'#999' }}>Est. Amount</div>
                  <div style={{ fontWeight:700 }}>KES {r.estimatedAmount.toLocaleString()}</div>
                </div>
                {pill(r.overdue ? '#FFEBEE' : '#FFF8E1', r.overdue ? '#C62828' : '#FF8F00', r.overdue ? 'OVERDUE' : 'PENDING')}
              </div>
            </div>
          ))}
        </Card>
      )}

      {/* iTax filing guide */}
      <Card style={{ padding:20, background:'#F8F9FF', border:'1px solid #C5CAE9' }}>
        <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:14 }}>
          <div style={{ fontWeight:700, fontSize:14, color:'#1565C0' }}>📋 iTax Filing Guide</div>
          <a href="https://itax.kra.go.ke" target="_blank" rel="noreferrer"
            style={{ fontSize:12, color:'#1565C0', textDecoration:'none', display:'flex', alignItems:'center', gap:4 }}>
            Open iTax Portal <ExternalLink size={12} />
          </a>
        </div>
        {[
          ['1.', 'Generate return below (VAT3 / TOT / WHT tab)'],
          ['2.', 'Download the KRA-format CSV file'],
          ['3.', 'Log in at itax.kra.go.ke → Returns → File Returns'],
          ['4.', 'Select your return type and period, upload the CSV'],
          ['5.', 'Copy the acknowledgement number back into Biashara360'],
        ].map(([num, step]) => (
          <div key={num} style={{ display:'flex', gap:10, marginBottom:8, fontSize:13 }}>
            <span style={{ fontWeight:800, color:'#1565C0', minWidth:20 }}>{num}</span>
            <span style={{ color:'#333' }}>{step}</span>
          </div>
        ))}
      </Card>
    </div>
  )
}

// ── Tab: eTIMS Invoices ───────────────────────────────────────────────────────
function EtimsTab() {
  const [invoices, setInvoices] = useState<EtimsInvoiceResponse[]>([])
  const [filter, setFilter] = useState('ALL')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    kraApi.getEtimsHistory().then(res => {
      if (res.success && res.data) setInvoices(res.data)
    }).finally(() => setLoading(false))
  }, [])

  const transmitted = invoices.filter(i => i.status === 'TRANSMITTED').length
  const errors      = invoices.filter(i => i.status === 'ERROR' || i.status === 'PENDING').length
  const rate        = invoices.length > 0 ? (transmitted / invoices.length * 100).toFixed(0) : '0'

  const filtered = filter === 'ALL' ? invoices : invoices.filter(i => i.status === filter)

  async function retryFailed() {
    try {
      await kraApi.retryEtims()
      const res = await kraApi.getEtimsHistory()
      if (res.success && res.data) setInvoices(res.data)
    } catch (_) {}
  }

  return (
    <div style={{ display:'flex', flexDirection:'column', gap:16 }}>
      {/* Stats */}
      <div style={{ display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:12 }}>
        <KpiCard title="Transmitted"     value={String(transmitted)}  change="Successfully signed" icon={<CheckCircle size={18}/>} color={G} />
        <KpiCard title="Failed/Pending"  value={String(errors)}        change="Need retransmission" icon={<AlertTriangle size={18}/>} color={errors > 0 ? '#C62828' : '#999'} />
        <KpiCard title="Transmission Rate" value={`${rate}%`}          change="KRA compliance"     icon={<BarChart2 size={18}/>} color="#1565C0" />
        <KpiCard title="Total Invoices"   value={String(invoices.length)} change="All time"         icon={<Zap size={18}/>} color="#6A1B9A" />
      </div>

      {/* What is eTIMS banner */}
      <div style={{ background:'#E8F5E9', border:'1px solid #A5D6A7', borderRadius:10, padding:'14px 18px' }}>
        <div style={{ fontWeight:700, color:G, fontSize:13, marginBottom:4 }}>What is KRA eTIMS?</div>
        <div style={{ fontSize:12, color:'#388E3C', lineHeight:1.6 }}>
          eTIMS (Electronic Tax Invoice Management System) is KRA's mandatory real-time invoice signing system.
          Every sale must be transmitted to KRA as it happens. Each transmitted invoice receives a unique
          KRA number and QR code printed on the customer's receipt, verifiable at <strong>etims.kra.go.ke</strong>.
          Mandatory for VAT-registered businesses from January 2024.
        </div>
      </div>

      {/* Controls */}
      <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center' }}>
        <div style={{ display:'flex', gap:6 }}>
          {(['ALL','TRANSMITTED','PENDING','ERROR']).map(f => (
            <button key={f} onClick={() => setFilter(f)} style={{
              padding:'6px 14px', borderRadius:20, fontSize:12, fontWeight:600, cursor:'pointer',
              border:`1px solid ${filter===f ? (f==='TRANSMITTED'?G:f==='ERROR'?'#C62828':'#FF8F00') : '#E0E0E0'}`,
              background: filter===f ? (f==='TRANSMITTED'?'#E8F5E9':f==='ERROR'?'#FFEBEE':'#FFF8E1') : 'white',
              color: filter===f ? (f==='TRANSMITTED'?G:f==='ERROR'?'#C62828':f==='PENDING'?'#FF8F00':'#333') : '#666',
            }}>{f === 'ALL' ? 'All Invoices' : f}</button>
          ))}
        </div>
        {errors > 0 && (
          <Btn icon={<RefreshCw size={13}/>} onClick={retryFailed}>
            Retry {errors} Failed
          </Btn>
        )}
      </div>

      {/* Invoice list */}
      <div style={{ display:'flex', flexDirection:'column', gap:8 }}>
        {loading && <div style={{ padding:30, textAlign:'center', color:'#888' }}>Loading...</div>}
        {!loading && filtered.length === 0 && <div style={{ padding:30, textAlign:'center', color:'#888' }}>No invoices yet</div>}
        {filtered.map(inv => {
          const s = ETIMS_STATUS_STYLE[inv.status] || { bg:'#F5F5F5', color:'#888', label: inv.status }
          return (
            <Card key={inv.id} style={{ padding:16 }}>
              <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center' }}>
                <div style={{ display:'flex', alignItems:'center', gap:14 }}>
                  <div style={{ width:8, height:8, borderRadius:'50%', background:s.color, flexShrink:0 }} />
                  <div>
                    <div style={{ fontWeight:700, fontSize:13 }}>{inv.invoiceNumber}</div>
                    <div style={{ fontSize:11, color:'#888', marginTop:2 }}>
                      {inv.etimsInvoiceNumber ? `KRA: ${inv.etimsInvoiceNumber}` : 'Awaiting KRA number'}
                      {inv.submittedAt && ` · ${new Date(inv.submittedAt).toLocaleString('en-KE',{dateStyle:'short',timeStyle:'short'})}`}
                    </div>
                  </div>
                </div>
                <div style={{ display:'flex', alignItems:'center', gap:16 }}>
                  <div style={{ textAlign:'right' }}>
                    <div style={{ fontSize:11, color:'#999' }}>VAT</div>
                    <div style={{ fontWeight:700, color:G }}>KES {inv.taxAmount.toLocaleString()}</div>
                  </div>
                  <div style={{ textAlign:'right' }}>
                    <div style={{ fontSize:11, color:'#999' }}>Total</div>
                    <div style={{ fontWeight:800, fontSize:15 }}>KES {inv.totalAmount.toLocaleString()}</div>
                  </div>
                  {pill(s.bg, s.color, s.label)}
                  {inv.qrCodeUrl && (
                    <a href={inv.qrCodeUrl} target="_blank" rel="noreferrer" title="Verify on KRA eTIMS">
                      <ExternalLink size={15} color="#999" />
                    </a>
                  )}
                </div>
              </div>
            </Card>
          )
        })}
      </div>
    </div>
  )
}

// ── Tab: Tax Returns ──────────────────────────────────────────────────────────
function ReturnsTab() {
  const [returns, setReturns]     = useState<TaxReturnResponse[]>([])
  const [genType, setGenType]     = useState<'VAT3'|'TOT'|'WHT'>('VAT3')
  const [genMonth, setGenMonth]   = useState(new Date().getMonth() + 1)
  const [genYear, setGenYear]     = useState(new Date().getFullYear())
  const [ackInput, setAckInput]   = useState<Record<string, string>>({})
  const [loading, setLoading]     = useState(true)

  useEffect(() => {
    kraApi.getReturns().then(res => {
      if (res.success && res.data) setReturns(res.data)
    }).finally(() => setLoading(false))
  }, [])

  const months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec']

  async function generate() {
    const label = `${months[genMonth-1]} ${genYear}`
    const existing = returns.find(r => r.returnType === genType && r.periodLabel === label)
    if (existing) {
      alert(`A ${genType} return for ${label} already exists.`)
      return
    }
    const startDate = `${genYear}-${String(genMonth).padStart(2,'0')}-01`
    const lastDay = new Date(genYear, genMonth, 0).getDate()
    const endDate = `${genYear}-${String(genMonth).padStart(2,'0')}-${String(lastDay).padStart(2,'0')}`
    try {
      const fn = genType === 'VAT3' ? kraApi.generateVat3 : genType === 'TOT' ? kraApi.generateTot : kraApi.generateWht
      const res = await fn({ periodStart: startDate, periodEnd: endDate })
      if (res.success && res.data) setReturns(r => [...r, res.data!])
    } catch (_) {}
  }

  async function markSubmitted(id: string) {
    const ack = ackInput[id]
    if (!ack) return
    try {
      const res = await kraApi.markReturnSubmitted(id)
      if (res.success && res.data) {
        setReturns(r => r.map(x => x.id === id ? res.data! : x))
      }
    } catch (_) {}
  }

  function getTaxAmount(r: TaxReturnResponse) {
    if (r.netVatPayable != null) return r.netVatPayable
    if (r.totAmount != null) return r.totAmount
    if (r.whtAmount != null) return r.whtAmount
    return 0
  }

  const typeColor: Record<string,string> = { VAT3:'#1B8B34', TOT:'#1565C0', WHT:'#6A1B9A' }
  const typeBg:    Record<string,string> = { VAT3:'#E8F5E9', TOT:'#E3F2FD', WHT:'#F3E5F5' }

  return (
    <div style={{ display:'flex', flexDirection:'column', gap:16 }}>
      {/* Generate control */}
      <Card style={{ padding:20 }}>
        <div style={{ fontWeight:700, fontSize:14, marginBottom:14 }}>Generate Return</div>
        <div style={{ display:'flex', gap:12, alignItems:'flex-end', flexWrap:'wrap' }}>
          <div>
            <label style={{ fontSize:11, fontWeight:600, display:'block', marginBottom:5, color:'#666' }}>Return Type</label>
            <select value={genType} onChange={e => setGenType(e.target.value as any)}
              style={{ padding:'9px 12px', borderRadius:8, border:'1px solid #E0E0E0', fontSize:13, fontWeight:600 }}>
              <option value="VAT3">VAT3 (16% VAT)</option>
              <option value="TOT">TOT (1.5% Turnover)</option>
              <option value="WHT">WHT (3% Withholding)</option>
            </select>
          </div>
          <div>
            <label style={{ fontSize:11, fontWeight:600, display:'block', marginBottom:5, color:'#666' }}>Month</label>
            <select value={genMonth} onChange={e => setGenMonth(Number(e.target.value))}
              style={{ padding:'9px 12px', borderRadius:8, border:'1px solid #E0E0E0', fontSize:13 }}>
              {months.map((m,i) => <option key={m} value={i+1}>{m}</option>)}
            </select>
          </div>
          <div>
            <label style={{ fontSize:11, fontWeight:600, display:'block', marginBottom:5, color:'#666' }}>Year</label>
            <select value={genYear} onChange={e => setGenYear(Number(e.target.value))}
              style={{ padding:'9px 12px', borderRadius:8, border:'1px solid #E0E0E0', fontSize:13 }}>
              {[2024,2025,2026].map(y => <option key={y}>{y}</option>)}
            </select>
          </div>
          <Btn icon={<FileText size={14}/>} onClick={generate}>Generate</Btn>
        </div>
      </Card>

      {/* Returns list */}
      <div style={{ display:'flex', flexDirection:'column', gap:10 }}>
        {loading && <div style={{ padding:30, textAlign:'center', color:'#888' }}>Loading...</div>}
        {!loading && returns.length === 0 && <div style={{ padding:30, textAlign:'center', color:'#888' }}>No returns yet</div>}
        {returns.map(r => {
          const s  = RETURN_STATUS_STYLE[r.status] || { bg:'#F5F5F5', color:'#757575' }
          const tc = typeColor[r.returnType]
          const tb = typeBg[r.returnType]
          const amt = getTaxAmount(r)
          return (
            <Card key={r.id} style={{ padding:18 }}>
              <div style={{ display:'flex', justifyContent:'space-between', alignItems:'flex-start' }}>
                <div style={{ display:'flex', gap:12, alignItems:'center' }}>
                  <span style={{ background:tb, color:tc, padding:'3px 10px', borderRadius:6, fontSize:11, fontWeight:700 }}>{r.returnType}</span>
                  <div>
                    <div style={{ fontWeight:700, fontSize:14 }}>{r.periodLabel}</div>
                    <div style={{ fontSize:11, color:'#888', marginTop:2 }}>Due {r.dueDate}</div>
                  </div>
                </div>
                <div style={{ display:'flex', alignItems:'center', gap:16 }}>
                  <div style={{ textAlign:'right' }}>
                    <div style={{ fontSize:11, color:'#999' }}>Tax Payable</div>
                    <div style={{ fontWeight:800, fontSize:16, color:tc }}>KES {amt.toLocaleString()}</div>
                  </div>
                  {pill(s.bg, s.color, r.status)}
                </div>
              </div>

              {r.iTaxAcknowledgementNo && (
                <div style={{ marginTop:10, fontSize:12, color:'#666', background:'#F5F5F5', padding:'6px 12px', borderRadius:6 }}>
                  iTax Ack: <strong>{r.iTaxAcknowledgementNo}</strong>
                </div>
              )}

              {/* Actions */}
              <div style={{ marginTop:12, display:'flex', gap:10, alignItems:'center', flexWrap:'wrap' }}>
                {r.csvDownloadReady && (
                  <Btn icon={<Download size={13}/>}>
                    Download CSV
                  </Btn>
                )}
                <a href="https://itax.kra.go.ke" target="_blank" rel="noreferrer" style={{ textDecoration:'none' }}>
                  <Btn icon={<ExternalLink size={13}/>}>Upload on iTax</Btn>
                </a>
                {(r.status === 'GENERATED' || r.status === 'SUBMITTED') && !r.iTaxAcknowledgementNo && (
                  <div style={{ display:'flex', gap:8, alignItems:'center', flex:1 }}>
                    <input
                      value={ackInput[r.id] ?? ''}
                      onChange={e => setAckInput(a => ({...a, [r.id]: e.target.value}))}
                      placeholder="Paste iTax acknowledgement number…"
                      style={{ flex:1, padding:'7px 10px', borderRadius:7, border:'1px solid #E0E0E0', fontSize:12, minWidth:220 }}
                    />
                    <Btn icon={<CheckCircle size={13}/>} onClick={() => markSubmitted(r.id)}>Confirm</Btn>
                  </div>
                )}
              </div>
            </Card>
          )
        })}
      </div>
    </div>
  )
}

// ── Tab: Setup ────────────────────────────────────────────────────────────────
function SetupTab() {
  const [pin, setPin]           = useState('P051234567X')
  const [company, setCompany]   = useState('Biashara360ERP Limited')
  const [vatNo, setVatNo]       = useState('')
  const [sdcId, setSdcId]       = useState('SDCK2024001')
  const [serialNo, setSerial]   = useState('VSCU123456')
  const [env, setEnv]           = useState<'sandbox'|'production'>('sandbox')
  const [saved, setSaved]       = useState(false)

  function save() { setSaved(true); setTimeout(() => setSaved(false), 2500) }

  return (
    <div style={{ display:'flex', flexDirection:'column', gap:16 }}>

      {/* Environment warning */}
      {env === 'production' && (
        <div style={{ background:'#FFF3E0', border:'1px solid #FFB300', borderRadius:10, padding:'12px 16px', display:'flex', gap:10, alignItems:'flex-start' }}>
          <AlertTriangle size={18} color='#FF8F00' style={{ flexShrink:0, marginTop:1 }} />
          <div style={{ fontSize:13 }}>
            <strong>Production mode:</strong> All invoices will be transmitted to KRA's live eTIMS system and are legally binding.
            Only switch to production when you have a verified KRA eTIMS approval.
          </div>
        </div>
      )}

      {/* KRA Profile */}
      <Card style={{ padding:22 }}>
        <div style={{ fontWeight:700, fontSize:15, marginBottom:16, display:'flex', alignItems:'center', gap:8 }}>
          <Shield size={16} color={G} /> KRA Taxpayer Profile
        </div>
        <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:14 }}>
          <div>
            <label style={{ fontSize:12, fontWeight:600, display:'block', marginBottom:5 }}>KRA PIN *</label>
            <input value={pin} onChange={e => setPin(e.target.value.toUpperCase())}
              placeholder="P051234567X"
              style={{ width:'100%', padding:'9px 12px', borderRadius:8, border:'1px solid #E0E0E0', fontSize:13, fontFamily:'monospace', fontWeight:700 }} />
            <div style={{ fontSize:11, color:'#888', marginTop:4 }}>Format: letter + 9 digits + letter</div>
          </div>
          <div>
            <label style={{ fontSize:12, fontWeight:600, display:'block', marginBottom:5 }}>Company Name *</label>
            <input value={company} onChange={e => setCompany(e.target.value)}
              style={{ width:'100%', padding:'9px 12px', borderRadius:8, border:'1px solid #E0E0E0', fontSize:13 }} />
          </div>
          <div>
            <label style={{ fontSize:12, fontWeight:600, display:'block', marginBottom:5 }}>VAT Registration Number</label>
            <input value={vatNo} onChange={e => setVatNo(e.target.value)}
              placeholder="Same as PIN if registered"
              style={{ width:'100%', padding:'9px 12px', borderRadius:8, border:'1px solid #E0E0E0', fontSize:13 }} />
          </div>
          <div>
            <label style={{ fontSize:12, fontWeight:600, display:'block', marginBottom:5 }}>Environment</label>
            <select value={env} onChange={e => setEnv(e.target.value as any)}
              style={{ width:'100%', padding:'9px 12px', borderRadius:8, border:'1px solid #E0E0E0', fontSize:13 }}>
              <option value="sandbox">Sandbox (Testing)</option>
              <option value="production">Production (Live KRA)</option>
            </select>
          </div>
        </div>
      </Card>

      {/* eTIMS Device */}
      <Card style={{ padding:22 }}>
        <div style={{ fontWeight:700, fontSize:15, marginBottom:6, display:'flex', alignItems:'center', gap:8 }}>
          <Wifi size={16} color="#1565C0" /> eTIMS Virtual Device (SDC)
        </div>
        <div style={{ fontSize:12, color:'#666', marginBottom:16, lineHeight:1.6 }}>
          A Software Device Controller (SDC) is your virtual eTIMS device issued by KRA.
          Register at <a href="https://etims.kra.go.ke" target="_blank" rel="noreferrer" style={{ color:'#1565C0' }}>etims.kra.go.ke</a> to get your SDC ID and serial number.
        </div>
        <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:14 }}>
          <div>
            <label style={{ fontSize:12, fontWeight:600, display:'block', marginBottom:5 }}>SDC ID</label>
            <input value={sdcId} onChange={e => setSdcId(e.target.value)}
              placeholder="From KRA eTIMS portal"
              style={{ width:'100%', padding:'9px 12px', borderRadius:8, border:'1px solid #E0E0E0', fontSize:13, fontFamily:'monospace' }} />
          </div>
          <div>
            <label style={{ fontSize:12, fontWeight:600, display:'block', marginBottom:5 }}>Device Serial Number</label>
            <input value={serialNo} onChange={e => setSerial(e.target.value)}
              placeholder="VSCU assigned by KRA"
              style={{ width:'100%', padding:'9px 12px', borderRadius:8, border:'1px solid #E0E0E0', fontSize:13, fontFamily:'monospace' }} />
          </div>
        </div>
        <div style={{ marginTop:14 }}>
          <Btn icon={<Zap size={13}/>}>Initialise Device with KRA</Btn>
        </div>
      </Card>

      {/* Registration guide */}
      <Card style={{ padding:22, background:'#F8F9FF', border:'1px solid #C5CAE9' }}>
        <div style={{ fontWeight:700, fontSize:14, color:'#1565C0', marginBottom:14, display:'flex', alignItems:'center', gap:8 }}>
          <Info size={16}/> How to Register for eTIMS
        </div>
        {[
          ['Step 1', 'Go to etims.kra.go.ke and log in with your iTax credentials'],
          ['Step 2', 'Navigate to Device Management → Register New VSCU'],
          ['Step 3', 'Select "Virtual Device" and follow the registration wizard'],
          ['Step 4', 'KRA will issue your SDC ID and serial number via email (1–3 days)'],
          ['Step 5', 'Enter the SDC ID and serial number above and click "Initialise Device"'],
          ['Step 6', 'Once initialised, all new invoices will be transmitted automatically'],
        ].map(([step, desc]) => (
          <div key={step} style={{ display:'flex', gap:12, marginBottom:10, fontSize:13 }}>
            <span style={{ fontWeight:700, color:'#1565C0', minWidth:52, flexShrink:0 }}>{step}</span>
            <span style={{ color:'#333' }}>{desc}</span>
          </div>
        ))}
      </Card>

      <div style={{ display:'flex', gap:10 }}>
        <Btn icon={<CheckCircle size={14}/>} onClick={save}>
          {saved ? '✅ Saved!' : 'Save KRA Profile'}
        </Btn>
        <a href="https://itax.kra.go.ke" target="_blank" rel="noreferrer" style={{ textDecoration:'none' }}>
          <Btn icon={<ExternalLink size={13}/>}>Open iTax Portal</Btn>
        </a>
      </div>
    </div>
  )
}

// ── Main KRA Page ─────────────────────────────────────────────────────────────
export default function KraPage() {
  const [tab, setTab] = useState<'compliance'|'etims'|'returns'|'setup'>('compliance')

  const tabs = [
    { key:'compliance', label:'Compliance', icon:<Shield size={14}/> },
    { key:'etims',      label:'eTIMS Invoices', icon:<Wifi size={14}/> },
    { key:'returns',    label:'Tax Returns', icon:<FileText size={14}/> },
    { key:'setup',      label:'Setup', icon:<Settings size={14}/> },
  ]

  return (
    <div className="fade-in" style={{ display:'flex', flexDirection:'column', gap:20 }}>
      <PageHeader title="KRA iTax Integration"
        action={
          <div style={{ display:'flex', gap:8, alignItems:'center' }}>
            <span style={{ fontSize:11, background:'#E8F5E9', color:G, padding:'4px 10px', borderRadius:20, fontWeight:700 }}>
              eTIMS Active
            </span>
            <a href="https://itax.kra.go.ke" target="_blank" rel="noreferrer" style={{ textDecoration:'none' }}>
              <Btn icon={<ExternalLink size={13}/>}>iTax Portal</Btn>
            </a>
          </div>
        }
      />

      {/* Tab bar */}
      <div style={{ display:'flex', gap:2, borderBottom:'2px solid #E0E0E0' }}>
        {tabs.map(t => (
          <button key={t.key} onClick={() => setTab(t.key as any)} style={{
            display:'flex', alignItems:'center', gap:6,
            padding:'10px 20px', border:'none', background:'none', cursor:'pointer',
            fontWeight: tab===t.key ? 700 : 500, fontSize:13,
            color: tab===t.key ? G : '#666',
            borderBottom: tab===t.key ? `2px solid ${G}` : '2px solid transparent',
            marginBottom:'-2px'
          }}>
            {t.icon}{t.label}
          </button>
        ))}
      </div>

      {tab === 'compliance' && <ComplianceTab />}
      {tab === 'etims'      && <EtimsTab />}
      {tab === 'returns'    && <ReturnsTab />}
      {tab === 'setup'      && <SetupTab />}
    </div>
  )
}
