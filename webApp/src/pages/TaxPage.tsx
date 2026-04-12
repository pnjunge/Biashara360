import React, { useState, useEffect } from 'react'
import { PieChart, Pie, Cell, BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts'
import { Plus, Settings, FileText, CheckCircle, Clock, AlertTriangle, Percent, TrendingUp, DollarSign, RefreshCw } from 'lucide-react'
import { PageHeader, Card, Btn, DataTable, StatusBadge, KpiCard } from '../components/ui'
import { taxApi, TaxRateResponse, TaxRemittanceResponse } from '../services/api'

// ── Shared constants ──────────────────────────────────────────────────────────
const TYPE_COLORS: Record<string, string> = {
  VAT:'#1B8B34', TOT:'#1565C0', WHT:'#6A1B9A', EXCISE:'#E65100', CUSTOM:'#37474F'
}
const TYPE_BG: Record<string, string> = {
  VAT:'#E8F5E9', TOT:'#E3F2FD', WHT:'#F3E5F5', EXCISE:'#FFF3E0', CUSTOM:'#ECEFF1'
}

function TaxBadge({ type }: { type: string }) {
  return (
    <span style={{
      background: TYPE_BG[type] || '#ECEFF1',
      color: TYPE_COLORS[type] || '#37474F',
      borderRadius: 6, padding: '2px 10px', fontSize: 11, fontWeight: 700, letterSpacing: 0.5
    }}>{type}</span>
  )
}

// ── Tax Rates Tab ─────────────────────────────────────────────────────────────
function TaxRatesTab() {
  const [showAdd, setShowAdd] = useState(false)
  const [rates, setRates] = useState<TaxRateResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [form, setForm] = useState({ taxType:'VAT', name:'', rate:'', isInclusive:false, appliesTo:'ALL', description:'' })

  useEffect(() => {
    taxApi.getRates().then(res => {
      if (res.success && res.data) setRates(res.data)
    }).finally(() => setLoading(false))
  }, [])

  async function toggleRate(id: string) {
    try {
      const res = await taxApi.toggleRate(id)
      if (res.success && res.data) {
        setRates(r => r.map(x => x.id === id ? res.data! : x))
      } else {
        setRates(r => r.map(x => x.id === id ? { ...x, isActive: !x.isActive } : x))
      }
    } catch (_) {
      setRates(r => r.map(x => x.id === id ? { ...x, isActive: !x.isActive } : x))
    }
  }

  async function seedDefaults() {
    await taxApi.seedDefaults()
    const res = await taxApi.getRates()
    if (res.success && res.data) setRates(res.data)
  }

  async function addRate() {
    if (!form.name || !form.rate) return
    try {
      const res = await taxApi.createRate({
        taxType: form.taxType,
        name: form.name,
        rate: parseFloat(form.rate) / 100,
        isInclusive: form.isInclusive,
        appliesTo: form.appliesTo,
        description: form.description,
      })
      if (res.success && res.data) {
        setRates(r => [...r, res.data!])
        setShowAdd(false)
        setForm({ taxType:'VAT', name:'', rate:'', isInclusive:false, appliesTo:'ALL', description:'' })
      }
    } catch (_) {}
  }

  return (
    <div style={{ display:'flex', flexDirection:'column', gap:20 }}>
      {/* Kenya Quick-seed banner */}
      <div style={{ background:'#E8F5E9', border:'1px solid #1B8B34', borderRadius:10, padding:'14px 18px', display:'flex', alignItems:'center', justifyContent:'space-between' }}>
        <div style={{ display:'flex', alignItems:'center', gap:10 }}>
          <TrendingUp size={18} color='#1B8B34' />
          <div>
            <div style={{ fontWeight:700, color:'#1B8B34', fontSize:13 }}>Kenya Tax Defaults</div>
            <div style={{ fontSize:12, color:'#388E3C' }}>VAT 16% · TOT 1.5% · WHT 3% · Excise 20% — based on KRA guidelines</div>
          </div>
        </div>
        <Btn icon={<RefreshCw size={13}/>} onClick={seedDefaults}>Seed Defaults</Btn>
      </div>

      {/* Add form */}
      {showAdd && (
        <Card style={{ padding:20, border:'2px solid #1B8B34' }}>
          <h4 style={{ fontWeight:700, marginBottom:16 }}>Add Tax Rate</h4>
          <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr 1fr', gap:12, marginBottom:12 }}>
            <div>
              <label style={{ fontSize:12, fontWeight:600, display:'block', marginBottom:5 }}>Tax Type</label>
              <select value={form.taxType} onChange={e => setForm(f => ({...f, taxType:e.target.value}))}
                style={{ width:'100%', padding:'8px 10px', borderRadius:7, border:'1px solid #E0E0E0', fontSize:13 }}>
                {['VAT','TOT','WHT','EXCISE','CUSTOM'].map(t => <option key={t}>{t}</option>)}
              </select>
            </div>
            <div>
              <label style={{ fontSize:12, fontWeight:600, display:'block', marginBottom:5 }}>Display Name</label>
              <input value={form.name} onChange={e => setForm(f => ({...f, name:e.target.value}))}
                placeholder="e.g. Value Added Tax"
                style={{ width:'100%', padding:'8px 10px', borderRadius:7, border:'1px solid #E0E0E0', fontSize:13 }} />
            </div>
            <div>
              <label style={{ fontSize:12, fontWeight:600, display:'block', marginBottom:5 }}>Rate (%)</label>
              <input type="number" value={form.rate} onChange={e => setForm(f => ({...f, rate:e.target.value}))}
                placeholder="e.g. 16"
                style={{ width:'100%', padding:'8px 10px', borderRadius:7, border:'1px solid #E0E0E0', fontSize:13 }} />
            </div>
          </div>
          <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:12, marginBottom:12 }}>
            <div>
              <label style={{ fontSize:12, fontWeight:600, display:'block', marginBottom:5 }}>Applies To</label>
              <select value={form.appliesTo} onChange={e => setForm(f => ({...f, appliesTo:e.target.value}))}
                style={{ width:'100%', padding:'8px 10px', borderRadius:7, border:'1px solid #E0E0E0', fontSize:13 }}>
                <option>ALL</option><option>PRODUCTS</option><option>SERVICES</option>
              </select>
            </div>
            <div style={{ display:'flex', alignItems:'center', gap:10, paddingTop:24 }}>
              <input type="checkbox" id="inclusive" checked={form.isInclusive}
                onChange={e => setForm(f => ({...f, isInclusive:e.target.checked}))} />
              <label htmlFor="inclusive" style={{ fontSize:13, fontWeight:500 }}>Tax is inclusive in price</label>
            </div>
          </div>
          <div style={{ marginBottom:16 }}>
            <label style={{ fontSize:12, fontWeight:600, display:'block', marginBottom:5 }}>Description</label>
            <input value={form.description} onChange={e => setForm(f => ({...f, description:e.target.value}))}
              placeholder="Optional description"
              style={{ width:'100%', padding:'8px 10px', borderRadius:7, border:'1px solid #E0E0E0', fontSize:13 }} />
          </div>
          <div style={{ display:'flex', gap:10 }}>
            <Btn icon={<Plus size={13}/>} onClick={addRate}>Save Rate</Btn>
            <Btn onClick={() => setShowAdd(false)}>Cancel</Btn>
          </div>
        </Card>
      )}

      {/* Rate cards */}
      {loading ? (
        <div style={{ padding:30, textAlign:'center', color:'#888' }}>Loading...</div>
      ) : rates.length === 0 ? (
        <div style={{ padding:30, textAlign:'center', color:'#888' }}>No tax rates configured. Click "Seed Defaults" to add Kenya standard rates.</div>
      ) : (
      <div style={{ display:'grid', gridTemplateColumns:'repeat(2, 1fr)', gap:14 }}>
        {rates.map(r => (
          <Card key={r.id} style={{ padding:20, borderLeft:`4px solid ${TYPE_COLORS[r.taxType] || '#9E9E9E'}`, opacity: r.isActive ? 1 : 0.6 }}>
            <div style={{ display:'flex', justifyContent:'space-between', alignItems:'flex-start', marginBottom:12 }}>
              <div style={{ display:'flex', alignItems:'center', gap:10 }}>
                <TaxBadge type={r.taxType} />
                <span style={{ fontWeight:700, fontSize:15 }}>{r.name}</span>
              </div>
              <div style={{ display:'flex', alignItems:'center', gap:8 }}>
                <span style={{ fontSize:22, fontWeight:800, color: TYPE_COLORS[r.taxType] }}>{r.ratePercent}%</span>
                <button onClick={() => toggleRate(r.id)} style={{
                  background: r.isActive ? '#E8F5E9' : '#F5F5F5',
                  color: r.isActive ? '#1B8B34' : '#9E9E9E',
                  border:'none', borderRadius:20, padding:'4px 12px', fontSize:11, fontWeight:700, cursor:'pointer'
                }}>{r.isActive ? 'Active' : 'Inactive'}</button>
              </div>
            </div>
            <div style={{ fontSize:12, color:'#666', marginBottom:10 }}>{r.description}</div>
            <div style={{ display:'flex', gap:8, fontSize:11 }}>
              <span style={{ background:'#F5F5F5', padding:'2px 8px', borderRadius:5, color:'#555' }}>
                Applies to: {r.appliesTo}
              </span>
              <span style={{ background:'#F5F5F5', padding:'2px 8px', borderRadius:5, color:'#555' }}>
                {r.isInclusive ? 'Tax Inclusive' : 'Tax Exclusive'}
              </span>
            </div>
          </Card>
        ))}
      </div>
      )}

      <Btn icon={<Plus size={14}/>} onClick={() => setShowAdd(v => !v)}>Add Custom Tax Rate</Btn>
    </div>
  )
}

