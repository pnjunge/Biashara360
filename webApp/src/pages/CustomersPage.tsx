import React from 'react'
import { PageHeader, Card, Btn, DataTable, StatusBadge, KpiCard } from '../components/ui'
import { Users, Plus } from 'lucide-react'

const customers = [
  { id: 1, name: 'Amina Hassan',  phone: '0712345678', orders: 12, spent: 54000, status: 'VIP' },
  { id: 2, name: 'David Kamau',   phone: '0745678901', orders: 7,  spent: 28500, status: 'REGULAR' },
  { id: 3, name: 'Sarah Wangui',  phone: '0767890123', orders: 3,  spent: 9200,  status: 'NEW' },
  { id: 4, name: 'Tom Mutua',     phone: '0778901234', orders: 19, spent: 87300, status: 'VIP' },
]

export function CustomersPage() {
  return (
    <div className="fade-in" style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      <PageHeader title="Customers"
        action={<Btn icon={<Plus size={14} />}>Add Customer</Btn>} />

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4,1fr)', gap: 12 }}>
        <KpiCard title="Total Customers" value={String(customers.length)} change="All time"         icon={<Users size={18} />} color="var(--b360-blue)" />
        <KpiCard title="VIP Customers"   value={String(customers.filter(c => c.status === 'VIP').length)} change="Top spenders" icon={<Users size={18} />} color="var(--b360-green)" />
        <KpiCard title="New This Month"  value={String(customers.filter(c => c.status === 'NEW').length)} change="First order"   icon={<Users size={18} />} color="var(--b360-amber)" />
        <KpiCard title="Avg. Spend"      value="KES 44,750" change="Per customer"    icon={<Users size={18} />} color="var(--b360-red)" />
      </div>

      <Card>
        <DataTable
          headers={['Name', 'Phone', 'Orders', 'Total Spent', 'Status', 'Actions']}
          rows={customers.map(c => [
            <span style={{ fontWeight: 600 }}>{c.name}</span>,
            c.phone,
            c.orders,
            <span style={{ fontWeight: 700 }}>KES {c.spent.toLocaleString()}</span>,
            <StatusBadge status={c.status} />,
            <Btn small>View</Btn>,
          ])}
        />
      </Card>
    </div>
  )
}

export default CustomersPage
