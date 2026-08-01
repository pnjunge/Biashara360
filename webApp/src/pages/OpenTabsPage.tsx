import React, { useEffect, useMemo, useState } from 'react'
import { Clock, ExternalLink, RefreshCw } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { Btn, Card, DataTable, KpiCard, PageHeader, StatusBadge } from '../components/ui'
import { HospitalityDashboard, hospitalityApi } from '../services/api'

export default function OpenTabsPage() {
  const navigate = useNavigate()
  const [data, setData] = useState<HospitalityDashboard | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = () => {
    setLoading(true)
    setError('')
    hospitalityApi.dashboard()
      .then(result => {
        if (result.success && result.data) setData(result.data)
        else setError(result.message || 'Could not load open tabs.')
      })
      .catch(error => setError(error.response?.data?.message || 'Could not load open tabs.'))
      .finally(() => setLoading(false))
  }

  useEffect(load, [])
  const total = useMemo(() => data?.openTabs.reduce((sum, tab) => sum + tab.subtotal, 0) || 0, [data])
  const age = (createdAt: string) => {
    const minutes = Math.max(0, Math.floor((Date.now() - new Date(createdAt).getTime()) / 60000))
    return minutes < 60 ? `${minutes} min` : `${Math.floor(minutes / 60)}h ${minutes % 60}m`
  }

  return (
    <div className="fade-in" style={{ display:'flex', flexDirection:'column', gap:20 }}>
      <PageHeader title="Open Tabs" action={<div style={{display:'flex',gap:8}}><Btn variant="secondary" icon={<RefreshCw size={14}/>} onClick={load} disabled={loading}>Refresh</Btn><Btn icon={<ExternalLink size={14}/>} onClick={() => navigate('/hospitality')}>Manage Tabs</Btn></div>} />
      {error && <Card style={{padding:16,color:'var(--b360-red)'}}>{error}</Card>}
      <div className="responsive-grid responsive-grid-4">
        <KpiCard title="Open receipts" value={String(data?.openTabs.length || 0)} change="Separate customer tabs" icon={<Clock size={18}/>} color="var(--b360-green)" />
        <KpiCard title="Awaiting payment" value={String(data?.openTabs.filter(tab => tab.tabStatus === 'AWAITING_PAYMENT').length || 0)} change="Payment initiated" icon={<Clock size={18}/>} color="var(--b360-amber)" />
        <KpiCard title="Open amount" value={`KES ${total.toLocaleString()}`} change="Outstanding tab value" icon={<Clock size={18}/>} color="var(--b360-blue)" />
      </div>
      {loading ? <Card style={{padding:32,textAlign:'center'}}>Loading open tabs…</Card> : !data?.openTabs.length ? <Card style={{padding:32,textAlign:'center',color:'var(--b360-text-secondary)'}}>No open tabs.</Card> : <Card><DataTable headers={['Table','Receipt / Tab','Customer','Guests / Items','Open','Amount','Status']} rows={data.openTabs.map(order => {
        const table = data.tables.find(item => item.id === order.hospitalityTableId)
        return [<strong>{table?.name || order.serviceType?.replace(/_/g, ' ') || 'Takeaway'}</strong>, <span style={{fontFamily:'monospace',fontWeight:800,color:'var(--b360-green)'}}>{order.orderNumber}</span>, order.customerName || 'Walk-in Guest', `${order.guestCount || 1} guest(s) · ${order.items.length} item(s)`, age(order.createdAt), <strong>KES {order.subtotal.toLocaleString()}</strong>, <StatusBadge status={order.tabStatus || 'OPEN'} />]
      })} /></Card>}
    </div>
  )
}