// ── Tax Calculator Tab ────────────────────────────────────────────────────────
function TaxCalculatorTab() {
  const [amount, setAmount] = useState('10000')
  const [selected, setSelected] = useState<string[]>([])
  const [inclusive, setInclusive] = useState(false)
  const [rates, setRates] = useState<TaxRateResponse[]>([])
  const [ratesLoading, setRatesLoading] = useState(true)

  useEffect(() => {
    taxApi.getRates().then(res => {
      if (res.success && res.data) {
        setRates(res.data)
        const firstActive = res.data.find(r => r.isActive)
        if (firstActive) setSelected([firstActive.id])
      }
    }).finally(() => setRatesLoading(false))
  }, [])

  const activeRates = rates.filter(r => r.isActive)
  const numAmount = parseFloat(amount) || 0

  const lines = rates
    .filter(r => selected.includes(r.id))
    .map(r => {
      const taxAmt = inclusive
        ? numAmount - numAmount / (1 + r.rate)
        : numAmount * r.rate
      return { ...r, taxAmount: Math.round(taxAmt * 100) / 100 }
    })

  const totalTax   = lines.reduce((s, l) => s + l.taxAmount, 0)
  const grandTotal = inclusive ? numAmount : numAmount + totalTax

  function toggle(id: string) {
    setSelected(sel => sel.includes(id) ? sel.filter(x => x !== id) : [...sel, id])
  }

  return (
    <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:20 }}>
      <Card style={{ padding:24 }}>
        <h3 style={{ fontWeight:700, marginBottom:18, display:'flex', alignItems:'center', gap:8 }}>
          <Percent size={18} color='#1B8B34' /> Tax Calculator
        </h3>
        <div style={{ marginBottom:16 }}>
          <label style={{ fontSize:12, fontWeight:600, display:'block', marginBottom:6 }}>Amount (KES)</label>
          <input type="number" value={amount} onChange={e => setAmount(e.target.value)}
            style={{ width:'100%', padding:'10px 14px', borderRadius:8, border:'1px solid #E0E0E0', fontSize:16, fontWeight:700 }} />
        </div>
        <div style={{ marginBottom:16 }}>
          <label style={{ fontSize:12, fontWeight:600, display:'block', marginBottom:8 }}>Apply Tax Rates</label>
          {ratesLoading && <div style={{ fontSize:13, color:'#888' }}>Loading tax rates...</div>}
          {!ratesLoading && activeRates.length === 0 && <div style={{ fontSize:13, color:'#888' }}>No active rates. Seed defaults in the Tax Rates tab.</div>}
          {activeRates.map(r => (
            <div key={r.id} onClick={() => toggle(r.id)} style={{
              display:'flex', justifyContent:'space-between', alignItems:'center',
              padding:'10px 14px', borderRadius:8, border:`1px solid ${selected.includes(r.id) ? TYPE_COLORS[r.taxType] : '#E0E0E0'}`,
              background: selected.includes(r.id) ? TYPE_BG[r.taxType] : 'white',
              cursor:'pointer', marginBottom:8
            }}>
              <div style={{ display:'flex', alignItems:'center', gap:10 }}>
                <input type="checkbox" readOnly checked={selected.includes(r.id)} />
                <TaxBadge type={r.taxType} />
                <span style={{ fontSize:13 }}>{r.name}</span>
              </div>
              <span style={{ fontWeight:700, color: TYPE_COLORS[r.taxType] }}>{r.ratePercent}%</span>
            </div>
          ))}
        </div>
        <div style={{ display:'flex', alignItems:'center', gap:10 }}>
          <input type="checkbox" id="calcInc" checked={inclusive} onChange={e => setInclusive(e.target.checked)} />
          <label htmlFor="calcInc" style={{ fontSize:13 }}>Amount is tax-inclusive</label>
        </div>
      </Card>

      <Card style={{ padding:24 }}>
        <h3 style={{ fontWeight:700, marginBottom:18 }}>Breakdown</h3>
        <div style={{ background:'#F8FFF9', borderRadius:10, padding:16, marginBottom:16 }}>
          <div style={{ display:'flex', justifyContent:'space-between', marginBottom:10 }}>
            <span style={{ fontSize:13, color:'#666' }}>Subtotal</span>
            <span style={{ fontWeight:600 }}>KES {numAmount.toLocaleString()}</span>
          </div>
          {lines.map(l => (
            <div key={l.id} style={{ display:'flex', justifyContent:'space-between', marginBottom:8 }}>
              <span style={{ fontSize:13, color: TYPE_COLORS[l.taxType] }}>{l.name} ({l.ratePercent}%)</span>
              <span style={{ fontWeight:600, color: TYPE_COLORS[l.taxType] }}>+ KES {l.taxAmount.toLocaleString()}</span>
            </div>
          ))}
          <div style={{ borderTop:'2px solid #E0E0E0', paddingTop:10, marginTop:6, display:'flex', justifyContent:'space-between' }}>
            <span style={{ fontWeight:700, fontSize:15 }}>Total Tax</span>
            <span style={{ fontWeight:800, fontSize:15, color:'#1B8B34' }}>KES {Math.round(totalTax).toLocaleString()}</span>
          </div>
        </div>
        <div style={{ background:'#1B8B34', borderRadius:10, padding:16, color:'white', display:'flex', justifyContent:'space-between', alignItems:'center' }}>
          <span style={{ fontWeight:700, fontSize:16 }}>Grand Total</span>
          <span style={{ fontWeight:900, fontSize:24 }}>KES {Math.round(grandTotal).toLocaleString()}</span>
        </div>
        {lines.length > 0 && (
          <div style={{ marginTop:16 }}>
            <ResponsiveContainer width="100%" height={140}>
              <PieChart>
                <Pie data={[
                  { name:'Net Amount', value: numAmount - (inclusive ? totalTax : 0) },
                  ...lines.map(l => ({ name:l.taxType, value:l.taxAmount }))
                ]} dataKey="value" cx="50%" cy="50%" outerRadius={60} label={({name, percent}) => `${name} ${(percent*100).toFixed(0)}%`}>
                  <Cell fill="#E8F5E9" />
                  {lines.map((l, i) => <Cell key={i} fill={TYPE_COLORS[l.taxType]} />)}
                </Pie>
                <Tooltip formatter={(v: number) => `KES ${v.toLocaleString()}`} />
              </PieChart>
            </ResponsiveContainer>
          </div>
        )}
      </Card>
    </div>
  )
}

