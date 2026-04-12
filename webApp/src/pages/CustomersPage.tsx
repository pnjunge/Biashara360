import React, { useState, useEffect } from 'react'
import { PageHeader, Card, Btn, DataTable, KpiCard } from '../components/ui'
import { Users, Plus } from 'lucide-react'
import { customerApi, CustomerResponse } from '../services/api'

function customerStatus(c: CustomerResponse): string {
  if (c.totalOrders > 5) return 'VIP'
  if (c.totalOrders <= 1) return 'NEW'
  return 'REGULAR'
}

export function CustomersPage() {
  const [customers, setCustomers] = useState<CustomerResponse[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    customerApi.list().then(res => {
      if (res.success && res.data) setCustomers(res.data)
    }).finally(() => setLoading(false))
  }, [])

  const avgSpend = customers.length > 0
    ? Math.round(customers.reduce((s, c) => s + c.totalSpent, 0) / customers.length)
    : 0

  return (
    <div className="fade-in" style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      <PageHeader title="Customers"
        action={<Btn icon={<Plus size={14} />}>Add Customer</Btn>} />

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
          <div style={{ padding: 40, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>No customers yet</div>
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
              <Btn small>View</Btn>,
            ])}
          />
        )}
      </Card>
    </div>
  )
}

export default CustomersPage
