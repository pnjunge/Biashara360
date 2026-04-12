import React, { useState, useEffect } from 'react'
import { PageHeader, Card, Btn, DataTable, KpiCard, Modal, Input } from '../components/ui'
import { Users, Plus, Eye } from 'lucide-react'
import { customerApi, CustomerResponse } from '../services/api'

function customerStatus(c: CustomerResponse): string {
  if (c.totalOrders > 5) return 'VIP'
  if (c.totalOrders <= 1) return 'NEW'
  return 'REGULAR'
}

const emptyCustomer = { name:'', phone:'', email:'', location:'', notes:'' }

export function CustomersPage() {
  const [customers, setCustomers] = useState<CustomerResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const [showAdd, setShowAdd] = useState(false)
  const [viewCustomer, setViewCustomer] = useState<CustomerResponse | null>(null)
  const [form, setForm] = useState(emptyCustomer)

  const loadCustomers = () => {
    setLoading(true)
    customerApi.list().then(res => {
      if (res.success && res.data) setCustomers(res.data)
    }).finally(() => setLoading(false))
  }

  useEffect(() => { loadCustomers() }, [])

  const avgSpend = customers.length > 0
    ? Math.round(customers.reduce((s, c) => s + c.totalSpent, 0) / customers.length)
    : 0

  const openAdd = () => { setForm(emptyCustomer); setError(''); setShowAdd(true) }

  const handleAddCustomer = async () => {
    if (!form.name || !form.phone) { setError('Name and phone are required.'); return }
    setSaving(true); setError('')
    try {
      const res = await customerApi.create({
        name: form.name,
        phone: form.phone,
        email: form.email || null,
        location: form.location,
        notes: form.notes,
      })
      if (res.success) { setShowAdd(false); loadCustomers() }
      else setError(res.message || 'Failed to add customer.')
    } catch (e: any) {
      setError(e.response?.data?.message || 'Network error. Please try again.')
    } finally { setSaving(false) }
  }

  const f = (k: keyof typeof emptyCustomer) => (v: string) => setForm(prev => ({ ...prev, [k]: v }))

  return (
    <div className="fade-in" style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      {showAdd && (
        <Modal title="Add Customer" onClose={() => setShowAdd(false)}
          footer={<><Btn variant="secondary" onClick={() => setShowAdd(false)}>Cancel</Btn><Btn onClick={handleAddCustomer} disabled={saving}>{saving ? 'Saving...' : 'Add Customer'}</Btn></>}>
          <div style={{ display:'flex', flexDirection:'column', gap:12 }}>
            {error && <p style={{ color:'var(--b360-red)', fontSize:12 }}>{error}</p>}
            <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:12 }}>
              <Input label="Full Name *" value={form.name} onChange={f('name')} placeholder="e.g. Jane Wanjiru" />
              <Input label="Phone *" value={form.phone} onChange={f('phone')} placeholder="+254..." />
            </div>
            <Input label="Email" value={form.email} onChange={f('email')} placeholder="optional@email.com" type="email" />
            <Input label="Location" value={form.location} onChange={f('location')} placeholder="e.g. Westlands, Nairobi" />
            <Input label="Notes" value={form.notes} onChange={f('notes')} placeholder="Optional notes" />
          </div>
        </Modal>
      )}

      {viewCustomer && (
        <Modal title={viewCustomer.name} onClose={() => setViewCustomer(null)}
          footer={<Btn variant="secondary" onClick={() => setViewCustomer(null)}>Close</Btn>}>
          <div style={{ display:'flex', flexDirection:'column', gap:12 }}>
            <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:12 }}>
              <div><span style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>Phone</span><div style={{ fontWeight:600 }}>{viewCustomer.phone}</div></div>
              <div><span style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>Email</span><div style={{ fontWeight:600 }}>{viewCustomer.email || '—'}</div></div>
              <div><span style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>Location</span><div style={{ fontWeight:600 }}>{viewCustomer.location || '—'}</div></div>
              <div><span style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>Status</span>
                <div style={{
                  display:'inline-block', background: customerStatus(viewCustomer) === 'VIP' ? 'var(--b360-green-bg)' : customerStatus(viewCustomer) === 'NEW' ? 'var(--b360-amber-bg)' : 'var(--b360-surface)',
                  color: customerStatus(viewCustomer) === 'VIP' ? 'var(--b360-green)' : customerStatus(viewCustomer) === 'NEW' ? 'var(--b360-amber)' : 'var(--b360-text-secondary)',
                  borderRadius: 20, padding: '2px 10px', fontSize: 11, fontWeight: 700, marginTop:4
                }}>{customerStatus(viewCustomer)}</div>
              </div>
              <div><span style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>Total Orders</span><div style={{ fontWeight:700, fontSize:18 }}>{viewCustomer.totalOrders}</div></div>
              <div><span style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>Total Spent</span><div style={{ fontWeight:700, fontSize:18, color:'var(--b360-green)' }}>KES {viewCustomer.totalSpent.toLocaleString()}</div></div>
              <div><span style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>Loyalty Points</span><div style={{ fontWeight:700, color:'var(--b360-amber)' }}>⭐ {viewCustomer.loyaltyPoints}</div></div>
            </div>
            {viewCustomer.notes && <div style={{ fontSize:13, color:'var(--b360-text-secondary)' }}>Notes: {viewCustomer.notes}</div>}
          </div>
        </Modal>
      )}

      <PageHeader title="Customers"
        action={<Btn icon={<Plus size={14} />} onClick={openAdd}>Add Customer</Btn>} />

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4,1fr)', gap: 12 }}>
        <KpiCard title="Total Customers" value={String(customers.length)} change="All time"         icon={<Users size={18} />} color="var(--b360-blue)" />
        <KpiCard title="VIP Customers"   value={String(customers.filter(c => customerStatus(c) === 'VIP').length)} change="Top spenders" icon={<Users size={18} />} color="var(--b360-green)" />
        <KpiCard title="New Customers"   value={String(customers.filter(c => customerStatus(c) === 'NEW').length)} change="First order"   icon={<Users size={18} />} color="var(--b360-amber)" />
        <KpiCard title="Avg. Spend"      value={`KES ${avgSpend.toLocaleString()}`} change="Per customer"    icon={<Users size={18} />} color="var(--b360-red)" />
      </div>

      <Card>
        {loading ? (
          <div style={{ padding: 40, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>Loading...</div>
        ) : customers.length === 0 ? (
          <div style={{ padding: 40, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>No customers yet. Click "Add Customer" to get started.</div>
        ) : (
          <DataTable
            headers={['Name', 'Phone', 'Orders', 'Total Spent', 'Status', 'Actions']}
            rows={customers.map(c => [
              <span style={{ fontWeight: 600 }}>{c.name}</span>,
              c.phone,
              c.totalOrders,
              <span style={{ fontWeight: 700 }}>KES {c.totalSpent.toLocaleString()}</span>,
              <span style={{
                background: customerStatus(c) === 'VIP' ? 'var(--b360-green-bg)' : customerStatus(c) === 'NEW' ? 'var(--b360-amber-bg)' : 'var(--b360-surface)',
                color: customerStatus(c) === 'VIP' ? 'var(--b360-green)' : customerStatus(c) === 'NEW' ? 'var(--b360-amber)' : 'var(--b360-text-secondary)',
                borderRadius: 20, padding: '2px 10px', fontSize: 11, fontWeight: 700
              }}>{customerStatus(c)}</span>,
              <Btn small icon={<Eye size={12}/>} onClick={() => setViewCustomer(c)}>View</Btn>,
            ])}
          />
        )}
      </Card>
    </div>
  )
}

export default CustomersPage