// ── Remittances Tab ───────────────────────────────────────────────────────────
function RemittancesTab() {
  const [showForm, setShowForm] = useState(false)
  const [filterType, setFilterType] = useState('ALL')
  const [remittances, setRemittances] = useState<TaxRemittanceResponse[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    taxApi.getRemittances().then(res => {
      if (res.success && res.data) setRemittances(res.data)
    }).finally(() => setLoading(false))
  }, [])

  const filtered = remittances.filter(r => filterType === 'ALL' || r.taxType === filterType)

  const statusIcon = (s: string) => {
    if (s === 'PAID')    return <CheckCircle size={14} color='#1B8B34' />
    if (s === 'FILED')   return <FileText size={14} color='#1565C0' />
    return <Clock size={14} color='#FF8F00' />
  }

  return (
    <div style={{ display:'flex', flexDirection:'column', gap:16 }}>
      <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center' }}>
        <div style={{ display:'flex', gap:8 }}>
          {['ALL','VAT','TOT','WHT','EXCISE'].map(t => (
            <button key={t} onClick={() => setFilterType(t)} style={{
              padding:'6px 14px', borderRadius:20, fontSize:12, fontWeight:600,
              border:`1px solid ${filterType === t ? TYPE_COLORS[t] || '#1B8B34' : '#E0E0E0'}`,
              background: filterType === t ? TYPE_BG[t] || '#E8F5E9' : 'white',
              color: filterType === t ? TYPE_COLORS[t] || '#1B8B34' : '#666', cursor:'pointer'
            }}>{t}</button>
          ))}
        </div>
        <Btn icon={<Plus size={13}/>} onClick={() => setShowForm(v => !v)}>File Period</Btn>
      </div>

      {showForm && (
        <Card style={{ padding:20, border:'2px solid #1B8B34' }}>
          <h4 style={{ fontWeight:700, marginBottom:16 }}>Create Remittance Record</h4>
          <div style={{ display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:12, marginBottom:14 }}>
            <div>
              <label style={{ fontSize:12, fontWeight:600, display:'block', marginBottom:5 }}>Tax Type</label>
              <select style={{ width:'100%', padding:'8px 10px', borderRadius:7, border:'1px solid #E0E0E0', fontSize:13 }}>
                {['VAT','TOT','WHT','EXCISE'].map(t => <option key={t}>{t}</option>)}
              </select>
            </div>
            <div>
              <label style={{ fontSize:12, fontWeight:600, display:'block', marginBottom:5 }}>Period Start</label>
              <input type="date" style={{ width:'100%', padding:'8px 10px', borderRadius:7, border:'1px solid #E0E0E0', fontSize:13 }} />
            </div>
            <div>
              <label style={{ fontSize:12, fontWeight:600, display:'block', marginBottom:5 }}>Period End</label>
              <input type="date" style={{ width:'100%', padding:'8px 10px', borderRadius:7, border:'1px solid #E0E0E0', fontSize:13 }} />
            </div>
            <div>
              <label style={{ fontSize:12, fontWeight:600, display:'block', marginBottom:5 }}>KRA Receipt #</label>
              <input placeholder="Optional" style={{ width:'100%', padding:'8px 10px', borderRadius:7, border:'1px solid #E0E0E0', fontSize:13 }} />
            </div>
          </div>
          <Btn icon={<Plus size={13}/>}>Save Remittance</Btn>
        </Card>
      )}

      <div style={{ display:'flex', flexDirection:'column', gap:10 }}>
        {loading && <div style={{ padding:30, textAlign:'center', color:'#888' }}>Loading...</div>}
        {!loading && filtered.length === 0 && <div style={{ padding:30, textAlign:'center', color:'#888' }}>No remittances yet</div>}
        {filtered.map(r => (
          <Card key={r.id} style={{ padding:18 }}>
            <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center' }}>
              <div style={{ display:'flex', alignItems:'center', gap:14 }}>
                {statusIcon(r.status)}
                <TaxBadge type={r.taxType} />
                <div>
                  <div style={{ fontWeight:700, fontSize:14 }}>
                    {r.periodStart} → {r.periodEnd}
                  </div>
                  {r.receiptNumber && (
                    <div style={{ fontSize:11, color:'#666' }}>KRA Receipt: {r.receiptNumber}</div>
                  )}
                </div>
              </div>
              <div style={{ display:'flex', alignItems:'center', gap:24 }}>
                <div style={{ textAlign:'right' }}>
                  <div style={{ fontSize:11, color:'#999' }}>Taxable Amount</div>
                  <div style={{ fontWeight:700 }}>KES {r.taxableAmount.toLocaleString()}</div>
                </div>
                <div style={{ textAlign:'right' }}>
                  <div style={{ fontSize:11, color:'#999' }}>Tax Due</div>
                  <div style={{ fontWeight:800, fontSize:16, color:'#1B8B34' }}>KES {r.taxAmount.toLocaleString()}</div>
                </div>
                <div style={{
                  padding:'4px 14px', borderRadius:20, fontSize:12, fontWeight:700,
                  background: r.status==='PAID' ? '#E8F5E9' : r.status==='FILED' ? '#E3F2FD' : '#FFF8E1',
                  color: r.status==='PAID' ? '#1B8B34' : r.status==='FILED' ? '#1565C0' : '#FF8F00'
                }}>{r.status}</div>
              </div>
            </div>
          </Card>
        ))}
      </div>
    </div>
  )
}

// ── Summary Tab ───────────────────────────────────────────────────────────────
function SummaryTab() {
  const [remittances, setRemittances] = useState<TaxRemittanceResponse[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    taxApi.getRemittances().then(res => {
      if (res.success && res.data) setRemittances(res.data)
    }).finally(() => setLoading(false))
  }, [])

  const totalVat = remittances.filter(r => r.taxType === 'VAT' && r.status !== 'PENDING').reduce((s, r) => s + r.taxAmount, 0)
  const totalWht = remittances.filter(r => r.taxType === 'WHT' && r.status !== 'PENDING').reduce((s, r) => s + r.taxAmount, 0)
  const pending  = remittances.filter(r => r.status === 'PENDING').length

  const pieData = [
    { name:'VAT', value: totalVat, color:'#1B8B34' },
    { name:'WHT', value: totalWht, color:'#1565C0' },
  ].filter(d => d.value > 0)

  return (
    <div style={{ display:'flex', flexDirection:'column', gap:20 }}>
      <div style={{ display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:12 }}>
        <KpiCard title="VAT Collected"          value={`KES ${totalVat.toLocaleString()}`}   change="All paid remittances"      icon={<Percent size={18}/>}     color="#1B8B34" />
        <KpiCard title="WHT Collected"          value={`KES ${totalWht.toLocaleString()}`}   change="Withholding tax"           icon={<DollarSign size={18}/>}  color="#6A1B9A" />
        <KpiCard title="Total Liability Filed"  value={`KES ${(totalVat + totalWht).toLocaleString()}`} change="All types" icon={<TrendingUp size={18}/>} color="#E65100" />
        <KpiCard title="Pending Remittances"    value={String(pending)}                      change="Need to file"              icon={<AlertTriangle size={18}/>} color="#FF8F00" />
      </div>

      <div style={{ display:'grid', gridTemplateColumns:'2fr 1fr', gap:16 }}>
        <Card style={{ padding:20 }}>
          <h3 style={{ fontWeight:700, marginBottom:16 }}>Remittance History</h3>
          {loading ? (
            <div style={{ padding:20, textAlign:'center', color:'#888' }}>Loading...</div>
          ) : remittances.length === 0 ? (
            <div style={{ padding:20, textAlign:'center', color:'#888', fontSize:13 }}>No remittances yet — data will appear as you file periods.</div>
          ) : (
            <div style={{ display:'flex', flexDirection:'column', gap:8 }}>
              {remittances.slice(0, 6).map(r => (
                <div key={r.id} style={{ display:'flex', justifyContent:'space-between', fontSize:13, padding:'6px 0', borderBottom:'1px solid #F0F0F0' }}>
                  <span style={{ color:'#555' }}>{r.taxType} · {r.periodStart} → {r.periodEnd}</span>
                  <span style={{ fontWeight:700 }}>KES {r.taxAmount.toLocaleString()}</span>
                </div>
              ))}
            </div>
          )}
        </Card>

        <Card style={{ padding:20 }}>
          <h3 style={{ fontWeight:700, marginBottom:16 }}>Tax Breakdown</h3>
          {pieData.length > 0 ? (
            <>
              <ResponsiveContainer width="100%" height={180}>
                <PieChart>
                  <Pie data={pieData} dataKey="value" cx="50%" cy="50%" outerRadius={70}
                    label={({ name, percent }) => `${name} ${(percent*100).toFixed(0)}%`}>
                    {pieData.map((d, i) => <Cell key={i} fill={d.color} />)}
                  </Pie>
                  <Tooltip formatter={(v: number) => `KES ${v.toLocaleString()}`} />
                </PieChart>
              </ResponsiveContainer>
              <div style={{ marginTop:12 }}>
                {pieData.map(d => (
                  <div key={d.name} style={{ display:'flex', justifyContent:'space-between', padding:'5px 0', borderBottom:'1px solid #F0F0F0', fontSize:13 }}>
                    <div style={{ display:'flex', alignItems:'center', gap:8 }}>
                      <div style={{ width:10, height:10, borderRadius:3, background:d.color }} />
                      <span>{d.name}</span>
                    </div>
                    <span style={{ fontWeight:700 }}>KES {d.value.toLocaleString()}</span>
                  </div>
                ))}
              </div>
            </>
          ) : (
            <div style={{ padding:20, textAlign:'center', color:'#888', fontSize:13 }}>No data yet</div>
          )}
        </Card>
      </div>

      {/* KRA Deadlines */}
      <Card style={{ padding:20 }}>
        <h3 style={{ fontWeight:700, marginBottom:16 }}>Kenya KRA Filing Deadlines</h3>
        <div style={{ display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:12 }}>
          {[
            { type:'VAT',    deadline:'20th of following month', freq:'Monthly', color:'#1B8B34', bg:'#E8F5E9' },
            { type:'TOT',    deadline:'20th of following month', freq:'Monthly', color:'#1565C0', bg:'#E3F2FD' },
            { type:'WHT',    deadline:'20th of following month', freq:'Monthly', color:'#6A1B9A', bg:'#F3E5F5' },
            { type:'EXCISE', deadline:'20th of following month', freq:'Monthly', color:'#E65100', bg:'#FFF3E0' },
            { type:'PAYE',   deadline:'9th of following month',  freq:'Monthly', color:'#37474F', bg:'#ECEFF1' },
            { type:'Corp. Tax', deadline:'Within 6 months of year end', freq:'Annual', color:'#BF360C', bg:'#FBE9E7' },
          ].map(d => (
            <div key={d.type} style={{ background:d.bg, borderRadius:10, padding:'14px 16px', border:`1px solid ${d.color}30` }}>
              <TaxBadge type={d.type} />
              <div style={{ marginTop:8, fontSize:12, fontWeight:700, color:d.color }}>{d.deadline}</div>
              <div style={{ fontSize:11, color:'#666', marginTop:2 }}>{d.freq} filing</div>
            </div>
          ))}
        </div>
      </Card>
    </div>
  )
}

// ── Main Tax Page ─────────────────────────────────────────────────────────────
export default function TaxPage() {
  const [tab, setTab] = useState<'rates'|'calculator'|'remittances'|'summary'>('summary')

  const tabs = [
    { key:'summary',     label:'Tax Summary' },
    { key:'rates',       label:'Tax Rates' },
    { key:'calculator',  label:'Calculator' },
    { key:'remittances', label:'KRA Remittances' },
  ]

  return (
    <div className="fade-in" style={{ display:'flex', flexDirection:'column', gap:20 }}>
      <PageHeader title="Tax Management"
        action={<Btn icon={<FileText size={14}/>}>Export Tax Report</Btn>} />

      {/* Tabs */}
      <div style={{ display:'flex', gap:4, borderBottom:'2px solid #E0E0E0', marginBottom:4 }}>
        {tabs.map(t => (
          <button key={t.key} onClick={() => setTab(t.key as any)} style={{
            padding:'10px 20px', border:'none', background:'none', cursor:'pointer',
            fontWeight: tab === t.key ? 700 : 500,
            fontSize:14,
            color: tab === t.key ? '#1B8B34' : '#666',
            borderBottom: tab === t.key ? '2px solid #1B8B34' : '2px solid transparent',
            marginBottom:'-2px'
          }}>{t.label}</button>
        ))}
      </div>

      {tab === 'summary'     && <SummaryTab />}
      {tab === 'rates'       && <TaxRatesTab />}
      {tab === 'calculator'  && <TaxCalculatorTab />}
      {tab === 'remittances' && <RemittancesTab />}
    </div>
  )
}
